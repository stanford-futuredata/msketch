package msketch;

import msketch.data.RetailData;
import msketch.data.ShuttleData;
import org.junit.Test;

import static org.junit.Assert.*;

public class BoundSolverTest {
    @Test
    public void testUniform() {
        // 101 evenly spaced points between 0 and 1, inclusive
        double[] values = new double[101];
        for (int i = 0; i <= 100; i++) {
            values[i] = 0.01 * i;
        }

        int k = 7;
        double m_values[] = new double[k];
        for (int i = 0; i < k; i++) {
            double moment = 0.0;
            for (double v : values) {
                moment += Math.pow(v, i);
            }
            m_values[i] = moment;
        }

        BoundSolver solver = new BoundSolver(m_values, 0, 100);
        double error = solver.quantileError(0.5, 0.5);
        assertEquals(error, 0.22, 0.01);
    }

    @Test
    public void testShuttle() {
        int k = 11;
        double[] m_values = new double[k];
        for (int i = 0; i < k; i++) {
            m_values[i] = ShuttleData.powerSums[i];
        }

        BoundSolver solver = new BoundSolver(m_values, ShuttleData.min, ShuttleData.max);
        double error = solver.quantileError(45, 0.5);
        assertEquals(error, 0.2, 0.01);
    }

    @Test
    public void testBoundsDecrease() {
        double prevQError = 1.0;
        for (int k = 3; k <= 11; k++) {
            double[] m_values = new double[k];
            for (int i = 0; i < k; i++) {
                m_values[i] = ShuttleData.powerSums[i];
            }

            BoundSolver solver = new BoundSolver(m_values, ShuttleData.min, ShuttleData.max);
            double error = solver.quantileError(45, 0.5);
            assertTrue(error <= prevQError);
            prevQError = error;
        }
    }

    @Test
    public void testMarkov() {
        int k = 2;
        double[] m_values = new double[k];
        for (int i = 0; i < k; i++) {
            m_values[i] = ShuttleData.powerSums[i];
        }

        BoundSolver solver = new BoundSolver(m_values, ShuttleData.min, ShuttleData.max);
        double error = solver.quantileError(100, 0.8);
        assertEquals(error, 0.2, 0.01);
    }

    @Test
    public void testRetail() {
        int k = 11;
        double[] m_values = new double[k];
        for (int i = 0; i < k; i++) {
            m_values[i] = RetailData.powerSums[i];
        }

        BoundSolver solver = new BoundSolver(m_values, RetailData.min, RetailData.max);
        double error = solver.quantileError(45, 0.9);
        assertEquals(error, 0.14, 0.01);
    }
}