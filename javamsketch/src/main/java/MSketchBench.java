import msketch.ChebyshevMomentSolver;
import msketch.MathUtil;
import msketch.SimpleBoundSolver;
import msketch.data.*;
import msketch.optimizer.NewtonOptimizer;
import sketches.HybridMomentSketch;
import sketches.MomentSketch;
import sketches.QuantileSketch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MSketchBench {
    public static void main(String[] args) throws Exception {
//        quantileErrorBench();
//        boundSizeBench();
//        estimateBench();
//        mergeBench();
        queryBench();
//        canonicalSolutionsBench();
    }

    public static void mergeBench() {
        int k = 11;
        int numMerges = 100000000;
        int numIters = 10;

        HybridMomentSketch ms = new HybridMomentSketch(1e-9);
        ms.setSizeParam(k);
        ms.initialize();
        ArrayList<QuantileSketch> sketches = new ArrayList<>(numMerges);
        for (int i = 0; i < numMerges; i++) {
            sketches.add(ms);
        }
        System.out.println("Initialized");

        long startTime = System.nanoTime();
        for (int i = 0; i < numIters; i++) {
            HybridMomentSketch merged = new HybridMomentSketch(1e-9);
            merged.setSizeParam(11);
            merged.initialize();

            merged.merge(sketches);
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters * numMerges);
        System.out.println("Time Per Merge: "+secondsPer);
    }

    public static void canonicalSolutionsBench() throws IOException {
        int k = 7;
        MomentData data = new RetailQuantityLogData();
        double[] moments = MathUtil.powerSumsToPosMoments(data.getPowerSums(k), data.getMin(), data.getMax());

        double[] limits = {0.0, 1.0};
        SimpleBoundSolver solver = new SimpleBoundSolver(k);

        int numIters = 200000;
        long startTime = System.nanoTime();
        for (int i = 0; i < numIters; i++) {
            SimpleBoundSolver.CanonicalDistribution[] sols = solver.getCanonicalDistributions(
                    moments,
                    limits
            );
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Time Per Solve: "+secondsPer);
   }

    public static void queryBench() throws Exception {
        int k = 11;
//        MomentData pData = new RetailQuantityData();
//        MomentData lData = new RetailQuantityLogData();
//        MomentData data = new UniformData();
        MomentData data = new RetailQuantityLogData();
//        HybridMomentSketch ms = new HybridMomentSketch(1e-9);
        MomentSketch ms = new MomentSketch(1e-6);
        ms.setVerbose(false);
        ms.setCalcError(true);
        ms.setStats(data.getPowerSums(k), data.getMin(), data.getMax());
//        ms.setStats(
//                pData.getPowerSums(k),
//                lData.getPowerSums(k),
//                pData.getMin(),
//                pData.getMax(),
//                lData.getMin(),
//                lData.getMax()
//        );
        int numIters = 500;
        ArrayList<Double> ps = new ArrayList<>();
        ps.add(.5);
        long startTime = System.nanoTime();
        for (int i = 0; i < numIters; i++) {
            ms.getQuantiles(ps);
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Time Per Solve: "+secondsPer);
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
        double[] moments = MathUtil.powerSumsToMoments(powerSums);

        long startTime = System.nanoTime();
        int numIters = 50000;
        SimpleBoundSolver boundSolver = new SimpleBoundSolver(k);
        double[] xs = {45.0};
        double[] ps = {.5};
        for (int i = 0; i < numIters; i++) {
            double[] boundSizes = boundSolver.solveBounds(moments, xs);
            boundSolver.getMaxErrors(moments, xs, ps, boundSizes);
//            BoundSolver boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
//            boundSolver.quantileError(45, 0.5);
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
        double[] moments = MathUtil.powerSumsToMoments(powerSums);

        long startTime = System.nanoTime();
        int numIters = 500000;
        SimpleBoundSolver boundSolver = new SimpleBoundSolver(k);
        double[] xs = {45.0};
        for (int i = 0; i < numIters; i++) {
            boundSolver.solveBounds(moments, xs);
//            BoundSolver boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
//            boundSolver.boundSizeRacz(45);
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Bound Size Time Per Solve: "+secondsPer);
    }
}
