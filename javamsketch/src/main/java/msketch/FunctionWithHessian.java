package msketch;

public interface FunctionWithHessian {
    void computeAll(double[] point, double tol);
    int dim();
    double getValue();
    double[] getGradient();
    // Returns in row-major order
    double[][] getHessian();
}
