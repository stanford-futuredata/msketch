package sketches;

import histogram.ApproximateHistogram;

import java.util.ArrayList;
import java.util.List;

public class ApproximateHistogramSketch implements QuantileSketch{
    private int size;
    private ApproximateHistogram hist;
    private boolean calcError;

    private double[] errors;

    @Override
    public String getName() {
        return "approx_histogram";
    }

    @Override
    public int getSize() {
        return hist.getMinStorageSize();
    }

    @Override
    public double getSizeParam() {
        return size;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.size = (int)sizeParam;
    }

    @Override
    public void setCalcError(boolean flag) {
        this.calcError = calcError;
    }

    @Override
    public void initialize() {
        this.hist = new ApproximateHistogram(size);
    }

    @Override
    public void add(double[] data) {
        for (double x : data) {
            this.hist.offer((float)x);
        }
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            ApproximateHistogramSketch s = (ApproximateHistogramSketch) sketches.get(i);
            this.hist.foldFast(s.hist);
        }
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        float[] psArray = new float[ps.size()];
        for (int i = 0; i < ps.size(); i++) {
            psArray[i] = ps.get(i).floatValue();
        }
        float[] qsFloat = this.hist.getQuantiles(psArray);
        double[] qsDouble = new double[qsFloat.length];
        errors = new double[qsFloat.length];
        for (int i = 0; i < qsFloat.length; i++) {
            qsDouble[i] = qsFloat[i];
        }
        return qsDouble;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
