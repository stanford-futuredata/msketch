package msketch;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;

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
    public boolean converged() {
        return converged;
    }
    public int getDampedStepCount() {
        return dampedStepCount;
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
        for (step = 0; step < maxIter; step++) {
            double PVal = P.getValue();
            double[] grad = P.getGradient();
            double[][] hess = P.getHessian();
            double mse = getMSE(grad);
            if (verbose) {
                System.out.println("Step: " + step);
                System.out.println("Grad: " + Arrays.toString(grad));
            }
            if (mse < gradTol2) {
                converged = true;
                break;
            }
            RealMatrix hhMat = new Array2DRowRealMatrix(hess, false);
            CholeskyDecomposition d = new CholeskyDecomposition(
                    hhMat, 0, 0);
            RealVector stepVector = d.getSolver().solve(new ArrayRealVector(grad));
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
            // we need just enough precision to perform relevant comparisons
            double requiredPrecision = FastMath.max(
                    gradTol,
                    FastMath.abs(alpha*stepScaleFactor*dfdx)
            ) / 4;
            // Warning: this overwrites grad and hess
            P.computeAll(newX, requiredPrecision);

            // do not look for damped steps if we are near stationary point
            if (dfdx*dfdx > gradTol2) {
                while (true) {
                    double f1 = P.getValue();
                    if (f1 <= PVal + alpha * stepScaleFactor * dfdx) {
                        break;
                    } else {
                        stepScaleFactor *= beta;
                    }
                    for (int i = 0; i < k; i++) {
                        newX[i] = x[i] + stepScaleFactor * stepVector.getEntry(i);
                    }
                    requiredPrecision = FastMath.max(
                            gradTol / 4,
                            FastMath.abs(alpha*stepScaleFactor*dfdx/2)
                    );
                    P.computeAll(newX, requiredPrecision);
                }
            }
            if (stepScaleFactor < 1.0) {
                dampedStepCount++;
            }
            if (verbose) {
                System.out.println("dfdx: "+dfdx);
                System.out.println("Step Size: "+stepScaleFactor);
            }
            System.arraycopy(newX, 0, x, 0, k);
        }
        stepCount = step;
        return x;

    }
}
