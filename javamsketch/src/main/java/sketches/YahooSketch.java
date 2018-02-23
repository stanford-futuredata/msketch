package sketches;

import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.DoublesUnion;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class YahooSketch implements QuantileSketch, Serializable {
    private int k;
    public UpdateDoublesSketch sketch;
    private boolean calcError = true;
    private DoublesUnion union;
    private boolean sketchIsUpdated = true;

    private double[] errors;

    public YahooSketch() {}

    public YahooSketch(UpdateDoublesSketch s) {
        this.k = s.getK();
        this.sketch = s;
        this.union = DoublesUnion.builder().setMaxK(k).build();
        union.update(this.sketch);
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
        union = DoublesUnion.builder().setMaxK(k).build();
    }

    @Override
    public void add(double[] data) {
        for (double x : data) {
            sketch.update(x);
        }
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches) {
        DoublesUnion union = DoublesUnion.builder().setMaxK(k).build();
        union.update(this.sketch);
        for (QuantileSketch s : sketches) {
            YahooSketch ys = (YahooSketch)s;
            union.update(ys.sketch);
        }
        this.sketch = union.getResult();
        sketchIsUpdated = true;
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        if (!sketchIsUpdated) {
            sketch = union.getResult();
            sketchIsUpdated = true;
        }

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

    public long getCount() {
        if (!sketchIsUpdated) {
            sketch = union.getResult();
            sketchIsUpdated = true;
        }
        return sketch.getN();
    }

    public double getCDF(double x) {
        if (!sketchIsUpdated) {
            sketch = union.getResult();
            sketchIsUpdated = true;
        }
        return sketch.getCDF(new double[]{x})[0];
    }

    public YahooSketch mergeYahoo(ArrayList<YahooSketch> sketches) {
        for (YahooSketch ys : sketches) {
            union.update(ys.sketch);
        }
        sketchIsUpdated = false;
        return this;
    }

    public YahooSketch mergeYahoo(YahooSketch[] sketches) {
        for (YahooSketch ys : sketches) {
            union.update(ys.sketch);
        }
        sketchIsUpdated = false;
        return this;
    }

    public YahooSketch mergeYahoo(YahooSketch sketch) {
        union.update(sketch.sketch);
        sketchIsUpdated = false;
        return this;
    }


}
