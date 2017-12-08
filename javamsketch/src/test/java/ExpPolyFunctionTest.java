import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ExpPolyFunctionTest {
    @Test
    public void testChebyshev() {
        double[] coeff = {1.0, 2.0, 3.0};
        ExpPolyFunction f = new ExpPolyFunction(coeff);
        assertEquals(2.34, f.chebyshev_poly(.7), 1e-11);
    }

    @Test
    public void testMoments() {
        double[] coeff = {1.0, 2.0, 3.0};
        ExpPolyFunction f = new ExpPolyFunction(coeff);
        double[] moments = f.moments(8, 1e-9);
        double[] expectedMoments = {
            6.303954641290793, -1.0395877292934701,
            -4.9297352972133845, 2.0119170973456093,
            2.458369282294647, -1.5127916121976486,
            -0.84272224125321182, 0.73491729283435847
        };
        for (int i = 0; i < moments.length; i++) {
            assertEquals(expectedMoments[i], moments[i], 1e-10);
        }
        assertTrue(f.getFuncEvals() < 10000);
    }

    @Test
    public void testSolve() {
        double m_values[] = {1.0, 0, -1.0/3, 0, -1.0/15, 0, -1.0/35};
        double l_values[] = {0.0, 0, 0, 0, 0, 0, 0};
        ExpPolyFunction f = new ExpPolyFunction(l_values);
        f.solve(m_values, 1e-8);
        double[] coeffs = f.getCoeffs();
        assertEquals(FastMath.log(2), coeffs[0], 1e-10);
        for (int i = 1; i < coeffs.length; i++) {
            assertEquals(0.0, coeffs[i], 1e-10);
        }
    }
}