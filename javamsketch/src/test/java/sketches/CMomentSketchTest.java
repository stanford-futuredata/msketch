package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import msketch.MathUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class CMomentSketchTest {
    @Test
    public void testUniform() throws Exception {
        CMomentSketch ms = new CMomentSketch(1e-10);
        ms.setSizeParam(7);
        ms.initialize();

        double[] data = TestDataSource.getUniform(0,1,10001);
        ms.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = ms.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 0.1);

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
    public void testLogUniform() throws Exception {
        CMomentSketch ms = new CMomentSketch(1e-9);
        ms.setSizeParam(5);
        ms.initialize();
        ms.setCalcError(true);

        double[] data = TestDataSource.getUniform(0,1,10001);
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.exp(data[i]);
        }
        ms.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = ms.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 0.1);
        assertTrue(ms.getErrors()[0] < .2);
    }
}