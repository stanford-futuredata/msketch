import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.transform.DctNormalization;
import org.apache.commons.math3.transform.FastCosineTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;

public class ChebyshevPolynomial implements UnivariateFunction {
    private double[] coeffs;
    private int numFitEvals;

    public ChebyshevPolynomial(double[] coeff) {
        this.coeffs = coeff;
    }

    public double[] coeffs() {
        return coeffs;
    }

    public int size() {
        return coeffs.length;
    }

    public static ChebyshevPolynomial basis(int k) {
        double[] basisCoeffs = new double[k+1];
        basisCoeffs[k] = 1.0;
        return new ChebyshevPolynomial(basisCoeffs);
    }

    public static ChebyshevPolynomial fit(
            UnivariateFunction f,
            double tol
    ) {
        int N = 32;
        double[] cs;
        while(true) {
            double[] fvals = new double[N+1];
            for (int i = 0; i <= N; i++) {
                fvals[i] = f.value(FastMath.cos(Math.PI*i/N));
            }
            FastCosineTransformer t = new FastCosineTransformer(
                    DctNormalization.STANDARD_DCT_I
            );
            cs = t.transform(fvals, TransformType.FORWARD);
            for (int i = 0; i <= N; i++) {
                cs[i] *= 2.0/N;
            }

            if (cs[cs.length - 1] < tol) {
                break;
            } else {
                N *= 2;
            }
        }
        cs[0] /= 2;
        ChebyshevPolynomial result = new ChebyshevPolynomial(cs);
        result.numFitEvals = N;
        return result;
    }

    public ChebyshevPolynomial multiplyByBasis(int k) {
        double[] newCoeffs = new double[coeffs.length+k];
        for (int i = 0; i < coeffs.length; i++) {
            double c2 = coeffs[i] / 2;
            newCoeffs[i + k] += c2;
            if ( i < k) {
                newCoeffs[k - i] += c2;
            } else {
                newCoeffs[i - k] += c2;
            }
        }
        return new ChebyshevPolynomial(newCoeffs);
    }

    public double integrate() {
        double sum = 0.0;
        for (int i2 = 0; i2 < coeffs.length; i2+=2) {
            sum -= coeffs[i2]/((i2+1)*(i2-1));
        }
        return 2*sum;
    }

    public double value(double x) {
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
    public String toString() {
        return "CPoly: "+ Arrays.toString(coeffs);
    }

    public int getNumFitEvals() {
        return numFitEvals;
    }
}
