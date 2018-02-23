package msketch;

import msketch.data.MilanData;
import msketch.data.MomentData;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class LowPrecisionTest {
    @Test
    public void testMilan() {
        LowPrecision lp;
        double relError;
        MilanData data = new MilanData();

        lp = new LowPrecision(16);
        relError = 2.e-2;
        lp.encode(data.getMin(), data.getMax(), data.getLogMin(), data.getLogMax(), data.getPowerSums(), data.getLogSums());
        checkRelativeError(data, lp, relError);

        lp = new LowPrecision(24);
        relError = 1.e-4;
        lp.encode(data.getMin(), data.getMax(), data.getLogMin(), data.getLogMax(), data.getPowerSums(), data.getLogSums());
        checkRelativeError(data, lp, relError);
    }

    private void checkRelativeError(MomentData data, LowPrecision lp, double relError) {
        assertEquals(lp.min, data.getMin(), Math.abs(data.getMin()) * relError);
        assertEquals(lp.max, data.getMax(), Math.abs(data.getMax()) * relError);
        assertEquals(lp.logMin, data.getLogMin(), Math.abs(data.getLogMin()) * relError);
        assertEquals(lp.logMax, data.getLogMax(), Math.abs(data.getLogMax()) * relError);
        double[] powerSums = data.getPowerSums();
        for (int i = 0; i < powerSums.length; i++) {
            assertEquals(lp.powerSums[i], powerSums[i], Math.abs(powerSums[i]) * relError);
        }
        double[] logSums = data.getLogSums();
        for (int i = 0; i < logSums.length; i++) {
            assertEquals(lp.logSums[i], logSums[i], Math.abs(logSums[i]) * relError);
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
