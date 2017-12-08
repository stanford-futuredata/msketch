import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        double[] d_mus = {1.        , -0.57071218, -0.22603818,  0.68115364, -0.60778341,
                0.17844626,  0.22018082, -0.29730968};

        long startTime = System.nanoTime();
        int numIters = 100;
        int totalFunctionEvals = 0;
        for (int i = 0; i < numIters; i++) {
            double l_values[] = {0, 0, 0, 0, 0, 0, 0, 0};
            ExpPolyFunction f = new ExpPolyFunction(l_values);
            f.solve(d_mus, 1e-9);
            totalFunctionEvals += f.getFuncEvals();
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Time Per Solve: "+secondsPer);
        System.out.println("Function Evals: "+totalFunctionEvals / numIters);
    }
}
