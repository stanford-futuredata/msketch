package sketches;

import com.tdunning.math.stats.Centroid;
import com.tdunning.math.stats.TDigest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TDigestSketch implements QuantileSketch {
    private TDigest td;
    private double compression = 20.0;
    private boolean enableErrors = false;
    private double[] errors;

    public TDigestSketch() {}

    protected TDigestSketch(double compression, TDigest td) {
        this.compression = compression;
        this.td = td;
    }

    @Override
    public String getName() {
        return "tdigest";
    }

    @Override
    public int getSize() {
        return td.byteSize();
    }

    @Override
    public double getSizeParam() {
        return compression;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.compression = sizeParam;
    }

    @Override
    public void setCalcError(boolean flag) {
        this.enableErrors = flag;
    }

    @Override
    public void initialize() {
//        this.td = TDigest.createDigest(compression);
        this.td = TDigest.createAvlTreeDigest(compression);
    }

    @Override
    public void add(double[] data) {
        for (double x : data) {
            this.td.add(x);
        }
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches, int startIndex, int endIndex) {
        TDigest newTD = this.td;
        for (int i = startIndex; i < endIndex; i++) {
            TDigestSketch ts = (TDigestSketch) sketches.get(i);
            newTD.add(ts.td);
        }
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        this.td.compress();
        int m = ps.size();
        double[] quantiles = new double[m];
        for (int i = 0; i < m; i++) {
            quantiles[i] = td.quantile(ps.get(i));
        }

        errors = new double[m];
        if (enableErrors) {
            for (int i = 0; i < m; i++) {
                errors[i] = quantileError(ps.get(i));
            }
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }

    public double quantileError(double p) throws Exception {
        long n = td.size();

        long countCutoff = Math.round(p * n);
        Collection<Centroid> centroids = this.td.centroids();
        long totalCount = 0;
        long lastCount = 0;
        for (Centroid c : centroids) {
            long curSize = c.count();
            totalCount += curSize;
            if (totalCount > countCutoff) {
                break;
            }
            lastCount = totalCount;
        }
        long d1 = totalCount - countCutoff;
        long d2 = countCutoff - lastCount;
        if (d1 > d2) {
            return d1*1.0/n;
        } else {
            return d2*1.0/n;
        }
    }
}
