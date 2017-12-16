import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AccuracyBenchTest {
    @Test
    public void testSimple() throws Exception {
        AccuracyBench bench = new AccuracyBench("src/test/resources/acc_bench.json");
        List<Map<String, String>> results = bench.run();
        assertEquals(12, results.size());
    }

}