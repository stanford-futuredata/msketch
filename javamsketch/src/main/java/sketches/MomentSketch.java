package sketches;

import msketch.BoundSolver;
import msketch.ChebyshevMomentSolver;

import java.util.ArrayList;
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
        double mMin = Double.MAX_VALUE;
        double mMax = -Double.MAX_VALUE;
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

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        ChebyshevMomentSolver solver = ChebyshevMomentSolver.fromPowerSums(
                min, max, powerSums
        );
        solver.setVerbose(verbose);
        solver.solve(tolerance);
        BoundSolver boundSolver = new BoundSolver(powerSums, min, max);
        int m = ps.size();
        double[] estQuantiles = new double[m];
        errors = new double[m];
        for (int i = 0; i < m; i++) {
            estQuantiles[i] = solver.estimateQuantile(ps.get(i), min, max);
            if (errorBounds) {
                errors[i] = boundSolver.quantileError(estQuantiles[i], ps.get(i));
            }
        }

        return estQuantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
