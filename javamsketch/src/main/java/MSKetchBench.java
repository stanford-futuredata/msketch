import msketch.BoundSolver;
import msketch.ChebyshevMomentSolver;
import msketch.data.ShuttleData;
import msketch.optimizer.NewtonOptimizer;

public class MSketchBench {
    public static void main(String[] args) throws Exception {
        quantileErrorBench();
        boundSizeBench();
        estimateBench();
    }

    public static void estimateBench() {
        int k = 11;
        double[] d_mus = new double[k];
        for (int i = 0; i < d_mus.length; i++) {
            d_mus[i] = ShuttleData.moments[i];
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

    public static void quantileErrorBench() {
        int k = 11;
        double[] powerSums = new double[k];
        for (int i = 0; i < powerSums.length; i++) {
            powerSums[i] = ShuttleData.powerSums[i];
        }

        long startTime = System.nanoTime();
        int numIters = 5000;
        for (int i = 0; i < numIters; i++) {
            BoundSolver boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
            boundSolver.quantileError(45, 0.5);
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Quantile Error Time Per Solve: "+secondsPer);
    }

    public static void boundSizeBench() {
        int k = 11;
        double[] powerSums = new double[k];
        for (int i = 0; i < powerSums.length; i++) {
            powerSums[i] = ShuttleData.powerSums[i];
        }

        long startTime = System.nanoTime();
        int numIters = 5000;
        for (int i = 0; i < numIters; i++) {
            BoundSolver boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
            boundSolver.boundSizeRacz(45);
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Bound Size Time Per Solve: "+secondsPer);
    }
}
