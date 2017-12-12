import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;

public class MaxEntFunction implements UnivariateFunction{
    private ChebyshevPolynomial p;
    private ChebyshevPolynomial p_approx;
    private int funcEvals;

    private int max_iter = 100;

    public int getFuncEvals() {
        return funcEvals;
    }
    public double[] coeffs() {
        return p.coeffs();
    }

    public MaxEntFunction(double[] coeffs) {
        this.p = new ChebyshevPolynomial(coeffs);
        this.funcEvals = 0;
    }

    @Override
    public double value(double v) {
        return FastMath.exp(-p.value(v));
    }

    public double[] moments(int mu_k, double tol) {
        p_approx = ChebyshevPolynomial.fit(this, tol);
        funcEvals += p_approx.getNumFitEvals();
        double[] out_moments = new double[mu_k];
        for (int i = 0; i < mu_k; i++) {
            ChebyshevPolynomial p_times_moment = p_approx.multiplyByBasis(i);
            out_moments[i] = p_times_moment.integrate();
        }
        return out_moments;
    }

    public int solve(double[] d_mus, double m_tol) {
        int k = p.size();
        double[] dl = new double[k];
        double[][] hh = new double[k][k];

        for (int step = 0; step < max_iter; step++) {
            double[] e_mus = moments(2*k, m_tol);

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

            for (int i=0; i < k; i++) {
                for (int j=0; j <= i; j++) {
                    hh[i][j] = (e_mus[i+j] + e_mus[i-j])/2;
                }
            }
            for (int i=0; i < k; i++) {
                for (int j=i+1; j < k; j++) {
                    hh[i][j] = hh[j][i];
                }
            }

            RealMatrix hhMat = new Array2DRowRealMatrix(hh, false);
            CholeskyDecomposition d = new CholeskyDecomposition(
                    hhMat, 0, m_tol);
            RealVector stepVector = d.getSolver().solve(new ArrayRealVector(dl));

            double[] coeffs = p.coeffs();
            for (int i = 0; i < k; i++) {
                coeffs[i] -= stepVector.getEntry(i);
            }
        }
        return -max_iter;
    }
}
