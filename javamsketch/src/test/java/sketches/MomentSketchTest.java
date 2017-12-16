package sketches;

import data.TestDataSource;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MomentSketchTest {
    @Test
    public void testUniform() throws Exception {
        MomentSketch ms = new MomentSketch(1e-10);
        ms.setSizeParam(7);
        ms.initialize();

        double[] data = TestDataSource.getUniform(10001);
        ms.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = ms.getQuantiles(ps);
        double[] expectedQs = TestSketchUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, 1.0);
    }

}