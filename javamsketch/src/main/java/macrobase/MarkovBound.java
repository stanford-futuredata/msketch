package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import msketch.MathUtil;
import sketches.CMomentSketch;

import java.util.Arrays;

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

    public static QualityMetric.Action isPastThreshold(double outlierRateNeeded, double cutoff, CMomentSketch sketch) {
        double[] totalSums = sketch.getTotalSums();
        double[] powerSums = Arrays.copyOfRange(totalSums, 0, (int)sketch.getSizeParam());
        double[] logSums = Arrays.copyOfRange(totalSums, (int)sketch.getSizeParam(), totalSums.length);

        return isPastThreshold(outlierRateNeeded, cutoff, sketch.getMin(), sketch.getMax(), sketch.getLogMin(),
                sketch.getLogMax(), powerSums, logSums, true);
    }

    public static QualityMetric.Action isPastThreshold(double outlierRateNeeded, double cutoff, double min, double max,
                                                double logMin, double logMax, double[] powerSums, double[] logSums) {
        return isPastThreshold(outlierRateNeeded, cutoff, min, max, logMin, logMax, powerSums, logSums, true);
    }

    public static QualityMetric.Action isPastThreshold(double outlierRateNeeded, double cutoff, double min, double max,
                             double logMin, double logMax, double[] powerSums, double[] logSums,
                             boolean useLogs) {
        double[] xMinusMinMoments = MathUtil.shiftPowerSum(powerSums, 1, min);
        double[] maxMinusXMoments = MathUtil.shiftPowerSum(powerSums, -1, max);
        for (int i = 1; i < powerSums.length; i++) {
            double outlierRateUpperBound = Math.min(1.0, (xMinusMinMoments[i] / powerSums[0]) / Math.pow(cutoff - min, i));
            double outlierRateLowerBound = Math.max(0.0, 1.0 - (maxMinusXMoments[i] / powerSums[0]) / Math.pow(max - cutoff, i));
            if (outlierRateUpperBound <= outlierRateNeeded) {
                return QualityMetric.Action.PRUNE;
            }
            if (outlierRateLowerBound >= outlierRateNeeded) {
                return QualityMetric.Action.KEEP;
            }
        }

        if (useLogs && logSums[0] == 0) {
            return null;
        }

        double logCutoff = Math.log(cutoff);
        double fracIncluded = logSums[0] / powerSums[0];
        double[] xMinusMinLogMoments = MathUtil.shiftPowerSum(logSums, 1, logMin);
        double[] maxMinusXLogMoments = MathUtil.shiftPowerSum(logSums, -1, logMax);
        for (int i = 1; i < logSums.length; i++) {
            double outlierRateUpperBound = Math.min(1.0, (1.0 - fracIncluded) + fracIncluded * (xMinusMinLogMoments[i] / logSums[0]) / Math.pow(logCutoff - logMin, i));
            double outlierRateLowerBound = Math.max(0.0, 1.0 - fracIncluded * (maxMinusXLogMoments[i] / logSums[0]) / Math.pow(logMax - logCutoff, i));
            if (outlierRateUpperBound <= outlierRateNeeded) {
                return QualityMetric.Action.PRUNE;
            }
            if (outlierRateLowerBound >= outlierRateNeeded) {
                return QualityMetric.Action.KEEP;
            }
        }

        return null;
    }
}
