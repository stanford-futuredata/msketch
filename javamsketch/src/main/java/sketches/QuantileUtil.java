package sketches;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class QuantileUtil {
    public static double[] getTrueQuantiles(List<Double> ps, double[] data) {
        double[] expectedQs = new double[ps.size()];
        Percentile p = new Percentile().withEstimationType(Percentile.EstimationType.R_1);
        p.setData(data);
        for (int i = 0; i < ps.size(); i++) {
            expectedQs[i] = p.evaluate(ps.get(i)*100);
        }
        return expectedQs;
    }

    public static QuantileSketch trainAndMerge(
            Supplier<QuantileSketch> sFactory,
            ArrayList<double[]> cellData
    ) throws Exception {
        int n = cellData.size();
        ArrayList<QuantileSketch> sketches = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            QuantileSketch curSketch = sFactory.get();
            curSketch.initialize();
            curSketch.add(cellData.get(i));
            sketches.add(curSketch);
        }
        QuantileSketch mergedSketch = sFactory.get();
        mergedSketch.initialize();
        mergedSketch.merge(sketches);
        return mergedSketch;
    }
}
