import msketch.*;
import msketch.data.*;
import msketch.optimizer.NewtonOptimizer;
import sketches.CMomentSketch;
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
//        queryBench();
//        canonicalSolutionsBench();
        newSolverBench();
    }

    public static void newSolverBench() throws IOException {
        MomentData data = new OccupancyData();
        double[] range = {data.getMin(), data.getMax()};
        double[] logRange = {data.getLogMin(), data.getLogMax()};
        double[] powerSums = data.getPowerSums(7);
        double[] logSums = data.getLogSums(1);

        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                range[0], range[1], powerSums,
                logRange[0], logRange[1], logSums
        );
        solver.setVerbose(true);

        int numIters = 100;
        long startTime = System.nanoTime();
        for (int i = 0; i < numIters; i++) {
            solver.solve(1e-9);
        }
        long endTime = System.nanoTime();
        long elapsed = endTime - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        MaxEntPotential2 P = (MaxEntPotential2) solver.getOptimizer().getP();

        double[] ps = {.1, .5, .9};
        double[] qs = solver.estimateQuantiles(ps);
        System.out.println("Quantiles: " + Arrays.toString(qs));

        System.out.println("Total evals: " + P.getCumFuncEvals());
        System.out.println("Time Per Solve: " + secondsPer);
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
//        MomentData data = new RetailQuantityLogData();
        double[] range = {412.75,2076.5};
        double[] logRange = {6.022842082800238,7.638439063070808};
        double[] powerSums = {20560.0, 14197775.359523809, 11795382081.900866, 11920150330935.938, 14243310876969824.0, 1.9248869180998238e+19};
        double[] logSums = {20560.0, 132778.81355561133, 860423.75561972987, 5595528.9043199299, 36524059.16578535, 239323723.78677931};

        CMomentSketch ms = new CMomentSketch(1e-9);
        ms.setCalcError(true);
        ms.setStats(range[0], range[1], logRange[0], logRange[1], powerSums, logSums);
        int numIters = 1000;
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
