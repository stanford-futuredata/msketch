package sketches;

import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.DoublesUnion;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

import java.util.List;

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
        sketch.compact();
    }

    @Override
    public QuantileSketch merge(QuantileSketch[] sketches) {
        YahooSketch[] ySketches = (YahooSketch[]) sketches;
        int k = ySketches[0].k;
        DoublesUnion union = DoublesUnion.builder().setMaxK(k).build();
        for (YahooSketch ys : ySketches) {
            union.update(ys.sketch);
        }
        UpdateDoublesSketch res = union.getResult();
        return new YahooSketch(res);
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
        double errorVal = sketch.getNormalizedRankError();
        for (int i = 0; i < m; i++) {
            errors[i] = errorVal;
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
