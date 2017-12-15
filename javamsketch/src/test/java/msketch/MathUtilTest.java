package msketch;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MathUtilTest {
    @Test
    public void testBinomial() {
        long[][] binoms = MathUtil.getBinomials(5);
        assertEquals(binoms[5][2], 10L);
    }

    @Test
    public void testChebyCoefficient() {
        int[][] cCoeffs = MathUtil.getChebyCoefficients(5);
        int[] expected = {0, -3, 0, 4, 0, 0};
        assertArrayEquals(expected, cCoeffs[3]);
    }

    @Test
    public void testConvertMoments() {
        // integers from 0...1000
        double[] uniformPowerSums = {1001,500500,333833500,250500250000L};
        double[] convertedChebyshevMoments = MathUtil.powerSumsToChebyMoments(0, 1000, uniformPowerSums);

        double[] expectedChebyshevMoments = {1.0, 0, -.332, 0};
        assertArrayEquals(expectedChebyshevMoments, convertedChebyshevMoments, 1e-14);
    }

}