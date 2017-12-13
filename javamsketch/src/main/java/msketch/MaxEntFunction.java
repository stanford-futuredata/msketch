package msketch;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;

public class MaxEntFunction implements UnivariateFunction{
    private ChebyshevPolynomial p;
    private ChebyshevPolynomial p_approx;
    private int funcEvals;

    public int getFuncEvals() {
        return funcEvals;
    }
    public double[] coeffs() {
        return p.coeffs();
    }
    public int size() {
        return p.size();
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
}
