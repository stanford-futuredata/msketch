package msolver.thresholds;

import msolver.data.ExponentialData;
import msolver.data.MomentData;
import msolver.struct.MomentStruct;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThresholdCascadeTest {
    @Test
    public void testSimple() {
        MomentData data = new ExponentialData();
        MomentStruct m = new MomentStruct();
        m.min = data.getMin();
        m.max = data.getMax();
        m.logMin = data.getLogMin();
        m.logMax = data.getLogMax();
        m.powerSums = data.getPowerSums(10);
        m.logSums = data.getLogSums(10);

        ThresholdCascade tc = new ThresholdCascade(m);
        boolean flag = tc.threshold(2, .01);
        assertTrue(flag);

        flag = tc.threshold(4, .01);
        assertTrue(flag);
    }

}