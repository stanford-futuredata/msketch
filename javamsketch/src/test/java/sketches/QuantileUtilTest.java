package sketches;

import data.TestDataSource;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class QuantileUtilTest {
    @Test
    public void testTrueQuantiles() {
        double[] data = TestDataSource.getUniform(1001);
        double[] qs = QuantileUtil.getTrueQuantiles(Arrays.asList(.1, .5), data);
        assertEquals(100.0, qs[0], 0.0);
        assertEquals(500.0, qs[1], 0.0);
    }
}