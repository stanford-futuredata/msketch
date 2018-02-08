package msketch.optimizer;

import org.apache.commons.math3.linear.*;

import java.util.Arrays;

/**
 * Minimizes a convex function using damped Newton's method.
 */
public class NewtonOptimizer {
    protected FunctionWithHessian P;
    protected int maxIter;

    protected int stepCount;
    protected boolean converged;
    protected int dampedStepCount;

    private double alpha = .3;
    private double beta = .25;
    private boolean verbose = false;

    public NewtonOptimizer(FunctionWithHessian P) {
        this.P = P;
        this.maxIter = 200;
        this.stepCount = 0;
        this.dampedStepCount = 0;
        this.converged = false;
    }
    public void setVerbose(boolean flag) {
        this.verbose = flag;
    }
    public void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }
    public int getStepCount() {
        return stepCount;
    }
    public boolean isConverged() {
        return converged;
    }
    public int getDampedStepCount() {
        return dampedStepCount;
    }

    public FunctionWithHessian getP() {
        return P;
    }

    private double getMSE(double[] error) {
        double sum = 0.0;
        for (int i = 0; i < error.length; i++) {
            sum += error[i]*error[i];
        }
        return sum / error.length;
    }

    public double[] solve(double[] start, double gradTol) {
        int k = P.dim();

        double[] x = start.clone();

        int step;
        P.computeAll(x, gradTol/4);

        double gradTol2 = gradTol * gradTol;
        converged = false;
        for (step = 0; step < maxIter; step++) {
            double PVal = P.getValue();
            double[] grad = P.getGradient();
            double[][] hess = P.getHessian();
            double mse = getMSE(grad);
            if (verbose) {
                System.out.println(String.format("Step: %3d GradRMSE: %10.5g P: %10.5g", step, Math.sqrt(mse), PVal));
            }
            if (mse < gradTol2) {
                converged = true;
                break;
            }
            RealMatrix hhMat = new Array2DRowRealMatrix(hess, false);
            RealVector stepVector;
            try {
                CholeskyDecomposition d = new CholeskyDecomposition(
                        hhMat,
                        0,
                        0
                );
                stepVector = d.getSolver().solve(new ArrayRealVector(grad));
            } catch (Exception e) {
                // Cholesky is faster but fall back to SVD if it doesn't work
                SingularValueDecomposition d = new SingularValueDecomposition(hhMat);
                stepVector = d.getSolver().solve(new ArrayRealVector(grad));
            }
            stepVector.mapMultiplyToSelf(-1.0);

            double dfdx = 0.0;
            for (int i = 0; i < k; i++) {
                dfdx += stepVector.getEntry(i) * grad[i];
            }

            double stepScaleFactor = 1.0;
            double[] newX = new double[k];
            for (int i = 0; i < k; i++) {
                newX[i] = x[i] + stepScaleFactor * stepVector.getEntry(i);
            }
            // optimistically choose precision to be size of expected step
            double requiredPrecision = Math.max(
                    gradTol,
                    Math.abs(alpha*stepScaleFactor*dfdx)
            ) / 10;
            // Warning: this overwrites grad and hess
            P.computeAll(newX, requiredPrecision);

            // do not look for damped steps if we are near stationary point
            if (dfdx*dfdx > gradTol2) {
                while (true) {
                    double f1 = P.getValue();
                    double delta = PVal + alpha * stepScaleFactor * dfdx - f1;
                    if (delta >= -gradTol) {
                        break;
                    } else {
                        stepScaleFactor *= beta;
                    }
                    for (int i = 0; i < k; i++) {
                        newX[i] = x[i] + stepScaleFactor * stepVector.getEntry(i);
                    }
                    // When we are taking small damped steps be conservative with precision
                    requiredPrecision = gradTol / 10;
                    P.computeAll(newX, requiredPrecision);
                }
            }
            if (stepScaleFactor < 1.0) {
                dampedStepCount++;
            }
            if (verbose) {
                if (stepScaleFactor < 1.0) {
                    System.out.println("Step Size: " + stepScaleFactor);
                }
            }
            System.arraycopy(newX, 0, x, 0, k);
        }
        stepCount = step;
        return x;

    }
}
