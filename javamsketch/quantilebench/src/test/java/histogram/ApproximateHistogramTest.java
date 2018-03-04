package histogram;

import data.TestDataSource;
import org.junit.Test;

import java.util.Arrays;

public class ApproximateHistogramTest {
    @Test
    public void testSimple() {
        ApproximateHistogram h = new ApproximateHistogram(
                100
        );
        double[] data = TestDataSource.getUniform(10001);
        for (double x : data) {
            h.offer((float)x);
        }
        float[] ps = {.1f, .5f, .9f};
        float[] qs = h.getQuantiles(ps);
        System.out.println(Arrays.toString(qs));
    }

}