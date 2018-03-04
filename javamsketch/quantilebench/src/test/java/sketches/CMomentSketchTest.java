package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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
    public void testWikiCell() throws Exception {
        CMomentSketch s = new CMomentSketch(1e-9);
        double min = 1.0;
        double max = 57040.000000;
        double[] sums = {
                10395.0, 1.0471506E8, 3.862161098896E12, 1.51617244847154464E17,
                6.042038827610059E21, 2.419472648115845E26, 10395.0, 60949.24720109948,
                472894.2826010175, 4205769.074129269, 4.015627275978303E7, 3.9816719593611807E8
        };
        s.setStats(
                min,
                max,
                Math.log(min),
                Math.log(max),
                Arrays.copyOfRange(sums, 0, 6),
                Arrays.copyOfRange(sums, 6, 12)
        );
        double[] qs = s.getQuantiles(Arrays.asList(.1, 0.5, .9));
        assertEquals(150, qs[1], 50);
    }

    @Test
    public void testLogUniform() throws Exception {
        CMomentSketch ms = new CMomentSketch(1e-9);
        ms.setSizeParam(9);
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