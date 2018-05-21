package sketches;

//import com.yahoo.sketches.quantiles.DoublesSketch;
//import com.yahoo.sketches.quantiles.DoublesUnion;
//import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

import yahoo.DoublesSketch;
import yahoo.DoublesUnion;
import yahoo.UpdateDoublesSketch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class YahooSketch implements QuantileSketch {
    private int k;
    private UpdateDoublesSketch sketch;
    private boolean calcError = true;

    private double[] errors;

    public YahooSketch() {}

    protected YahooSketch(UpdateDoublesSketch s) {
        this.k = s.getK();
        this.sketch = s;
    }

    @Override
    public String getName() {
        return "yahoo";
    }

    @Override
    public int getSize() {
        return sketch.getStorageBytes();
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
    public void setCalcError(boolean flag) {
        calcError = flag;
    }

    @Override
    public void initialize() {
        sketch = DoublesSketch.builder().setK(this.k).build();
    }

    @Override
    public void add(double[] data) {
        for (double x : data) {
            sketch.update(x);
        }
    }

    @Override
    public QuantileSketch merge(List<QuantileSketch> sketches, int startIndex, int endIndex) {
        DoublesUnion union = DoublesUnion.builder().setMaxK(k).build();
        union.update(this.sketch);
        for (int i = startIndex; i < endIndex; i++) {
            YahooSketch ys = (YahooSketch) sketches.get(i);
            union.update(ys.sketch);
        }
        this.sketch = union.getResult();
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        int m = ps.size();
        double[] psArray = new double[m];
        for (int i = 0; i < m; i++) {
            psArray[i] = ps.get(i);
        }
        double[] quantiles = sketch.getQuantiles(psArray);

        errors = new double[m];
        if (calcError) {
            double errorVal = sketch.getNormalizedRankError();
            for (int i = 0; i < m; i++) {
                errors[i] = errorVal;
            }
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
