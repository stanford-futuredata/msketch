package msketch;

import msketch.optimizer.NewtonOptimizer;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MaxEntPotential2Test {
    @Test
    public void testSimple() {
        double[] lambdas = {0, -1, 1};
        double[] d_mus = {3.730021562141137, -0.45542618913430216, 2.0002734064304235};
        MaxEntPotential2 P = new MaxEntPotential2(
                true,
                2,
                d_mus,
                5.05,
                4.95,
                2.220446049250313e-16,
                2.302585092994046
        );
        P.computeAll(lambdas, 1e-8);
        assertEquals(0.40782188035828565, P.getHessian()[1][2], 1e-8);
        assertEquals(0, P.getGradient()[1], 1e-8);

        double[] l0 = {0, 0, 0};
        NewtonOptimizer optimizer = new NewtonOptimizer(P);
        l0 = optimizer.solve(l0, 1e-6);
        assertArrayEquals(lambdas, l0, 1e-6);
    }

    @Test
    public void testSimpleExp() {
        double[] lambdas = {0, 1, -1};
        double[] d_mus = {3.730021562141138, 0.2702518442892859, -2.1854477512754396};
        MaxEntPotential2 P = new MaxEntPotential2(
                false,
                2,
                d_mus,
                2.220446049250313e-16,
                2.302585092994046,
                5.05,
                4.95
        );
        P.computeAll(lambdas, 1e-8);
        double[] l0 = {0, 0, 0};
        NewtonOptimizer optimizer = new NewtonOptimizer(P);
        l0 = optimizer.solve(l0, 1e-6);
        assertArrayEquals(lambdas, l0, 1e-6);
    }

    @Test
    public void testPerf() {
        double[] lambdas = {0, -1, 0, 0, 1, 0, 0, 0};
        double[] d_mus = {3.730021562141137, -0.45542618913430216, -1.488108058594525, 0.4736377991372763, 2.0002734064304235, -0.6195034955141868, -1.3919328416578363, -0.8740690041785556};
        MaxEntPotential2 P = new MaxEntPotential2(
                true,
                4,
                d_mus,
                5.05,
                4.95,
                2.220446049250313e-16,
                2.302585092994046
        );
        int numIters = 2;
        long startTime = System.nanoTime();
        for (int i = 0; i < numIters; i++) {
            P.computeAll(lambdas, 1e-8);
        }
        long endTime = System.nanoTime();
        double timePerLoop = (endTime-startTime)*1.0/(numIters*1e9);
        System.out.println(timePerLoop);
    }
}