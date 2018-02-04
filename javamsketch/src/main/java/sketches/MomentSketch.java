package sketches;

import msketch.BoundSolver;
import msketch.ChebyshevMomentSolver;
import msketch.MathUtil;
import msketch.SimpleBoundSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MomentSketch implements QuantileSketch {
    private int k = 5;
    private boolean errorBounds = false;
    private double tolerance = 1e-10;
    private boolean verbose = false;

    private double[] errors;

    private double min;
    private double max;
    private double[] powerSums;

    public double[] getPowerSums() {
        return powerSums;
    }
    public double getMin() { return min; }
    public double getMax() { return max; }

    public MomentSketch(double tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public String getName() {
        return "moment";
    }

    @Override
    public int getSize() {
        return (Double.BYTES)*(2+powerSums.length);
    }

    @Override
    public double getSizeParam() {
        return k;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.k = (int)sizeParam;
    }

    @Override
    public void setVerbose(boolean flag) {
        verbose = flag;
    }

    @Override
    public void initialize() {
        this.min = Double.MAX_VALUE;
        this.max = -Double.MAX_VALUE;
        this.powerSums = new double[k];
    }

    public void setStats(double[] powerSums, double min, double max) {
        this.k = powerSums.length;
        this.powerSums = powerSums;
        this.min = min;
        this.max = max;
    }

    @Override
    public void setCalcError(boolean flag) {
        this.errorBounds = flag;
    }

    @Override
    public void add(double[] data) {
        for (double x: data) {
            if (x < this.min) {
                this.min = x;
            } else if (x > this.max) {
                this.max = x;
            }
            this.powerSums[0]++;
            double curPow = 1.0;
            int numPows = k;
            double[] localPowerSums = this.powerSums;
            for (int i = 1; i < numPows; i++) {
                curPow *= x;
                localPowerSums[i] += curPow;
            }
        }
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches) {
        double mMin = this.min;
        double mMax = this.max;
        double[] mPowerSums = this.powerSums;
        for (QuantileSketch s : sketches) {
            MomentSketch ms = (MomentSketch)s;
            if (ms.min < mMin) {
                mMin = ms.min;
            }
            if (ms.max > mMax) {
                mMax = ms.max;
            }
            for (int i = 0; i < k; i++) {
                mPowerSums[i] += ms.powerSums[i];
            }
        }
        this.min = mMin;
        this.max = mMax;
        return this;
    }

    /* Returns bounds for the number of values greater than a threshold. */
    public double[] boundGreaterThanThreshold(double x) {
        double[] xs = new double[]{x};
        SimpleBoundSolver boundSolver = new SimpleBoundSolver(k);
        double[] moments = MathUtil.powerSumsToMoments(powerSums);
        double[] bounds;
        try {
            double[] boundSizes = boundSolver.solveBounds(moments, xs);
            bounds = boundSolver.getBoundEndpoints(moments, x, boundSizes[0]);
        } catch (Exception e) {
            return new double[]{0.0, 1.0};
        }
        return new double[]{1.0 - bounds[1], 1.0 - bounds[0]};
    }

    public double estimateGreaterThanThreshold(double x) {
        if (x < min) return 1.0;
        if (x > max) return 0.0;
        ChebyshevMomentSolver solver = ChebyshevMomentSolver.fromPowerSums(
                min, max, powerSums
        );
        solver.setVerbose(verbose);
        // special case
        if (min == max) {
            return (x > min) ? 0.0 : 1.0;
        }
        solver.solve(tolerance);
        double scaledX = 2.0 * (x - min) / (max - min) - 1.0;
        double quantile = solver.estimateCDF(scaledX);
        return 1.0 - quantile;
    }

    @Override
    public double[] getQuantiles(List<Double> pList) throws Exception {
        ChebyshevMomentSolver solver = ChebyshevMomentSolver.fromPowerSums(
                min, max, powerSums
        );
        solver.setVerbose(verbose);
        solver.solve(tolerance);
        int m = pList.size();
        double[] ps = MathUtil.listToArray(pList);
        double[] quantiles = solver.estimateQuantiles(ps, min, max);

        if (errorBounds) {
            double[] moments = MathUtil.powerSumsToMoments(powerSums);
            SimpleBoundSolver boundSolver = new SimpleBoundSolver(k);
            double[] boundSizes = boundSolver.solveBounds(moments, quantiles);
            errors = boundSolver.getMaxErrors(moments, quantiles, ps, boundSizes);
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
