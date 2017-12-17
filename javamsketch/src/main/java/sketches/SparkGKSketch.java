package sketches;

import org.apache.spark.sql.catalyst.util.QuantileSummaries;
import scala.Array;

import java.util.List;

public class SparkGKSketch  implements QuantileSketch{
    private QuantileSummaries summary;

    @Override
    public String getName() {
        return "gk";
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public double getSizeParam() {
        return 0;
    }

    @Override
    public void setSizeParam(double sizeParam) {

    }

    @Override
    public void setCalcError(boolean flag) {

    }

    @Override
    public void initialize() {
        this.summary = new QuantileSummaries(
                1000,
                0.01,
                new QuantileSummaries.Stats[0],
                0
        );
    }

    @Override
    public void add(double[] data) {

    }

    @Override
    public QuantileSketch merge(QuantileSketch[] sketches) {
        return null;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        return new double[0];
    }

    @Override
    public double[] getErrors() {
        return new double[0];
    }
}
