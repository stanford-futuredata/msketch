package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import msolver.MathUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class MomentSketchTest {
    @Test
    public void testUniform() throws Exception {
        MomentSketch ms = new MomentSketch(1e-10);
        ms.setSizeParam(7);
        ms.initialize();

        double[] data = TestDataSource.getUniform(0,1,10001);
        ms.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = ms.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 1.0);

        DataGrouper grouper = new SeqDataGrouper(60);
        ArrayList<double[]> cellData = grouper.group(data);
        QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                () -> {
                    MomentSketch newMs = new MomentSketch(1e-10);
                    newMs.setSizeParam(7);
                    return newMs;
                },
                cellData
        );
        MomentSketch mmSketch = (MomentSketch)mergedSketch;
        double[] qs2 = mmSketch.getQuantiles(ps);

        assertArrayEquals(qs, qs2, 1e-7);
    }

    @Test
    public void testLogOccupancy() throws Exception {
        double[] powerSums = {
                20560.0, 132778.81355561252, 860423.7556197477, 5595528.904319964,
                3.652405916578557E7, 2.3932372378677437E8, 1.5744015769855406E9, 1.0399585507478048E10,
                6.898067822853244E10, 4.59495821550009E11, 3.073979747643975E12
        };
        double min = 6.022842082800238;
        double max = 7.638439063070808;

        double[] chebyMoments = MathUtil.powerSumsToChebyMoments(min, max, powerSums);
//        System.out.println(Arrays.toString(chebyMoments));
//        MomentSketch ms = new MomentSketch(1e-9);
//        ms.setStats(powerSums, min, max);
//
//        List<Double> ps = Arrays.asList(.1, .5, .9);
//        double[] qs = ms.getQuantiles(ps);
    }
}