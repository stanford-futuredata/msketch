package msketch;

import msketch.QuadraticPotential;
import org.junit.Test;

import static org.junit.Assert.*;

public class QuadraticPotentialTest {
    @Test
    public void testSimple() {
        QuadraticPotential qp = new QuadraticPotential(2);
        double[] x = {1.0, 2.0};
        qp.computeAll(x);
        assertEquals(5, qp.getValue(), 1e-10);

        double[] xMin = {0.0, 0.0};
        qp.computeAll(xMin);
        assertEquals(0, qp.getGradient()[1], 1e-10);
    }

}