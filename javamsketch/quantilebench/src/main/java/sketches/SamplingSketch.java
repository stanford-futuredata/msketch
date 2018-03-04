package sketches;

import com.yahoo.sketches.sampling.ReservoirItemsSketch;
import com.yahoo.sketches.sampling.ReservoirItemsUnion;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.List;

public class SamplingSketch implements QuantileSketch {
    private int size;
    private ReservoirItemsSketch<Double> reservoir;
    private double[] errors;
    private boolean calcError = true;

    public SamplingSketch() {}

    @Override
    public String getName() {
        return "sampling";
    }

    @Override
    public int getSize() {
        return (Long.BYTES + this.size * Double.BYTES);
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
        this.calcError = flag;
    }

    @Override
    public void initialize() {
        this.reservoir = ReservoirItemsSketch.newInstance(size);
    }

    @Override
    public void add(double[] data) {
        for (double x : data) {
            this.reservoir.update(x);
        }
    }


    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches) {
        ReservoirItemsUnion<Double> newUnion = ReservoirItemsUnion.newInstance(this.size);
        newUnion.update(this.reservoir);
        for (QuantileSketch s :sketches) {
            SamplingSketch ss = (SamplingSketch)s;
            newUnion.update(ss.reservoir);
        }
        this.reservoir = newUnion.getResult();
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        Double[] samples = this.reservoir.getSamples();
        double[] data = new double[samples.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = samples[i];
        }

        int m = ps.size();
        double[] quantiles = QuantileUtil.getTrueQuantiles(ps, data);

        errors = new double[m];
        if (calcError) {
            for (int i = 0; i < m; i++) {
                double p = ps.get(i);
                errors[i] = 2.5 * FastMath.sqrt(p * (1 - p) / this.size);
            }
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }

}
