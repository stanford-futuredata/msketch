package msolver.thresholds;

import msolver.ChebyshevMomentSolver2;
import msolver.struct.MomentStruct;

public class ThresholdCascade {
    private MomentStruct ms;
    private MomentThreshold[] cascade;
    private ChebyshevMomentSolver2 solver;

    public ThresholdCascade(MomentStruct ms) {
        this.ms = ms;
        this.cascade = new MomentThreshold[2];
        this.cascade[0] = new MarkovThreshold(ms);
        this.cascade[1] = new RTTThreshold(ms);
    }

    // Are there phi fraction above x, aka is CDF(x) < 1 - phi?
    public boolean threshold(double x, double phi) {
        int ka = ms.powerSums.length;
        if (ka > 0) {
            if (ms.min == ms.max) {
                return x > ms.min;
            }
        } else {
            if (ms.logMin == ms.logMax) {
                return x > Math.exp(ms.logMin);
            }
        }

        if (x < ms.min) {
            return true;
        }
        if (x > ms.max) {
            return false;
        }

        for (int i = 0; i < cascade.length; i++) {
            MomentThreshold mt = cascade[i];
            double[] bounds = mt.bound(x);
            if (bounds[0] > phi) {
                return true;
            }
            if (bounds[1] < phi) {
                return false;
            }
        }

        solver = ChebyshevMomentSolver2.fromPowerSums(
                ms.min, ms.max,
                ms.powerSums,
                ms.logMin, ms.logMax,
                ms.logSums
        );
        solver.solve(1e-9);
        double cdfValue = solver.estimateCDF(x);
        if (cdfValue < 1 - phi) {
            return true;
        } else {
            return false;
        }
    }

    public ChebyshevMomentSolver2 getSolver() {
        return solver;
    }
}
