package msketch;

import msketch.data.MomentData;
import msketch.data.RetailQuantityData;
import msketch.data.RetailQuantityLogData;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MnatSolverTest {
    @Test
    public void testUniform() {
        double[] m_values = {1.0, 1.0/2, 1.0/3, 1.0/4, 1.0/5, 1.0/6, 1.0/7};

        double[] cdf = MnatSolver.estimateCDF(m_values);
        double[] qs = MnatSolver.estimateQuantiles(0, 1, m_values, Arrays.asList(.2, .5, .8));
        double[] expectedQs = {.2, .5, .8};
        assertArrayEquals(expectedQs, qs, .2);
    }

    @Test
    public void testLogRetailQuantity() {
        MomentData data = new RetailQuantityData();
//        double[] moments = MathUtil.powerSumsToPosMoments(data.getPowerSums(5), data.getMin(), data.getMax());
        double[] moments = {1.0, .5, .25, .125, 1.0/16};
        System.out.println(Arrays.toString(moments));
        double[] pdf = MnatSolver.estimatePDF(moments);
        System.out.println(Arrays.toString(pdf));

        data = new RetailQuantityLogData();
        moments = MathUtil.powerSumsToPosMoments(data.getPowerSums(5), data.getMin(), data.getMax());
        pdf = MnatSolver.estimatePDF(moments);
        System.out.println(Arrays.toString(pdf));
    }

}