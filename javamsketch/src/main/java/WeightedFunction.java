import org.apache.commons.math3.analysis.UnivariateFunction;

public class WeightedFunction implements UnivariateFunction{
    private int k;
    private UnivariateFunction f;

    public WeightedFunction(UnivariateFunction f, int k) {
        this.f = f;
        this.k = k;
    }

    public double chebyshev_monomial(double x) {
        if (k == 0) {
            return 1.0;
        } else if (k == 1) {
            return x;
        } else {
            double ts0 = 1.0;
            double ts1 = x;
            double tt1 = 0.0;
            for (int i = 2; i <= k; i++) {
                tt1 = ts1;
                ts1 = 2*x*ts1 - ts0;
                ts0 = tt1;
            }
            return ts1;
        }
    }

    @Override
    public double value(double x) {
        return chebyshev_monomial(x) * f.value(x);
    }
}
