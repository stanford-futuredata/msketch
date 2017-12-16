package sketches;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.List;

public class TestSketchUtil {
    public static double[] getTrueQuantiles(List<Double> ps, double[] data) {
        double[] expectedQs = new double[ps.size()];
        Percentile p = new Percentile().withEstimationType(Percentile.EstimationType.R_1);
        p.setData(data);
        for (int i = 0; i < ps.size(); i++) {
            expectedQs[i] = p.evaluate(ps.get(i)*100);
        }
        return expectedQs;
    }
}
