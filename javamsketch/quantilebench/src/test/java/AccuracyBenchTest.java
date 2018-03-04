import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AccuracyBenchTest {
    @Test
    public void testSimple() throws Exception {
        AccuracyBench bench = new AccuracyBench("src/test/resources/acc_bench.json");
        List<Map<String, String>> results = bench.run();
        assertEquals(12*2, results.size());
    }

}