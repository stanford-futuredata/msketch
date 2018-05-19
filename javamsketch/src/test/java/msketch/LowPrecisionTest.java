package msketch;

import msketch.data.MilanData;
import msketch.data.MomentData;
import org.junit.Test;
import sketches.CMomentSketch;

import java.util.Arrays;

import static org.junit.Assert.*;

public class LowPrecisionTest {
//    @Test
//    public void testWeird() {
//        LowPrecision lp;
//
//        lp = new LowPrecision(12);
//        lp.encode(-1.960339, 3.715409, -11.349560, 1.312489,
//                new double[]{105000.0, 2048.625055727497, 106147.48176223612},
//                new double[]{51224.0, -29264.60717153238, 81804.1521212234});
//
//        lp = new LowPrecision(20);
//        lp.encode(-1.955355, 2.893550, -5.511830, 1.062484,
//                new double[]{1050.0, 76.0091322340304, 1088.6783648574456},
//                new double[]{518.0, -252.8682791737441, 721.2322109189713});
//        System.out.println(lp.min);
//    }

    @Test
    public void testCorrect() {
        LowPrecision lp;
        MilanData data = new MilanData();

        lp = new LowPrecision(16);
        lp.encode(data.getMin(), data.getMax(), data.getLogMin(), data.getLogMax(), data.getPowerSums(), data.getLogSums());
        checkCorrect(lp);

        lp = new LowPrecision(24);
        lp.encode(data.getMin(), data.getMax(), data.getLogMin(), data.getLogMax(), data.getPowerSums(), data.getLogSums());
        checkCorrect(lp);
    }

    private void checkCorrect(LowPrecision lp) {
        int numUsedBits = 1 + 11 + lp.bitsForSignificand;

        assertTrue(checkUnused(bits(lp.min), numUsedBits));
        assertTrue(checkUnused(bits(lp.max), numUsedBits));
        assertTrue(checkUnused(bits(lp.logMin), numUsedBits));
        assertTrue(checkUnused(bits(lp.logMax), numUsedBits));
        for (double val : lp.powerSums) {
            assertTrue(checkUnused(bits(val), numUsedBits));
        }
        for (double val : lp.logSums) {
            assertTrue(checkUnused(bits(val), numUsedBits));
        }

        int exponentRange = (int) Math.ceil(Math.log(getMax(lp)) / Math.log(2)) - lp.minExponent + 1;
        int expectedExponentBits = (int) Math.ceil(Math.log(exponentRange) / Math.log(2));
        assertEquals(expectedExponentBits, lp.bitsForExponent);
    }

    private String bits(double value) {
        return String.format("%64s", Long.toBinaryString(Double.doubleToRawLongBits(value))).replace(' ', '0');
    }

    private boolean checkUnused(String bits, int numUsedBits) {
        String zeros = new String(new char[64-numUsedBits]).replace("\0", "0");
        String unusedBits = bits.substring(numUsedBits);
        return zeros.equals(unusedBits);
    }

    private double getMax(LowPrecision lp) {
        double maxVal = Double.MIN_VALUE;
        for (double val : new double[]{lp.min, lp.max, lp.logMin, lp.logMax}) {
            double absVal = Math.abs(val);
            if (absVal > maxVal) maxVal = absVal;
        }
        for (double val : lp.powerSums) {
            double absVal = Math.abs(val);
            if (absVal > maxVal) maxVal = absVal;
        }
        for (double val : lp.logSums) {
            double absVal = Math.abs(val);
            if (absVal > maxVal) maxVal = absVal;
        }

        return maxVal;
    }

    @Test
    public void testMilan() {
        LowPrecision lp;
        double relError;
        MilanData data = new MilanData();

        lp = new LowPrecision(16);
        relError = 3.e-2;
        lp.encode(data.getMin(), data.getMax(), data.getLogMin(), data.getLogMax(), data.getPowerSums(), data.getLogSums());
        checkRelativeError(data, lp, relError);

        lp = new LowPrecision(24);
        relError = 1.e-4;
        lp.encode(data.getMin(), data.getMax(), data.getLogMin(), data.getLogMax(), data.getPowerSums(), data.getLogSums());
        checkRelativeError(data, lp, relError);
    }

    private void checkRelativeError(MomentData data, LowPrecision lp, double relError) {
        assertEquals(data.getMin(), lp.min, Math.abs(data.getMin()) * relError);
        assertEquals(data.getMax(), lp.max, Math.abs(data.getMax()) * relError);
        assertEquals(data.getLogMin(), lp.logMin, Math.abs(data.getLogMin()) * relError);
        assertEquals(data.getLogMax(), lp.logMax, Math.abs(data.getLogMax()) * relError);
        double[] powerSums = data.getPowerSums();
        for (int i = 0; i < powerSums.length; i++) {
            assertEquals(powerSums[i], lp.powerSums[i], Math.abs(powerSums[i]) * relError);
        }
        double[] logSums = data.getLogSums();
        for (int i = 0; i < logSums.length; i++) {
            assertEquals(logSums[i], lp.logSums[i], Math.abs(logSums[i]) * relError);
        }
    }

    private void print(MomentData data, LowPrecision lp) {
        System.out.println(lp.min);
        System.out.println(data.getMin());
        System.out.println(lp.max);
        System.out.println(data.getMax());
        System.out.println(lp.logMin);
        System.out.println(data.getLogMin());
        System.out.println(lp.logMax);
        System.out.println(data.getLogMax());
        System.out.println(Arrays.toString(lp.powerSums));
        System.out.println(Arrays.toString(data.getPowerSums()));
        System.out.println(Arrays.toString(lp.logSums));
        System.out.println(Arrays.toString(data.getLogSums()));

        System.out.println(Math.abs(lp.min - data.getMin()) / data.getMin());
        System.out.println(Math.abs(lp.max - data.getMax()) / data.getMax());
        System.out.println(Math.abs(lp.logMin - data.getLogMin()) / data.getLogMin());
        System.out.println(Math.abs(lp.logMax - data.getLogMax()) / data.getLogMax());
        double[] powerSums = data.getPowerSums();
        for (int i = 0; i < powerSums.length; i++) {
            System.out.println(Math.abs(lp.powerSums[i] - powerSums[i]) / powerSums[i]);
        }
        double[] logSums = data.getLogSums();
        for (int i = 0; i < logSums.length; i++) {
            System.out.println(Math.abs(lp.logSums[i] - logSums[i]) / logSums[i]);
        }
    }
}
