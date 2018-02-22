package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ApproximateHistogramSketchTest {
    @Test
    public void testUniform() throws Exception {
        ApproximateHistogramSketch s = new ApproximateHistogramSketch();
        s.setSizeParam(1000);
        s.initialize();

        double[] data = TestDataSource.getUniform(10001);
        s.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = s.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 100.0);

        DataGrouper grouper = new SeqDataGrouper(60);
        ArrayList<double[]> cellData = grouper.group(data);
        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                () -> {
                    QuantileSketch newSketch = new ApproximateHistogramSketch();
                    newSketch.setSizeParam(1000.0);
                    return newSketch;
                },
                cellData
        );
        double[] qs2 = mergedSketch.getQuantiles(ps);
        assertArrayEquals(expectedQs, qs2, 10.0);
        System.out.println(Arrays.toString(qs2));
    }
}