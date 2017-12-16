package sketches;

import data.TestDataSource;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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
        assertArrayEquals(expectedQs, qs, 1.0);
    }
}