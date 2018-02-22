package sketches;

import msketch.ChebyshevMomentSolver2;
import msketch.MathUtil;
import msketch.SimpleBoundSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tracks both the moments and the log-moments and solves for both
 * simultaneously when possible.
 */
public class CMomentSketch implements QuantileSketch{
    private int ka = 5;
    private int kb = 5;
    private double tolerance = 1e-10;
    private boolean verbose = false;

    private double min;
    private double max;
    private double logMin;
    private double logMax;
    // Stores the normal moments and the log moments
    private double[] totalSums;

    private boolean errorBounds = false;
    private double[] errors;

    @Override
    public String getName() {
        return "cmoments";
    }

    public CMomentSketch(double tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public int getSize() {
        return (Double.BYTES)*(2+1+totalSums.length);
    }

    @Override
    public double getSizeParam() {
        return ka;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.ka = (int)sizeParam;
        this.kb = ka;
    }

    @Override
    public void setVerbose(boolean flag) {
        verbose = flag;
    }

    @Override
    public void initialize() {
        this.min = Double.MAX_VALUE;
        this.max = -Double.MAX_VALUE;
        this.logMin = Double.MAX_VALUE;
        this.logMax = -Double.MAX_VALUE;
        this.totalSums = new double[ka+kb];
    }

    @Override
    public void setCalcError(boolean flag) {
        errorBounds = flag;
        return;
    }

    public void setStats(
            double min,
            double max,
            double logMin,
            double logMax,
            double[] powerSums,
            double[] logSums
    ) {
        ka = powerSums.length;
        kb = logSums.length;
        int l = powerSums.length + logSums.length;
        totalSums = new double[l];
        for (int i = 0; i < ka; i++) {
            totalSums[i] = powerSums[i];
        }
        for (int i = 0; i < kb; i++) {
            totalSums[powerSums.length + i] = logSums[i];
        }
        this.min = min;
        this.max = max;
        this.logMin = logMin;
        this.logMax = logMax;
    }

    /* Floating point */
    public void convertToLowPrecision(int bits) {
        if (bits >= 64) {
            return;
        }

        System.out.println(Arrays.toString(totalSums));

        double[] scaledChebyMoments = MathUtil.powerSumsToChebyMoments(
                min, max, totalSums
        );

        int k = totalSums.length - 1;
        double r = (max - min) / 2;
        double xc = (max + min) / 2;
        // First rescale the variables so that they lie in [-1,1]
        double[] scaledPowerSums = MathUtil.shiftPowerSum(
                totalSums,r,xc
        );

        System.out.println(Arrays.toString(scaledPowerSums));
        System.out.println(Arrays.toString(scaledChebyMoments));

        double minVal = min;
        double maxVal = max;
        for (double val : scaledPowerSums) {
            if (val < Math.abs(minVal)) minVal = Math.abs(val);
            if (val > maxVal) maxVal = val;
        }

        int logRange = (int)Math.ceil(log(maxVal, 2)) - (int)Math.floor(log(minVal, 2));
        int EL = (int)Math.ceil(log(logRange + 1, 2));
        int SL = bits - EL - 1;
        int emax = (int)Math.ceil(log(maxVal, 2));
        int emin = (int)Math.floor(log(minVal, 2));
        int maxSignificand = (int)Math.pow(2, SL) - 1;
//        int max = maxSignificand * (int)Math.pow(2, emax);
//        int min = (int)Math.pow(2, emin);

        System.out.format("%f %f %d\n", minVal, maxVal, bits);
        System.out.format("%d %d %d %d\n", logRange, EL, SL, maxSignificand);

        max = encode(max, maxSignificand);
        min = encode(min, maxSignificand);
        for (int i = 0; i < scaledPowerSums.length; i++) {
            scaledPowerSums[i] = encode(scaledPowerSums[i], maxSignificand);
        }

        totalSums = MathUtil.shiftPowerSum(
                scaledPowerSums,1/r, 0
        );
        totalSums = MathUtil.shiftPowerSum(
                totalSums,1, -xc
        );

        System.out.println(Arrays.toString(totalSums));
    }

    private double encode(double val, int maxSignificand) {
        int exponent = (int)Math.ceil(log(Math.abs(val) / (maxSignificand + 1), 2));
        double eps = Math.pow(2, exponent);
        System.out.format("%d %f %d\n", exponent, eps, Math.round(val / eps));
        return Math.round(val / eps) * eps;
    }

    private double log(double val, int base) {
        return Math.log(val) / Math.log(base);
    }

    @Override
    public void add(double[] data) {
        for (double x: data) {
            if (x < this.min) {
                this.min = x;
            } else if (x > this.max) {
                this.max = x;
            }
            double[] localSums = this.totalSums;
            localSums[0]++;
            double curPow = 1.0;
            for (int i = 1; i < ka; i++) {
                curPow *= x;
                localSums[i] += curPow;
            }

            if (x > 0.0) {
                double logX = Math.log(x);
                if (logX < this.logMin) {
                    this.logMin = logX;
                } else if (logX > this.logMax) {
                    this.logMax = logX;
                }
                localSums[ka]++;
                curPow = 1.0;
                for (int i = 1; i < kb; i++) {
                    curPow *= logX;
                    localSums[ka+i] += curPow;
                }
            }
        }
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches) {
        double mMin = this.min;
        double mMax = this.max;
        double mLogMin = this.logMin;
        double mLogMax = this.logMax;
        double[] mSums = this.totalSums;
        final int l = this.totalSums.length;

        for (QuantileSketch s : sketches) {
            CMomentSketch ms = (CMomentSketch) s;
            if (ms.min < mMin) {
                mMin = ms.min;
            }
            if (ms.max > mMax) {
                mMax = ms.max;
            }
            if (ms.logMin < mLogMin) {
                mLogMin = ms.logMin;
            }
            if (ms.logMax > mLogMax) {
                mLogMax = ms.logMax;
            }
            for (int i = 0; i < l; i++) {
                mSums[i] += ms.totalSums[i];
            }
        }
        this.min = mMin;
        this.max = mMax;
        this.logMin = mLogMin;
        this.logMax = mLogMax;
        return this;
    }


    @Override
    public double[] getQuantiles(List<Double> pList) throws Exception {
        double[] powerSums = Arrays.copyOfRange(totalSums, 0, ka);
        double[] logSums = Arrays.copyOfRange(totalSums, ka, ka+kb);

        ChebyshevMomentSolver2 solver;
        boolean useStandardBasis = true;
        if (min > 0) {
            solver = ChebyshevMomentSolver2.fromPowerSums(
                    min, max, powerSums,
                    logMin, logMax, logSums
            );
            useStandardBasis = solver.isUseStandardBasis();
        } else {
            useStandardBasis = true;
            logSums = new double[1];
            solver = ChebyshevMomentSolver2.fromPowerSums(
                    min, max, powerSums,
                    logMin, logMax, logSums
            );
        }
        solver.setVerbose(verbose);
        solver.solve(tolerance);
        int m = pList.size();
        double[] ps = MathUtil.listToArray(pList);
        double[] quantiles = solver.estimateQuantiles(ps);

        errors = new double[m];
        if (errorBounds) {
            if (useStandardBasis) {
                double[] moments = MathUtil.powerSumsToMoments(powerSums);
                SimpleBoundSolver boundSolver = new SimpleBoundSolver(solver.getNumNormalPowers());
                double[] boundSizes = boundSolver.solveBounds(moments, quantiles);
                errors = boundSolver.getMaxErrors(moments, quantiles, ps, boundSizes);
            } else {
                double[] logQuantiles = new double[m];
                for (int i = 0; i < m; i++) {
                    logQuantiles[i] = Math.log(quantiles[i]);
                }
                double[] moments = MathUtil.powerSumsToMoments(logSums);
                SimpleBoundSolver boundSolver = new SimpleBoundSolver(solver.getNumNormalPowers());
                double[] boundSizes = boundSolver.solveBounds(moments, logQuantiles);
                errors = boundSolver.getMaxErrors(moments, logQuantiles, ps, boundSizes);
            }
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
