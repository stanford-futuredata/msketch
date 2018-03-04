import msolver.BoundSolver;
import msolver.data.ShuttleData;

public class BoundRuntimeBench {
    private static int numIters = 1000000;
    private static BoundSolver boundSolver;

    public static void main(String[] args) throws Exception {
        int k = 11;
        double[] powerSums = new double[k];
        for (int i = 0; i < powerSums.length; i++) {
            powerSums[i] = ShuttleData.powerSums[i];
        }

        // Check bounds match
        boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
        double boundLindsay = boundSolver.boundSizeLindsay(45);
        double boundRacz = boundSolver.boundSizeRacz(45);
        if (Math.abs(boundLindsay - boundRacz) > 1e-4) {
            System.out.format("Lindsay bound and Racz bound do not match: %f %f\n", boundLindsay, boundRacz);
        }

        // Warm start
        for (int i = 0; i < numIters / 2; i++) {
            boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
            boundSolver.boundSizeLindsay(45);
            boundSolver.boundSizeRacz(45);
        }

        bench(1);
        bench(10);
        bench(100);
    }

    public static void bench(int queriesPerSolver) {
        long startTime;
        long elapsed;

        startTime = System.nanoTime();
        for (int i = 0; i < numIters / queriesPerSolver; i++) {
            boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
            for (int j = 0; j < queriesPerSolver; j++) {
                boundSolver.boundSizeRacz(45);
            }
        }
        elapsed = System.nanoTime() - startTime;
        double secondsPerRacz = elapsed / (1.0e9 * numIters);
        System.out.format("Time Per Solve @%d queries per solver (Racz): %g\n", queriesPerSolver, secondsPerRacz);

        startTime = System.nanoTime();
        for (int i = 0; i < numIters / queriesPerSolver; i++) {
            boundSolver = new BoundSolver(ShuttleData.powerSums, ShuttleData.min, ShuttleData.max);
            for (int j = 0; j < queriesPerSolver; j++) {
                boundSolver.boundSizeLindsay(45);
            }
        }
        elapsed = System.nanoTime() - startTime;
        double secondsPerLindsay = elapsed / (1.0e9 * numIters);
        System.out.format("Time Per Solve @%d queries per solver (Lindsay): %g\n", queriesPerSolver, secondsPerLindsay);

        System.out.println("Speedup by using Lindsay (higher is better): "+secondsPerRacz/secondsPerLindsay);
        System.out.println("");
    }
}
