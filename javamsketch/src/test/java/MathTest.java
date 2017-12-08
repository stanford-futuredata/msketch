import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.junit.Test;

public class MathTest {
    @Test
    public void testIntegration() {
        RombergIntegrator r = new RombergIntegrator(
                0.0,
                1e-8,
                10,
                100
        );
    }

}
