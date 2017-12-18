import msketch.BoundSolver;
import msketch.ChebyshevMomentSolver;
import msketch.data.RetailPowerSums;
import msketch.data.ShuttleMoments;
import msketch.data.ShuttlePowerSums;
import msketch.optimizer.NewtonOptimizer;

public class MSketchBench {
    public static void main(String[] args) throws Exception {
        boundsBench(ShuttlePowerSums.powerSums, 11);
        boundsBench(RetailPowerSums.powerSums, 11);
        estimateBench();
    }

    public static void estimateBench() {
        int k = 11;
        double[] d_mus = new double[k];
        for (int i = 0; i < d_mus.length; i++) {
            d_mus[i] = ShuttleMoments.moments[i];
//            d_mus[i] = RetailMoments.moments[i];
        }

//        System.in.read();
        long startTime = System.nanoTime();
        int numIters = 5000;
        int numFunctionEvals = 0;
        int numSteps = 0;
        int numDampedSteps = 0;
        double tol = 1e-9;
        for (int i = 0; i < numIters; i++) {
            ChebyshevMomentSolver solver = new ChebyshevMomentSolver(d_mus);
//            solver.setVerbose(true);
            solver.solve(tol);
            NewtonOptimizer opt = solver.getOptimizer();
            numSteps = opt.getStepCount();
            numDampedSteps = opt.getDampedStepCount();
            numFunctionEvals = solver.getCumFuncEvals();
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Time Per Solve: "+secondsPer);
        System.out.println("Newton Steps: "+numSteps);
        System.out.println("Damped Newton Steps: "+numDampedSteps);
        System.out.println("Function Evals: "+numFunctionEvals);
    }

    public static void boundsBench(double[] ps, int k) {
        double[] powerSums = new double[k];
        for (int i = 0; i < powerSums.length; i++) {
            powerSums[i] = ps[i];
        }

        long startTime = System.nanoTime();
        int numIters = 5000;
        for (int i = 0; i < numIters; i++) {
            BoundSolver boundSolver = new BoundSolver(powerSums);
            boundSolver.quantileError(45, 0.5);
        }
        for (int i = 0; i < numIters; i++) {
            BoundSolver boundSolver = new BoundSolver(powerSums);
            boundSolver.quantileError(80, 0.95);
        }
        for (int i = 0; i < numIters; i++) {
            BoundSolver boundSolver = new BoundSolver(powerSums);
            boundSolver.quantileError(120, 0.99);
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Bounds Time Per Solve: "+secondsPer);
    }
}
