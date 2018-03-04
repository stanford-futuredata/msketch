package sketches;

import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.List;

/**
 * Equi-width histograms with power-of-2 bucket widths for precise mergeabilitly.
 */
public class HistogramSketch implements QuantileSketch{
    private int k;

    private double bucketWidth = 0.0;
    private double startLoc = 0.0;
    private long[] counts;

    private double[] errors;

    @Override
    public String getName() {
        return "histogram";
    }

    @Override
    public int getSize() {
        return Integer.BYTES + Double.BYTES + Long.BYTES*k;
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
        return;
    }

    @Override
    public void initialize() {
        this.counts = new long[k];
    }

    @Override
    public void add(double[] data) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double x : data) {
            if (x > max) {
                max = x;
            }
            if (x < min) {
                min = x;
            }
        }
        double rawBucketWidth = (max-min)/(k-1);
        if (rawBucketWidth == 0) {
            startLoc = min;
            bucketWidth = 0.0;
            counts[0] += data.length;
            return;
        }

        bucketWidth = FastMath.pow(2.0, FastMath.ceil(FastMath.log(2.0, rawBucketWidth)));
        startLoc = FastMath.floor(min/bucketWidth) * bucketWidth;
        double invBucketWidth = 1.0/bucketWidth;
        int l = counts.length;
        for (double x : data) {
            int idx = (int)((x - startLoc) * invBucketWidth);
            if (idx == l) {
//                System.out.println(x+"::"+startLoc+":"+bucketWidth);
                counts[l-1]++;
            } else {
                counts[idx]++;
            }
        }

        return;
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (QuantileSketch rawSketch : sketches) {
            HistogramSketch curSketch = (HistogramSketch)rawSketch;
            double curMin = curSketch.startLoc;
            double curMax = curSketch.startLoc + k*curSketch.bucketWidth;
            if (curMin < min) {
                min = curMin;
            }
            if (curMax > max) {
                max = curMax;
            }
        }

        double rawBucketWidth = (max-min)/(k-1);
        if (rawBucketWidth == 0.0) {
            startLoc = min;
            bucketWidth = 0.0;
            for (QuantileSketch rawSketch : sketches) {
                HistogramSketch curSketch = (HistogramSketch)rawSketch;
                counts[0] += curSketch.counts[0];
            }
            return this;
        }
        double bucketWidth = FastMath.pow(2.0, FastMath.ceil(FastMath.log(2.0, rawBucketWidth)));
        double startLoc = FastMath.floor(min/bucketWidth) * bucketWidth;
        long[] counts = this.counts;
        int l = counts.length;

        for (QuantileSketch rawSketch : sketches) {
            HistogramSketch curSketch = (HistogramSketch)rawSketch;
            double lStartLoc = curSketch.startLoc;
            double lBucketWidth = curSketch.bucketWidth;
            long[] lCounts = curSketch.counts;
            for (int j = 0; j < l; j++) {
                double bucketStart = lStartLoc + j*lBucketWidth;
                int idx = (int)((bucketStart - startLoc) / bucketWidth);
                counts[idx] += lCounts[j];
            }
        }

        this.bucketWidth = bucketWidth;
        this.startLoc = startLoc;
//        System.out.println(this.startLoc);
//        System.out.println(this.bucketWidth);
//        System.out.println(Arrays.toString(counts));
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        int n = ps.size();

        long[] totalCounts = new long[k];
        totalCounts[0] = counts[0];
        for (int i = 1 ; i < k; i++) {
            totalCounts[i] = totalCounts[i-1] + counts[i];
        }
        long totalCount = totalCounts[k-1];

        double[] quantiles = new double[n];
        errors = new double[n];
        for (int i = 0; i < n; i++) {
            double p = ps.get(i);
            double targetRank = totalCount * p;

            double lastRank = 0.0;
            double curRank = 0.0;

            double targetIdx = k;
            double maxRankError = 0.0;
            for (int curIdx = 0; curIdx < k; curIdx++) {
                lastRank = curRank;
                curRank = totalCounts[curIdx];
                if (curRank >= targetRank) {
                    targetIdx = curIdx + (targetRank - lastRank) / (curRank - lastRank);
                    maxRankError = FastMath.max(targetRank - lastRank, curRank - targetRank);
                    break;
                }
            }

            quantiles[i] = targetIdx*bucketWidth + startLoc;
            errors[i] = maxRankError/totalCount;
        }

        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}
