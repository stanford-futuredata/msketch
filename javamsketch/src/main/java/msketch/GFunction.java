package msketch;

import msketch.chebyshev.ChebyshevPolynomial;
import org.apache.commons.math3.analysis.UnivariateFunction;

class GFunction implements UnivariateFunction {
    private boolean useStandardBasis;
    private double aCenter, aScale, bCenter, bScale;
    private ChebyshevPolynomial cBasis;

    public GFunction(
            int k, boolean useStandardBasis,
            double aMin, double aMax, double bMin, double bMax
    ) {
        this.cBasis = ChebyshevPolynomial.basis(k);
        this.useStandardBasis = useStandardBasis;
        aCenter = (aMin + aMax) / 2;
        aScale = (aMax - aMin) / 2;
        bCenter = (bMin + bMax) / 2;
        bScale = (bMax - bMin) / 2;
    }

    @Override
    public double value(double y) {
        double x = y * aScale + aCenter;
        double gX;
        if (useStandardBasis) {
            gX = Math.log(x);
        } else {
            gX = Math.exp(x);
        }
        double scaledBGX = (gX - bCenter) / bScale;
        return cBasis.value(scaledBGX);
    }
}
