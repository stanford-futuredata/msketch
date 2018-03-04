package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class TDigestSketchTest {
    @Test
    public void testUniform() throws Exception {
        TDigestSketch ts = new TDigestSketch();
        ts.setSizeParam(20);
        ts.initialize();

        double[] data = TestDataSource.getUniform(10001);
        ts.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = ts.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 100.0);

        DataGrouper grouper = new SeqDataGrouper(60);
        ArrayList<double[]> cellData = grouper.group(data);
        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                () -> {
                    QuantileSketch newSketch = new TDigestSketch();
                    newSketch.setSizeParam(20);
                    return newSketch;
                },
                cellData
        );
        double[] qs2 = mergedSketch.getQuantiles(ps);
        assertArrayEquals(expectedQs, qs2, 200.0);
    }
}