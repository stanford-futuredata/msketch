package msketch;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

/**
 * Choose how many powers / log powers to use when solving for
 * maximum entropy.
 */
public class SolveBasisSelector {
    private double maxConditionNumber = 10000;
    private double tol = 1e-9;

    private int ka, kb;

    public SolveBasisSelector() {

    }

    public void select(
            boolean useStandardBasis, double[] aMoments, double[] bMoments,
            double aMin, double aMax, double bMin, double bMax
    ) {
        // only use moments that are < 1, others are the product of numeric issues
        int maxKa = aMoments.length;
        for (int i = 0; i < aMoments.length; i++) {
            if (Math.abs(aMoments[i]) > 1.1) {
                maxKa = i;
                break;
            }
        }
        int maxKb = bMoments.length;
        for (int i = 0; i < bMoments.length; i++) {
            if (Math.abs(bMoments[i]) > 1.1) {
                maxKb = i;
                break;
            }

        }

        ka = maxKa;
        for (int nBMoments = 0; nBMoments < maxKb; nBMoments++) {
            kb = nBMoments+1;
            MaxEntFunction2 f = new MaxEntFunction2(
                    useStandardBasis,
                    new double[ka],
                    new double[kb],
                    aMin, aMax, bMin, bMax
            );
            double[][] hess = f.getHessian(tol);
            RealMatrix m = new Array2DRowRealMatrix(hess, false);
            double c = new SingularValueDecomposition(m).getConditionNumber();
//            System.out.println("ka: "+ka+" kb: "+kb+" c: "+c);
            if (c > maxConditionNumber || !Double.isFinite(c)) {
                kb = Math.max(1, kb-1);
                break;
            }
        }
    }

    public int getKa() {
        return ka;
    }
    public int getKb() {
        return kb;
    }
}
