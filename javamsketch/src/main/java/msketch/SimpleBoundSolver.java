package msketch;

import org.apache.commons.math3.linear.*;

import java.util.Arrays;
import java.util.List;

// http://www.sciencedirect.com/science/article/pii/S0895717705004863#fd16
public class SimpleBoundSolver {
    private double[] moments;
    private double min;
    private double max;
    private int n;  // moments from 0..2n

    private double[][] momentArray;
    private double[][] smallArray;

    private List<Double> xs;
    private double[] boundSizes;

    public SimpleBoundSolver(int numMoments) {
        this.n = (numMoments - 1) / 2;
        this.moments = new double[numMoments];
        this.momentArray = new double[n+1][n+1];
        this.smallArray = new double[n][n];
    }

    /**
     * @param xs locations to calculate bound size at
     * @return total size of the error bounds provided by moments
     * http://www.personal.psu.edu/faculty/f/k/fkv/2000-06-moment-as.pdf
     */
    public double[] solveBounds(double[] powerSums, double min, double max, List<Double> xs) {
        this.min = min;
        this.max = max;
        this.xs = xs;
        for (int i = 0; i < moments.length; i++) {
            this.moments[i] = powerSums[i] / powerSums[0];
        }
        for (int i = 0; i <= n; i++) {
            System.arraycopy(moments, i, momentArray[i], 0, n+1);
        }
        RealMatrix momentMatrix = new Array2DRowRealMatrix(momentArray, false);

        double[] vectorData = new double[n+1];
        CholeskyDecomposition momentMatrixDecomp = new CholeskyDecomposition(momentMatrix);

        int numPoints = xs.size();
        boundSizes = new double[numPoints];
        for (int i = 0; i < numPoints; i++) {
            double x = xs.get(i);
            MathUtil.calcPowers(x, vectorData);
            ArrayRealVector vec = new ArrayRealVector(vectorData, false);
            double boundSize = 1.0 / vec.dotProduct(momentMatrixDecomp.getSolver().solve(vec));
            boundSizes[i] = boundSize;
        }

        return boundSizes;
    }

    public double[] getMaxErrors(List<Double> ps) {
        if (boundSizes == null) {
            throw new RuntimeException("Solve Bounds First");
        }

        int numPoints = boundSizes.length;
        double[] maxErrors = new double[numPoints];
        int n2 = moments.length;
        for (int qIdx = 0; qIdx < numPoints; qIdx++) {
            double x = xs.get(qIdx);
            double p = ps.get(qIdx);
            double maxMass = boundSizes[qIdx];
            if (n2 <= 1) {
                maxErrors[qIdx] = Math.max(p, 1.0-p);
            } else if (n2 == 2) {
                maxErrors[qIdx] = markovBoundError(x, p);
            } else {
                double scale = (max - min);
                double[] shiftedMoments = MathUtil.shiftPowerSum(moments, scale, x);
                shiftedMoments[0] -= maxMass;

                // Find Positions
                double[] coefs = orthogonalPolynomialCoefficients(shiftedMoments, n);
                boolean coefsAllZeros = true;
                for (double c : coefs) {
                    coefsAllZeros &= (c == 0);
                }
                if (coefsAllZeros) {
                    maxErrors[qIdx] = maxMass / 2.0;  // TODO: does coefs all 0 imply symmetric error?
                    break;
                }
                double[] positions = polynomialRoots(coefs);
                int n_positive_positions = 0;
                for (int j = 0; j < n; j++) {
                    if (positions[j] > 0) n_positive_positions++;
                }
                // Special case where upper bound is 1
                if (n_positive_positions == n) {
                    maxErrors[qIdx] = Math.max(maxMass - p, p);
                } else if (n_positive_positions == 0) {
                    maxErrors[qIdx] = Math.max(1.0 - p, p - (1.0 - maxMass));
                } else {

                    // Find weights
                    for (int c = 0; c < positions.length; c++) {
                        double curPow = 1.0;
                        smallArray[0][c] = 1.0;
                        for (int r = 1; r < positions.length; r++) {
                            curPow *= positions[c];
                            smallArray[r][c] = curPow;
                        }
                    }
                    RealMatrix matrix = new Array2DRowRealMatrix(smallArray, false);
                    RealVector shiftedMomentVector = new ArrayRealVector(
                            Arrays.copyOf(shiftedMoments, n), false
                    );
                    double[] weights = (new LUDecomposition(matrix)).getSolver().solve(shiftedMomentVector).toArray();

                    // Compute bounds
                    double lowerBound = 0.0;
                    for (int i = 0; i < positions.length; i++) {
                        if (positions[i] < 0) {
                            lowerBound += weights[i];
                        }
                    }
                    double upperBound = lowerBound + maxMass;

                    // Return the larger one-sided error
                    maxErrors[qIdx] = Math.max(
                            Math.abs(upperBound - p),
                            Math.abs(p - lowerBound)
                    );
                }

            }
        }

        return maxErrors;
    }


    // Solve for polynomial roots using the companion matrix
    private double[] polynomialRoots(double[] coefs) {
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n-1; c++) {
                smallArray[r][c] = 0.0;
            }
        }
        for (int i = 0; i < n-1; i++) {
            smallArray[i+1][i] = 1.0;
        }
        double a = coefs[n];
        for (int r = 0; r < n; r++) {
            smallArray[r][n-1] = -coefs[r] / a;
        }
        Array2DRowRealMatrix companionMatrix = new Array2DRowRealMatrix(smallArray, false);
        return (new EigenDecomposition(companionMatrix)).getRealEigenvalues();
    }

    private double[] orthogonalPolynomialCoefficients(double[] shiftedPowerSums, int n) {
        for (int r = 0; r < n; r++) {
            System.arraycopy(shiftedPowerSums, r+1, smallArray[r], 0, n);
        }

        double[] coefs = new double[n+1];
        double sign = n % 2 == 0 ? 1 : -1;

        Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(smallArray, false);
        for (int i = 0; i <= n; i++) {
            coefs[i] = sign * (new LUDecomposition(matrix)).getDeterminant();
            if (i == n) break;
            for (int r = 0; r < n; r++) {
                matrix.setEntry(r, i, shiftedPowerSums[r+i]);
            }
            sign *= -1;
        }
        return coefs;
    }

    private double markovBoundError(double estimate, double p) {
        double mean = moments[1];
        double lowerBound = Math.max(0.0, 1 - (mean - min) / (estimate - min));
        double upperBound = Math.min(1.0, (max - mean) / (max - estimate));
        return Math.max(upperBound - p, p - lowerBound);
    }

}
