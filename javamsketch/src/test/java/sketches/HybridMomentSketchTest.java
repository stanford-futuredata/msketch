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
        int n = 1000000;
        double[] data = source.sample(n);
        data[0] = -1;
        data[1] = 0.0;
        HybridMomentSketch sketch = new HybridMomentSketch(1e-8);
        sketch.setSizeParam(11);
        sketch.initialize();
        sketch.add(data);
        List<Double> ps = Arrays.asList(.01, .2, .5, .8, .99);
        double[] qs = sketch.getQuantiles(ps);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        assertArrayEquals(expectedQs, qs, .2);
        double[] errors = sketch.getErrors();
        for (double curError : errors) {
            assertTrue((curError < .3) && (curError > 0));
        }
    }

    @Test
    public void testUnusualLog2() throws Exception {
        double[] powerSums = {1000000.0, 1652735.3916750294, 7556782.3164341925, 1.0834717917136438E8, 6.289689194886326E9, 9.432139208415868E11, 1.9285688383144788E14, 4.3129632568547192E16, 9.95180828268E18, 2.32585318948916E21, 5.4670626718836166E23};
        double[] logSums = {999998.0, 893.5046947201799, 1000688.4702974836, 6918.641148454149, 3015321.6089141453, 51658.80472632411, 1.5229415856258227E7, 455919.9402806272, 1.0921006413605675E8, 4169809.9003732745, 1.0394860969752378E9};
        double min = -1.0;
        double max = 236.33702261357814;
        double logMin = -5.583752834843317;
        double logMax = 5.465258848236864;
        HybridMomentSketch sketch = new HybridMomentSketch(1e-8);
        sketch.setStats(powerSums, logSums, min, max, logMin, logMax);
        List<Double> ps = Arrays.asList(.01, .2, .5, .8, .99);
        double[] qs = sketch.getQuantiles(ps);
        double[] errors = sketch.getErrors();
        for (double curError : errors) {
            assertTrue((curError < .3) && (curError > 0));
        }

    }

    @Test
    public void testUnusualLog() throws Exception {
        double[] powerSums = {1000000.0, 1651635.6610583442, 7426470.3818426095, 9.699014686460353E7, 4.482143026437266E9, 5.314711622775991E11, 8.92158094096364E13, 1.6708614942289482E16, 3.2656766247329797E18, 6.512870665201928E20, 1.3127700722550198E23};
        double[] logSums = {999998.0, 1999.8742587751233, 999649.8829994559, 4559.973586570845, 3004799.465295013, 9161.708805759514, 1.5037391982759036E7, -60694.192177750294, 1.0551369967089364E8, -1016502.7484347977, 9.616654629886193E8};
        double min = -1.0;
        double max = 204.55537744352497;
        double logMin = -5.015434793743874;
        double logMax = 5.320838733169551;
        HybridMomentSketch sketch = new HybridMomentSketch(1e-8);
        sketch.setStats(powerSums, logSums, min, max, logMin, logMax);
        List<Double> ps = Arrays.asList(.01, .2, .5, .8, .99);
        double[] qs = sketch.getQuantiles(ps);
        double[] errors = sketch.getErrors();
        for (double curError : errors) {
            assertTrue((curError < .3) && (curError > 0));
        }
    }

}