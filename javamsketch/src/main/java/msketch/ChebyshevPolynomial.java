package msketch;

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
        int numEvals = 0;
        double[] oldFVals = null;
        double oldError = Double.MAX_VALUE;
        while(true) {
            double[] fvals = new double[N+1];
            if (oldFVals == null) {
                for (int i = 0; i <= N; i++) {
                    fvals[i] = f.value(FastMath.cos(Math.PI * i / N));
                }
                numEvals += (N+1);
            } else {
                for (int i = 0; i <= N; i++) {
                    if (i % 2 == 1) {
                        fvals[i] = f.value(FastMath.cos(Math.PI * i / N));
                    } else {
                        fvals[i] = oldFVals[i/2];
                    }
                }
                numEvals += N/2;
            }
            oldFVals = fvals;
            FastCosineTransformer t = new FastCosineTransformer(
                    DctNormalization.STANDARD_DCT_I
            );
            cs = t.transform(fvals, TransformType.FORWARD);
            for (int i = 0; i <= N; i++) {
                cs[i] *= 2.0/N;
            }

            double error = 0.0;
            double e1 = FastMath.abs(cs[cs.length-1]);
            if (e1 > error) {error = e1;}
            double e2 = 2*FastMath.abs(cs[cs.length-3]);
            if (e2 > error) {error = e2;}
            double e3 = 2*FastMath.abs(cs[cs.length-5]);
            if (e3 > error) {error = e3;}

            // HACK: just stop trying if the error gets worse and hope
            // newton damping will save us
            if (error < tol || error > oldError) {
                break;
            } else {
                N *= 2;
                oldError = error;
            }
        }
        cs[0] /= 2;
        ChebyshevPolynomial result = new ChebyshevPolynomial(cs);
        result.numFitEvals = numEvals;
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
