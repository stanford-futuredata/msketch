package msketch;

import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.Arrays;

public class MaxEntFunction2 implements UnivariateFunction {
    private double[] aCoeffs;
    private double[] bCoeffs;
    private double aCenter, aScale, bCenter, bScale;
    private boolean isLog;

    private ChebyshevPolynomial aPoly;
    private ChebyshevPolynomial bPoly;

    private ChebyshevPolynomial[] bases;

    public MaxEntFunction2(
            boolean isLog,
            double[] aCoeffs,
            double[] bCoeffs,
            double aCenter,
            double aScale,
            double bCenter,
            double bScale
    ) {
        this.isLog = isLog;
        this.aCoeffs = aCoeffs;
        this.bCoeffs = bCoeffs;
        this.aCenter = aCenter;
        this.aScale = aScale;
        this.bCenter = bCenter;
        this.bScale = bScale;

        this.aPoly = new ChebyshevPolynomial(aCoeffs);
        this.bPoly = new ChebyshevPolynomial(bCoeffs);
        this.bases = new ChebyshevPolynomial[2*(aCoeffs.length+bCoeffs.length)];
        for (int i = 0; i < bases.length; i++) {
            bases[i] = ChebyshevPolynomial.basis(i);
        }
    }

    public double valueRaw(double x) {
        return value((x - aCenter) / aScale);
    }

    @Override
    public double value(double y) {
        double x = y*aScale+aCenter;
        double gX;
        if (isLog) {
            gX = Math.log(x);
        } else {
            gX = Math.exp(x);
        }
        double scaledBGX = (gX - bCenter) / bScale;
        double expValue = aPoly.value(y) + bPoly.value(scaledBGX);
        return Math.exp(expValue);
    }

    public double zerothMoment(double tol) {
        ChebyshevPolynomial pApprox = ChebyshevPolynomial.fit(this, tol);
        return pApprox.integrate();
    }

    private class WeightedFunction implements UnivariateFunction{
        private int i, j;
        private boolean iType, jType;
        private MaxEntFunction2 f2;
        public WeightedFunction(
                MaxEntFunction2 f2,
                int i,
                int j
        ) {
            this.f2 = f2;
            if (i < f2.aCoeffs.length) {
                this.i = i;
                this.iType = true;
            } else {
                this.i = i - f2.aCoeffs.length;
                this.iType = false;
            }
            if (j < f2.aCoeffs.length) {
                this.j = j;
                this.jType = true;
            } else {
                this.j = j - f2.aCoeffs.length;
                this.jType = false;
            }
        }

        @Override
        public double value(double y) {
            double x = y * aScale + aCenter;
            double gX;
            if (isLog) {
                gX = Math.log(x);
            } else {
                gX = Math.exp(x);
            }
            double scaledBGX = (gX - bCenter) / bScale;
            double wi, wj;
            if (iType) {
                wi = bases[i].value(y);
            } else {
                wi = bases[i].value(scaledBGX);
            }
            if (jType) {
                wj = bases[j].value(y);
            } else {
                wj = bases[j].value(scaledBGX);
            }
            return wi*wj*f2.value(y);
        }
    }

    private class WeightedBFunction implements UnivariateFunction {
        int i;
        private MaxEntFunction2 f2;
        public WeightedBFunction(
                int i,
                MaxEntFunction2 f2
        ) {
            this.i = i;
            this.f2 = f2;
        }

        @Override
        public double value(double y) {
            double x = y * aScale + aCenter;
            double gX;
            if (isLog) {
                gX = Math.log(x);
            } else {
                gX = Math.exp(x);
            }
            double scaledBGX = (gX - bCenter) / bScale;
            return bases[i].value(scaledBGX)*f2.value(y);
        }
    }

    public double[][] getPairwiseMoments(double tol) {
        ChebyshevPolynomial[] bApproxs = new ChebyshevPolynomial[2*bCoeffs.length];
        bApproxs[0] = ChebyshevPolynomial.fit(this, tol);
        for (int i = 1; i < 2*bCoeffs.length; i++) {
            UnivariateFunction weightedBFunction = new WeightedBFunction(
                    i,
                    this
            );
            bApproxs[i] = ChebyshevPolynomial.fit(weightedBFunction, tol);
        }


        int k = aCoeffs.length + bCoeffs.length;
        double[][] pairwiseMoments = new double[k][k];
        for (int j=0; j<aCoeffs.length; j++) {
            for (int i=j; i<aCoeffs.length; i++) {
                pairwiseMoments[i][j] = (
                        bApproxs[0].multiplyByBasis(i+j).integrate()
                        + bApproxs[0].multiplyByBasis(i-j).integrate()
                ) / 2;
            }
        }
        for (int j=0; j<bCoeffs.length; j++) {
            for (int i=j; i<bCoeffs.length; i++) {
                pairwiseMoments[i+aCoeffs.length][j+aCoeffs.length] = (
                        bApproxs[i+j].integrate() + bApproxs[i-j].integrate()
                        ) / 2;
            }
        }
        for (int j=0; j<aCoeffs.length; j++) {
            for (int i=0; i<bCoeffs.length; i++) {
                pairwiseMoments[i+aCoeffs.length][j] = (
                        bApproxs[i].multiplyByBasis(j).integrate()
                );
            }
        }
        for (int j=0; j < k; j++) {
            for (int i = 0; i < j; i++) {
                pairwiseMoments[i][j] = pairwiseMoments[j][i];
            }
        }
        return pairwiseMoments;
    }
}
