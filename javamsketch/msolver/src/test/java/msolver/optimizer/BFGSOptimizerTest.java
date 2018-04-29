package msolver.optimizer;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class BFGSOptimizerTest {
    @Test
    public void testQuadratic() {
        QuadraticPotential qp = new QuadraticPotential(2);
        BFGSOptimizer opt = new BFGSOptimizer(qp);
        opt.setVerbose(false);
        double[] start = {1.0, 2.0};
        double[] solution = opt.solve(start, 1e-10);
        for (int i = 0; i < start.length; i++) {
            assertEquals(0.0, solution[i], 1e-10);
        }
    }
}