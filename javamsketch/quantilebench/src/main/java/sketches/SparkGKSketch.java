package sketches;

import org.apache.spark.sql.catalyst.util.QuantileSummaries;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

public class SparkGKSketch implements QuantileSketch{
    private QuantileSummaries summary;
    private double sizeParam = 100.0;
    private double[] errors;

    @Override
    public String getName() {
        return "spark_gk";
    }

    @Override
    public int getSize() {
        return summary.sampled().length * (Double.BYTES + 2*Integer.BYTES);
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
        this.summary = new QuantileSummaries(
                QuantileSummaries.defaultCompressThreshold(),
                1.0/sizeParam,
                new QuantileSummaries.Stats[0],
                0
        );
    }

    @Override
    public void add(double[] data) {
        QuantileSummaries curSummary = this.summary;
        for (double d : data) {
            curSummary = curSummary.insert(d);
        }
        this.summary = curSummary.compress();
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches, int startIndex, int endIndex) {
        QuantileSummaries newSumm = this.summary;
        for (int i = startIndex; i < endIndex; i++) {
            SparkGKSketch gks = (SparkGKSketch) sketches.get(i);
            newSumm = newSumm.merge(gks.summary);
        }
        this.summary = newSumm.compress();
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        int m = ps.size();
        double[] quantiles = new double[m];
        for (int i = 0; i < m; i++) {
            Option<Object> res = summary.query(ps.get(i));
            if (res.isEmpty()) {
                throw new Exception("Bad GK Query");
            } else {
                quantiles[i] = (Double)res.get();
            }
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
        StringBuilder b = new StringBuilder();
        for (QuantileSummaries.Stats s : summary.sampled()) {
            b.append(s.value());
            b.append(":");
            b.append(s.delta()+"/"+s.g());
            b.append(",");
        }
        return b.toString();
    }
}
