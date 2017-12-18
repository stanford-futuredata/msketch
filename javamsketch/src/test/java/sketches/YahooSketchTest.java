package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class YahooSketchTest {
    @Test
    public void testUniform() throws Exception {
        YahooSketch s = new YahooSketch();
        s.setSizeParam(1024.0);
        s.initialize();

        double[] data = TestDataSource.getUniform(10001);
        s.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = s.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 10.0);

        DataGrouper grouper = new SeqDataGrouper(60);
        ArrayList<double[]> cellData = grouper.group(data);
        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                () -> {
                    QuantileSketch newSketch = new YahooSketch();
                    newSketch.setSizeParam(1024.0);
                    return newSketch;
                },
                cellData
        );
        double[] qs2 = mergedSketch.getQuantiles(ps);
        assertArrayEquals(expectedQs, qs2, 10.0);
    }
}