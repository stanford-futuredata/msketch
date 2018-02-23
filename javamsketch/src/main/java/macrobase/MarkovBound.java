package macrobase;

import msketch.MathUtil;

public class MarkovBound {
    public static double[] getOutlierRateBounds(double cutoff, double min, double max, double logMin, double logMax,
                                                double[] powerSums, double[] logSums) {
        return getOutlierRateBounds(cutoff, min,max, logMin, logMax, powerSums, logSums, true);
    }

    public static double[] getOutlierRateBounds(double cutoff, double min, double max, double logMin, double logMax,
                                                double[] powerSums, double[] logSums, boolean useLogs) {
        double[] outlierRateBounds = new double[2];
        outlierRateBounds[0] = 0.0;
        outlierRateBounds[1] = 1.0;

        double[] xMinusMinMoments = MathUtil.shiftPowerSum(powerSums, 1, min);
        double[] maxMinusXMoments = MathUtil.shiftPowerSum(powerSums, -1, max);
        for (int i = 1; i < powerSums.length; i++) {
            double outlierRateUpperBound = (xMinusMinMoments[i] / powerSums[0]) / Math.pow(cutoff - min, i);
            double outlierRateLowerBound = 1.0 - (maxMinusXMoments[i] / powerSums[0]) / Math.pow(max - cutoff, i);
            outlierRateBounds[0] = Math.max(outlierRateBounds[0], outlierRateLowerBound);
            outlierRateBounds[1] = Math.min(outlierRateBounds[1], outlierRateUpperBound);
        }

        if (useLogs && logSums[0] == 0) {
            return outlierRateBounds;
        }

        double logCutoff = Math.log(cutoff);
        double fracIncluded = logSums[0] / powerSums[0];
        double[] xMinusMinLogMoments = MathUtil.shiftPowerSum(logSums, 1, logMin);
        double[] maxMinusXLogMoments = MathUtil.shiftPowerSum(logSums, -1, logMax);
        for (int i = 1; i < logSums.length; i++) {
            double outlierRateUpperBound = (1.0 - fracIncluded) + fracIncluded * (xMinusMinLogMoments[i] / logSums[0]) / Math.pow(logCutoff - logMin, i);
            double outlierRateLowerBound = 1.0 - fracIncluded * (maxMinusXLogMoments[i] / logSums[0]) / Math.pow(logMax - logCutoff, i);
            outlierRateBounds[0] = Math.max(outlierRateBounds[0], outlierRateLowerBound);
            outlierRateBounds[1] = Math.min(outlierRateBounds[1], outlierRateUpperBound);
        }

        return outlierRateBounds;
    }
}
