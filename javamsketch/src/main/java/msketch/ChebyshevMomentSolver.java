package msketch;

public class ChebyshevMomentSolver {
    private double[] d_mus;
    private double[] lambdas;
    private double[] momentDeltas;

    private boolean verbose = false;
    private int stepCount;
    private int dampedStepCount;
    private int numFunctionEvals;

    public ChebyshevMomentSolver(double[] chebyshev_moments) {
        d_mus = chebyshev_moments;
    }
    public void setVerbose(boolean flag) {
        this.verbose = flag;
    }

    public int solve(double tol) {
        double[] l_initial = new double[d_mus.length];
        return solve(l_initial, tol);
    }

    public int solve(double[] l_initial, double tol) {
        double[] x = l_initial;
        MaxEntPotential P = new MaxEntPotential(d_mus);
        NewtonOptimizer opt = new NewtonOptimizer(P);
        opt.setVerbose(verbose);
        lambdas = opt.solve(x, tol);
        momentDeltas = P.getGradient();

        stepCount = opt.getStepCount();
        dampedStepCount = opt.getDampedStepCount();
        numFunctionEvals = P.getNumFuncEvals();
        return opt.getStepCount();
    }

    public double[] getLambdas() {
        return lambdas;
    }

    public double[] getMomentDeltas() {
        return momentDeltas;
    }

    public int getStepCount() {
        return stepCount;
    }

    public int getDampedStepCount() {
        return dampedStepCount;
    }

    public int getNumFunctionEvals() {
        return numFunctionEvals;
    }
}
