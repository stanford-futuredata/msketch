package sketches;

import msketch.ChebyshevMomentSolver;

import java.util.List;

public class MomentSketch implements QuantileSketch {
    private int k = 5;
    private boolean errorBounds = false;
    private double tolerance = 1e-10;
    private double[] errors;

    private double min;
    private double max;
    private double[] powerSums;

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
            for (int i = 1; i < k; i++) {
                curPow *= x;
                this.powerSums[i] += curPow;
            }
        }
    }

    @Override
    public QuantileSketch merge(QuantileSketch[] sketches) {
        MomentSketch firstSketch = (MomentSketch)sketches[0];
        double mMin = Double.MAX_VALUE;
        double mMax = -Double.MAX_VALUE;
        int k = firstSketch.k;
        double[] mPowerSums = new double[k];
        MomentSketch[] mSketches = (MomentSketch[])sketches;
        for (MomentSketch ms : mSketches) {
            if (ms.min < mMin) {
                mMin = ms.min;
            }
            if (ms.max > mMax) {
                mMax = ms.max;
            }
            for (int i = 0; i <= k; i++) {
                mPowerSums[i] += ms.powerSums[i];
            }
        }
        MomentSketch m = new MomentSketch(firstSketch.tolerance);
        m.setSizeParam(k);
        m.min = mMin;
        m.max = mMax;
        m.powerSums = mPowerSums;
        return m;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        ChebyshevMomentSolver solver = ChebyshevMomentSolver.fromPowerSums(
                min, max, powerSums
        );
        solver.solve(tolerance);
        int m = ps.size();
        double[] estQuantiles = new double[m];
        for (int i = 0; i < m; i++) {
            estQuantiles[i] = solver.estimateQuantile(ps.get(i), min, max);
        }

        errors = new double[m];
        return estQuantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
