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

//        System.out.println(Arrays.toString(qs));

        assertArrayEquals(expectedQs, qs, n/sizeParam);
    }

//    @Test
//    public void testUniformWithMerge() throws Exception {
//        double sizeParam = 50.0;
//        int n = 20000;
//        double[] data = TestDataSource.getUniform(n+1);
//
//        List<Double> ps = Arrays.asList(.1, .5, .9);
//        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
//
//        DataGrouper grouper = new SeqDataGrouper(10000);
//        ArrayList<double[]> cellData = grouper.group(data);
//        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
//                () -> {
//                    RandomSketch newSketch = new RandomSketch();
//                    newSketch.setSizeParam(sizeParam);
//                    return newSketch;
//                },
//                cellData
//        );
//        double[] qs2 = mergedSketch.getQuantiles(ps);
//
////        System.out.println(Arrays.toString(qs2));
////        System.out.println(((RandomSketch)mergedSketch).b);
////        System.out.println(((RandomSketch)mergedSketch).s);
//
//        assertArrayEquals(expectedQs, qs2, n/sizeParam);
//    }
}