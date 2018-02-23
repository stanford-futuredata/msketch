package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SamplingSketchTest {
    @Test
    public void testSimple() throws Exception {
        SamplingSketch sketch = new SamplingSketch();
        sketch.setSizeParam(1000);
        sketch.initialize();

        double[] data = TestDataSource.getUniform(10001);
        sketch.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = sketch.getQuantiles(ps);

        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 1000.0);

        DataGrouper grouper = new SeqDataGrouper(60);
        ArrayList<double[]> cellData = grouper.group(data);
        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                () -> {
                    QuantileSketch newSketch = new SamplingSketch();
                    newSketch.setSizeParam(1000);
                    return newSketch;
                },
                cellData
        );
        double[] qs2 = mergedSketch.getQuantiles(ps);

        assertArrayEquals(expectedQs, qs2, 1000.0);
    }

}