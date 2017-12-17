package sketches;

import data.TestDataSource;
import org.junit.Test;

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
        assertArrayEquals(expectedQs, qs, 400.0);
    }

}