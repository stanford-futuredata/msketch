package sketches;

import java.util.ArrayList;
import java.util.List;

public interface QuantileSketch {
    String getName();
    int getSize();
    double getSizeParam();

    void setSizeParam(double sizeParam);
    void setCalcError(boolean flag);
    default void setVerbose(boolean flag) {return;}
    void initialize();

    void add(double[] data);
    QuantileSketch merge(ArrayList<QuantileSketch> sketches);

    double[] getQuantiles(List<Double> ps) throws Exception;
    double[] getErrors();
}
