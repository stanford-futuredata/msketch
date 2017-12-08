import org.junit.Test;

import static org.junit.Assert.*;

public class WeightedFunctionTest {
    @Test
    public void testSimple() {
        WeightedFunction f = new WeightedFunction(
                (x) -> 1,
                3
        );
        assertEquals(0.296, f.value(-.1), 1e-11);
    }
}