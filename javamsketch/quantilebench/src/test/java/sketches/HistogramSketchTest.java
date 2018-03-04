package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class HistogramSketchTest {
    @Test
    public void testSimple() throws Exception {
        HistogramSketch hs = new HistogramSketch();
        hs.setSizeParam(10);
        hs.initialize();

        double[] data = TestDataSource.getUniform(10001);
        hs.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = hs.getQuantiles(ps);

        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 200.0);

        double[] errors = hs.getErrors();
        for (double e : errors) {
            assertTrue(e < .15);
        }
    }

    @Test
    public void testMerge() throws Exception {
        HistogramSketch hs1 = new HistogramSketch();
        hs1.setSizeParam(10);
        hs1.initialize();
        double[] data = TestDataSource.getUniform(10001);
        hs1.add(data);

        DataGrouper grouper = new SeqDataGrouper(100);
        ArrayList<double[]> cellData = grouper.group(data);
        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                () -> {
                    QuantileSketch newSketch = new HistogramSketch();
                    newSketch.setSizeParam(10);
                    return newSketch;
                },
                cellData
        );

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] origQuantiles = hs1.getQuantiles(ps);
        double[] mergedQuantiles = mergedSketch.getQuantiles(ps);
        assertArrayEquals(origQuantiles, mergedQuantiles, 0.0);

    }

}