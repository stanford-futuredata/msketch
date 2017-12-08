import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;

public class ExpPolyFunction implements UnivariateFunction{
    private double[] coeffs;
    private int funcEvals;

    private int max_iter = 100;

    public int getFuncEvals() {
        return funcEvals;
    }
    public double[] getCoeffs() {
        return coeffs;
    }

    public ExpPolyFunction(double[] coeffs) {
        this.coeffs = coeffs;
        this.funcEvals = 0;
    }

    public double chebyshev_poly(double x) {
        int k = coeffs.length;
        double sum = 0.0;
        double ts0 = 1.0;
        double ts1 = x;

        sum += coeffs[0];
        if (k > 0) {
            sum += coeffs[1] * x;
            for (int i = 2; i < k; i++) {
                double tt1 = ts1;
                ts1 = 2*x*ts1 - ts0;
                ts0 = tt1;
                sum += coeffs[i] * ts1;
            }
        }

        return sum;
    }

    @Override
    public double value(double v) {
        return FastMath.exp(-chebyshev_poly(v));
    }

    public double[] moments(int mu_k, double int_tol) {
        UnivariateIntegrator r = new RombergIntegrator(
                int_tol,
                int_tol,
                3,
                32
        );
        double[] out_moments = new double[mu_k];
        for (int i = 0; i < mu_k; i++) {
            UnivariateFunction f = new WeightedFunction(this, i);
            out_moments[i] = r.integrate(100000, f, -1.0, 1.0);
            funcEvals += r.getEvaluations();
        }
        return out_moments;
    }

    public int solve(double[] d_mus, double m_tol) {
        int k = coeffs.length;
        for (int step = 0; step < max_iter; step++) {
            double[] e_mus = moments(2*k, m_tol);
            double[] dl = new double[k];

            boolean withinTolerance = true;
            for (int i = 0; i < k; i++) {
                double momentDelta = d_mus[i] - e_mus[i];
                dl[i] = momentDelta;
                if (FastMath.abs(momentDelta) > m_tol) {
                    withinTolerance = false;
                }
            }

            if (withinTolerance) {
                return step;
            }

            double[][] hh = new double[k][k];
            for (int i=0; i < k; i++) {
                for (int j=0; j < k; j++) {
                    double val = .5*(e_mus[i+j] + e_mus[FastMath.abs(i-j)]);
                    hh[i][j] = val;
                }
            }

            RealMatrix hhMat = new Array2DRowRealMatrix(hh);
            CholeskyDecomposition d = new CholeskyDecomposition(hhMat);
            RealVector stepVector = d.getSolver().solve(new ArrayRealVector(dl));

            for (int i = 0; i < k; i++) {
                coeffs[i] -= stepVector.getEntry(i);
            }
        }

        return -max_iter;
    }
}
