import msketch.ChebyshevMomentSolver;
import msketch.data.RetailMoments;
import msketch.data.ShuttleMoments;

public class Main {
    public static void main(String[] args) throws Exception {
        int k = 11;
        double[] d_mus = new double[k];
        for (int i = 0; i < d_mus.length; i++) {
//            d_mus[i] = ShuttleMoments.moments[i];
            d_mus[i] = RetailMoments.moments[i];
        }

//        System.in.read();
        long startTime = System.nanoTime();
        int numIters = 300;
        int numFunctionEvals = 0;
        int numSteps = 0;
        int numDampedSteps = 0;
        double tol = 1e-9;
        for (int i = 0; i < numIters; i++) {
            ChebyshevMomentSolver solver = new ChebyshevMomentSolver(d_mus);
            solver.solve(tol);
            numSteps = solver.getStepCount();
            numDampedSteps = solver.getDampedStepCount();
            numFunctionEvals = solver.getNumFunctionEvals();
        }
        long elapsed = System.nanoTime() - startTime;
        double secondsPer = elapsed / (1.0e9 * numIters);
        System.out.println("Time Per Solve: "+secondsPer);
        System.out.println("Newton Steps: "+numSteps);
        System.out.println("Damped Newton Steps: "+numDampedSteps);
        System.out.println("Function Evals: "+numFunctionEvals);
    }
}
