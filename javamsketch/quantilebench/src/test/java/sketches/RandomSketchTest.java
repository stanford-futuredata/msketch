package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class RandomSketchTest {
    @Test
    public void testUniformNoMerge() throws Exception {
        RandomSketch sketch = new RandomSketch();
        double sizeParam = 50.0;
        sketch.setSizeParam(sizeParam);
        sketch.initialize();

        int n = 20000;
        double[] data = TestDataSource.getUniform(n+1);
        sketch.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = sketch.getQuantiles(ps);

        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);

        assertArrayEquals(expectedQs, qs, n/sizeParam);
    }

//    @Test
//    public void testUniform() throws Exception {
//        RandomSketch rs = new RandomSketch();
//        rs.setSizeParam(20);
//        rs.initialize();
//
//        double[] data = TestDataSource.getUniform(10001);
//        rs.add(data);
//
//        List<Double> ps = Arrays.asList(.1, .5, .9);
//        double[] qs = rs.getQuantiles(ps);
//        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
//        assertArrayEquals(expectedQs, qs, 100.0);
//
//        DataGrouper grouper = new SeqDataGrouper(60);
//        ArrayList<double[]> cellData = grouper.group(data);
//        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
//                () -> {
//                    QuantileSketch newSketch = new TDigestSketch();
//                    newSketch.setSizeParam(20);
//                    return newSketch;
//                },
//                cellData
//        );
//        double[] qs2 = mergedSketch.getQuantiles(ps);
//        assertArrayEquals(expectedQs, qs2, 200.0);
//    }
}