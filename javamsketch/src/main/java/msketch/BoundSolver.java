package msketch;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.*;

import java.util.Arrays;

// http://www.sciencedirect.com/science/article/pii/S0895717705004863#fd16
public class BoundSolver {
    private double[] powerSums;
    private double min;
    private double max;

    public BoundSolver(double[] powerSums, double min, double max) {
        this.powerSums = powerSums;
        this.min = min;
        this.max = max;
    }

    public double quantileError(double est, double p) {
        if (powerSums.length <= 1) {
            return Math.max(p, 1.0-p);
        } else if (powerSums.length == 2) {
            return markovBoundError(est, p);
        }

        double[] shiftedPowerSums = MathUtil.shiftPowerSum(powerSums, 1.0, est);
        double count = shiftedPowerSums[0];
        for (int i = 0; i < shiftedPowerSums.length; i++) {
            shiftedPowerSums[i] /= count;
        }
        int n = (powerSums.length - 1) / 2;
        double massAtZero = maxMassAtZero(shiftedPowerSums, n);
        shiftedPowerSums[0] -= massAtZero;

        // Find positions
        double[] coefs = orthogonalPolynomialCoefficients(shiftedPowerSums, n);
        boolean coefsAllZeros = true;
        for (double c : coefs) {
            coefsAllZeros &= (c == 0);
        }
        if (coefsAllZeros) {
            return massAtZero / 2.0;  // TODO: does coefs all 0 imply symmetric error?
        }
        LaguerreSolver rootSolver = new LaguerreSolver();
        Complex[] roots = rootSolver.solveAllComplex(coefs, 0);
        double[] positions = new double[roots.length];
        int n_positive_positions = 0;
        for (int i = 0; i < roots.length; i++) {
            positions[i] = roots[i].getReal();
            if (positions[i] > 0) n_positive_positions++;
        }

        // Special case where upper bound is 1
        if (n_positive_positions == n) {
            return Math.max(massAtZero - p, p);
        }
        // Special case where lower bound is 0
        if (n_positive_positions == 0) {
            return Math.max(1.0 - p, p - (1.0 - massAtZero));
        }

        // Find weights
        double[][] matrixData = new double[positions.length][positions.length];
        for (int c = 0; c < positions.length; c++) {
            double curPow = 1.0;
            matrixData[0][c] = 1.0;
            for (int r = 1; r < positions.length; r++) {
                curPow *= positions[c];
                matrixData[r][c] = curPow;
            }
        }
        RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
        double[] weights = (new LUDecomposition(matrix)).getSolver().solve(
                new ArrayRealVector(Arrays.copyOfRange(shiftedPowerSums, 0, n))).toArray();

        // Compute bounds
        double lowerBound = 0.0;
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] < 0) {
                lowerBound += weights[i];
            }
        }
        double upperBound = lowerBound + massAtZero;

        // Return the larger one-sided error
        return Math.max(upperBound - p, p - lowerBound);
    }

    private double maxMassAtZero(double[] shiftedPowerSums, int n) {
        double[][] numeratorMatrixData = new double[n+1][n+1];
        for (int r = 0; r <= n; r++) {
            System.arraycopy(shiftedPowerSums, r, numeratorMatrixData[r], 0, n + 1);
        }
        RealMatrix numeratorMatrix = new Array2DRowRealMatrix(numeratorMatrixData);

        double[][] denominatorMatrixData = new double[n][n];
        for (int r = 0; r < n; r++) {
            System.arraycopy(shiftedPowerSums, r+2, denominatorMatrixData[r], 0, n);
        }
        RealMatrix denominatorMatrix = new Array2DRowRealMatrix(denominatorMatrixData);

        return (new LUDecomposition(numeratorMatrix)).getDeterminant() /
                (new LUDecomposition(denominatorMatrix)).getDeterminant();
    }

    private double[] orthogonalPolynomialCoefficients(double[] shiftedPowerSums, int n) {
        double[][] matrixData = new double[n][n];
        for (int r = 0; r < n; r++) {
            System.arraycopy(shiftedPowerSums, r+1, matrixData[r], 0, n);
        }
        double[] coefs = new double[n+1];
        double sign = n % 2 == 0 ? 1 : -1;
        for (int i = 0; i <= n; i++) {
            coefs[i] = sign * (new LUDecomposition(new Array2DRowRealMatrix(matrixData))).getDeterminant();
            if (i == n) break;
            for (int r = 0; r < n; r++) {
                matrixData[r][i] = shiftedPowerSums[r+i];
            }
            sign *= -1;
        }
        return coefs;
    }

    private double markovBoundError(double estimate, double p) {
        double mean = powerSums[1] / powerSums[0];
        double lowerBound = Math.max(0.0, 1 - (mean - min) / (estimate - min));
        double upperBound = Math.min(1.0, (max - mean) / (max - estimate));
        return Math.max(upperBound - p, p - lowerBound);
    }
}
