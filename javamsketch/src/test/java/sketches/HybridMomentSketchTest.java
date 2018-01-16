package sketches;

import data.TestDataSource;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class HybridMomentSketchTest {
    @Test
    public void testSimple() throws Exception {
        double[] data = TestDataSource.getUniform(.1, 10, 100);
        HybridMomentSketch sketch = new HybridMomentSketch(1e-8);
        sketch.setSizeParam(5);
        sketch.initialize();
        sketch.add(data);
        List<Double> ps = Arrays.asList(.01, .5, .99);
        double[] qs = sketch.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, .5);
        double[] es = sketch.getErrors();
        assertEquals(ps.size(), es.length);
        for (double e : es) {
            assertEquals(0.15, e, .1);
        }
    }

    @Test
    public void testSimpleNegative() throws Exception {
        double[] data = TestDataSource.getUniform(-5, 5, 101);
        HybridMomentSketch sketch = new HybridMomentSketch(1e-8);
        sketch.setSizeParam(5);
        sketch.initialize();
        sketch.add(data);
        List<Double> ps = Arrays.asList(.01, .5, .99);
        double[] qs = sketch.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, .5);
    }

    @Test
    public void testLogWithNegative() throws Exception {
        LogNormalDistribution source = new LogNormalDistribution(0, 1);
        int n = 100000;
        double[] data = source.sample(n);
        data[0] = -1;
        data[1] = 0.0;
        HybridMomentSketch sketch = new HybridMomentSketch(1e-8);
        sketch.setSizeParam(10);
        sketch.initialize();
        sketch.add(data);
        List<Double> ps = Arrays.asList(.01, .2, .5, .7, .99);
        double[] qs = sketch.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, .2);
    }

}