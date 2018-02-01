package msketch;

import msketch.optimizer.NewtonOptimizer;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;

import java.util.Arrays;

public class ChebyshevMomentSolver2 {
    private double[] d_mus;
    private int numNormalPowers;
    private boolean isLog = false;
    private boolean verbose = false;
    private double aCenter, aScale, bCenter, bScale;

    private double[] lambdas;
    private ChebyshevPolynomial approxCDF;
    private boolean isConverged;

    private NewtonOptimizer optimizer;
    private int cumFuncEvals;

    public ChebyshevMomentSolver2(
            boolean isLog,
            int numNormalPowers,
            double[] chebyshev_moments,
            double aCenter,
            double aScale,
            double bCenter,
            double bScale
    ) {
        this.isLog = isLog;
        this.numNormalPowers = numNormalPowers;
        this.d_mus = chebyshev_moments;
        this.aCenter = aCenter;
        this.aScale = aScale;
        this.bCenter = bCenter;
        this.bScale = bScale;
    }

    public static ChebyshevMomentSolver2 fromPowerSums(
            double min, double max, double[] powerSums,
            double logMin, double logMax, double[] logPowerSums
    ) {
        double[] powerChebyMoments = MathUtil.powerSumsToChebyMoments(
                min, max, powerSums
        );
        double[] logChebyMoments = MathUtil.powerSumsToChebyMoments(
                logMin, logMax, logPowerSums
        );
        double[] combinedMoments = new double[powerSums.length + logPowerSums.length - 1];
        for (int i = 0; i < logChebyMoments.length; i++) {
            combinedMoments[i] = logChebyMoments[i];
        }
        for (int i = 0; i < powerChebyMoments.length-1; i++) {
            combinedMoments[i+logChebyMoments.length] = powerChebyMoments[i+1];
        }
        return new ChebyshevMomentSolver2(
                false,
                logChebyMoments.length,
                combinedMoments,
                (logMax+logMin)/2,
                (logMax-logMin)/2,
                (max+min)/2,
                (max-min)/2
        );
    }

    public void setVerbose(boolean flag) {
        this.verbose = flag;
    }

    public int solve(double tol) {
        double[] l_initial = new double[d_mus.length];
        return solve(l_initial, tol);
    }

    public int solve(double[] l_initial, double tol) {
        MaxEntPotential2 potential = new MaxEntPotential2(
                isLog,
                numNormalPowers,
                d_mus,
                aCenter,
                aScale,
                bCenter,
                bScale
        );
        optimizer = new NewtonOptimizer(potential);
        optimizer.setVerbose(verbose);
        lambdas = optimizer.solve(l_initial, tol);
        isConverged = optimizer.isConverged();
        if (verbose) {
            System.out.println("Final Polynomial: " + Arrays.toString(lambdas));
        }
        cumFuncEvals = potential.getCumFuncEvals();

        approxCDF = ChebyshevPolynomial.fit(potential.getFunc(), tol).integralPoly();
//        approxCDF = ChebyshevPolynomial.fit(new MaxEntFunction(lambdas), tol).integralPoly();
        return optimizer.getStepCount();
    }

    public double[] estimateQuantiles(double[] ps) {
        UnivariateSolver bSolver = new BrentSolver(1e-6);
        int n = ps.length;
        double[] quantiles = new double[n];

        for (int i = 0; i < n; i++) {
            double p = ps[i];
            double q;
            if (p <= 0.0) {
                q = -1;
            } else if (p >= 1.0) {
                q = 1;
            } else {
                q = bSolver.solve(
                        100,
                        (x) -> approxCDF.value(x) - p,
                        -1,
                        1,
                        0
                );
            }
            quantiles[i] = q*aScale+aCenter;
        }
        return quantiles;
    }

    public double estimateCDF(double x) {
        return approxCDF.value(x);
    }

    public double[] getLambdas() {
        return lambdas;
    }

    public NewtonOptimizer getOptimizer() {
        return optimizer;
    }
    public int getCumFuncEvals() {
        return cumFuncEvals;
    }
    public double[] getChebyshevMoments() { return d_mus; }

    public boolean isConverged() {
        return isConverged;
    }
}
