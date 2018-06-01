package sketches;

import gk.GKSketch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GKAdaptiveSketch implements QuantileSketch{
    private GKSketch summary;
    private double sizeParam = 100.0;
    private int bufferSize = 100;
    private double[] buffer;
    private double[] errors;

    @Override
    public String getName() {
        return "gk_adaptive";
    }

    @Override
    public int getSize() {
        return summary.getTuples().size() * (Double.BYTES + 2*Integer.BYTES);
    }

    @Override
    public double getSizeParam() {
        return sizeParam;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.sizeParam = sizeParam;
    }

    @Override
    public void setCalcError(boolean flag) {
        return;
    }

    @Override
    public void initialize() {
        this.buffer = new double[bufferSize];
        this.summary = new GKSketch(
                1.0/sizeParam
        );
    }

    @Override
    public void add(double[] data) {
        int iBuff = 0;
        for (double x : data) {
            buffer[iBuff] = x;
            iBuff++;
            if (iBuff == bufferSize) {
                this.summary.add(buffer);
                iBuff = 0;
            }
        }
        this.summary.add(Arrays.copyOf(buffer, iBuff));
    }

    @Override
    public QuantileSketch merge(List<QuantileSketch> sketches, int startIndex, int endIndex) {
        GKSketch newSumm = this.summary;
        for (int i = startIndex; i < endIndex; i++) {
            GKAdaptiveSketch gks = (GKAdaptiveSketch) sketches.get(i);
            newSumm.merge(gks.summary);
        }
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        int m = ps.size();
        double[] quantiles = new double[m];
        for (int i = 0; i < m; i++) {
            quantiles[i] = summary.quantile(ps.get(i));
        }

        errors = new double[m];
        for (int i = 0; i < m; i++) {
            errors[i] = 1.0/sizeParam;
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }

    public String getDebugString() {
        ArrayList<String> parts = new ArrayList<>();
        for (GKSketch.Tuple t : summary.getTuples()) {
            parts.add(t.toString());
        }
        return String.join(",", parts);
    }
}
