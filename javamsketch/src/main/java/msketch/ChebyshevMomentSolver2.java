package msketch;

import msketch.chebyshev.ChebyshevPolynomial;
import msketch.optimizer.NewtonOptimizer;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;

import java.util.Arrays;

public class ChebyshevMomentSolver2 {
    private double[] d_mus;
    private int numNormalPowers;
    private boolean useStandardBasis = true;
    private boolean verbose = false;
    private double aCenter, aScale, bCenter, bScale;

    private double[] lambdas;
    private ChebyshevPolynomial approxCDF;
    private boolean isConverged;

    private NewtonOptimizer optimizer;
    private int cumFuncEvals;

    public ChebyshevMomentSolver2(
            boolean useStandardBasis,
            int numNormalPowers,
            double[] chebyshev_moments,
            double aCenter,
            double aScale,
            double bCenter,
            double bScale
    ) {
        this.useStandardBasis = useStandardBasis;
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

        boolean useStandardBasis  = true;
        if (logChebyMoments.length > 2 && powerChebyMoments.length > 2) {
            // Use the variance as an indicator of how suitable a basis is
            // Low variance means "spiky" distributions that are hard to handle
            double powerVar = MathUtil.varOfChebyMoments(powerChebyMoments);
            double logVar = MathUtil.varOfChebyMoments(logChebyMoments);
            if (powerVar > logVar || powerSums[0] > logPowerSums[0]) {
                useStandardBasis = true;
            } else {
                useStandardBasis = false;
            }
        }

        double[] aMoments, bMoments;
        double aMin, aMax, bMin, bMax;
        if (useStandardBasis) {
            aMoments = powerChebyMoments;
            bMoments = logChebyMoments;
            aMin = min; aMax = max; bMin = logMin; bMax = logMax;
        } else {
            aMoments = logChebyMoments;
            bMoments = powerChebyMoments;
            bMin = min; bMax = max; aMin = logMin; aMax = logMax;
        }

        // Don't use all of the secondary powers to solve, the acc / speed tradeoff
        // isn't worth it.
        SolveBasisSelector sel = new SolveBasisSelector();
        sel.select(useStandardBasis, aMoments.length, bMoments.length, aMin, aMax, bMin, bMax);
        int ka = sel.getKa();
        int kb = sel.getKb();
        bMoments = Arrays.copyOf(bMoments, kb);

        double[] combinedMoments = new double[aMoments.length + bMoments.length - 1];
        for (int i = 0; i < aMoments.length; i++) {
            combinedMoments[i] = aMoments[i];
        }
        for (int i = 0; i < bMoments.length - 1; i++) {
            combinedMoments[i + aMoments.length] = bMoments[i + 1];
        }
        return new ChebyshevMomentSolver2(
                useStandardBasis,
                aMoments.length,
                combinedMoments,
                (aMax + aMin) / 2,
                (aMax - aMin) / 2,
                (bMax + bMin) / 2,
                (bMax - bMin)/ 2
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
                useStandardBasis,
                numNormalPowers,
                d_mus,
                aCenter,
                aScale,
                bCenter,
                bScale
        );
        optimizer = new NewtonOptimizer(potential);
        optimizer.setVerbose(verbose);
        if (verbose) {
            System.out.println("Beginning solve with order: "+numNormalPowers+","+(d_mus.length-numNormalPowers+1));
        }

        lambdas = optimizer.solve(l_initial, tol);
        isConverged = optimizer.isConverged();
        cumFuncEvals = potential.getCumFuncEvals();
        if (verbose) {
            System.out.println("Using standard basis: "+ useStandardBasis);
            System.out.println("Final Polynomial: " + Arrays.toString(lambdas));
            System.out.println("Total Function Evals: "+cumFuncEvals);
            System.out.println(String.format("linscales: "+ aCenter +","+aScale+","+bCenter+","+bScale));
        }

        approxCDF = ChebyshevPolynomial.fit(potential.getFunc(), tol).integralPoly();
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
            if (!useStandardBasis) {
                quantiles[i] = Math.exp(quantiles[i]);
            }
        }
        return quantiles;
    }

    public int getK1() {
        return numNormalPowers;
    }
    public int getK2() {
        return d_mus.length - numNormalPowers + 1;
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
    public boolean isUseStandardBasis() {
        return useStandardBasis;
    }
}
