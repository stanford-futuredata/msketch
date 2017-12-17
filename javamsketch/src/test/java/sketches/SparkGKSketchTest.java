package sketches;

import org.apache.spark.sql.catalyst.util.QuantileSummaries;
import org.junit.Test;

import static org.junit.Assert.*;

public class SparkGKSketchTest {
    @Test
    public void testRaw() {
        QuantileSummaries summary = new QuantileSummaries(
                1000,
                0.01,
                new QuantileSummaries.Stats[0],
                0
        );
        for (int i = 0; i < 10000; i++) {
            summary.insert(i);
        }
        summary = summary.compress();
        System.out.println(summary.query(.5));
    }

}