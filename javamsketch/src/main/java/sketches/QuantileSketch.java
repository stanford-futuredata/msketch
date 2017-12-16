package sketches;

import java.util.List;

public interface QuantileSketch {
    String getName();
    int getSize();
    double getSizeParam();

    void setSizeParam(double sizeParam);
    void setCalcError(boolean flag);
    void initialize();

    void add(double[] data);
    QuantileSketch merge(QuantileSketch[] sketches);

    double[] getQuantiles(List<Double> ps) throws Exception;
    double[] getErrors();
}
