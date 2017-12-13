package msketch;

import msketch.data.ShuttleMoments;
import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChebyshevMomentSolverTest {
    @Test
    public void testUniform() {
        double m_values[] = {1.0, 0, -1.0/3, 0, -1.0/15, 0, -1.0/35};
        double tol = 1e-10;
        ChebyshevMomentSolver solver = new ChebyshevMomentSolver(m_values);
        solver.solve(tol);

        double[] coeffs = solver.getLambdas();

        assertEquals(FastMath.log(2), coeffs[0], 1e-10);
        for (int i = 1; i < coeffs.length; i++) {
            assertEquals(0.0, coeffs[i], 1e-10);
        }
        assertTrue(solver.getStepCount() < 20);
        assertEquals(0, solver.getDampedStepCount());
    }
    @Test
    public void testShuttle() {
        int k = 11;
        double[] m_values = new double[k];
        for (int i = 0; i < k; i++) {
            m_values[i] = ShuttleMoments.moments[i];
        }
        double tol = 1e-10;
        ChebyshevMomentSolver solver = new ChebyshevMomentSolver(m_values);
        solver.solve(tol);

        double[] coeffs = solver.getLambdas();
        MaxEntFunction f = new MaxEntFunction(coeffs);
        double[] f_mus = f.moments(k, tol);
        for (int i = 0; i < k; i++) {
            assertEquals(m_values[i], f_mus[i], 10*tol);
        }
        assertTrue(solver.getStepCount() < 20);
    }

}