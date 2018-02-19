package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import msketch.MathUtil;
import sketches.CMomentSketch;
import sketches.MomentSketch;

import java.util.Arrays;
import java.util.Collections;

/**
 * Measures the relative outlier rate w.r.t. the global outlier rate
 */
public class EstimatedSupportMetric implements QualityMetric {
    private int minIdx = 0;
    private int maxIdx = 1;
    private int logMinIdx = 2;
    private int logMaxIdx = 3;
    private int momentsBaseIdx = 4;
    private int logMomentsBaseIdx = 4 + 5;
    private double quantile;  // eg, 0.99
    private double cutoff;
    private double globalCount;
    private double tolerance = 1e-10;
    private boolean useCascade = true;
    private boolean[] useStages;
    private boolean verbose;

    // Statistics
    public int numEnterCascade = 0;
    public int numAfterNaiveCheck = 0;
    public int numAfterMarkovBound = 0;
    public int numAfterMomentBound = 0;
    public int numEnterAction = 0;
    public long actionTime = 0;
    public long markovBoundTime = 0;
    public long momentBoundTime = 0;
    public long maxentTime = 0;

    public EstimatedSupportMetric(int minIdx, int maxIdx, int logMinIdx, int logMaxIdx, int momentsBaseIdx,
                                  int logMomentsBaseIdx, double quantile, double tolerance, boolean useCascade) {
        this.minIdx = minIdx;
        this.maxIdx = maxIdx;
        this.logMinIdx = logMinIdx;
        this.logMaxIdx = logMaxIdx;
        this.momentsBaseIdx = momentsBaseIdx;
        this.logMomentsBaseIdx = logMomentsBaseIdx;
        this.quantile = quantile;
        this.tolerance = tolerance;
        this.useCascade = useCascade;
    }

    @Override
    public String name() {
        return "est_support";
    }

    private CMomentSketch sketchFromAggregates(double[] aggregates) {
        CMomentSketch ms = new CMomentSketch(tolerance);
        double min = aggregates[minIdx];
        double max = aggregates[maxIdx];
        double logMin = aggregates[logMinIdx];
        double logMax = aggregates[logMaxIdx];
        double[] powerSums = Arrays.copyOfRange(aggregates, momentsBaseIdx, logMomentsBaseIdx);
        double[] logSums = Arrays.copyOfRange(aggregates, logMomentsBaseIdx, aggregates.length);
        ms.setStats(min, max, logMin, logMax, powerSums, logSums);
        return ms;
    }

    @Override
    public QualityMetric initialize(double[] globalAggregates) {
        globalCount = globalAggregates[momentsBaseIdx] * (1.0 - quantile);
        CMomentSketch ms = sketchFromAggregates(globalAggregates);
        try {
            cutoff = ms.getQuantiles(Collections.singletonList(quantile))[0];
        } catch (Exception e) {
            cutoff = quantile * (globalAggregates[maxIdx] - globalAggregates[minIdx]) + globalAggregates[minIdx];
        }
        return this;
    }

    @Override
    public double value(double[] aggregates) {
        CMomentSketch ms = sketchFromAggregates(aggregates);
        return ms.estimateGreaterThanThreshold(cutoff) * aggregates[momentsBaseIdx] / globalCount;
    }

    @Override
    public Action getAction(double[] aggregates, double threshold) {
        Action action;
        long start = System.nanoTime();
        if (useCascade) {
            action = getActionCascade(aggregates, threshold);
        } else {
            numEnterAction++;
            action = getActionMaxent(aggregates, threshold);
        }
        actionTime += System.nanoTime() - start;
        return action;
    }

    private Action getActionCascade(double[] aggregates, double threshold) {
        numEnterCascade++;
        double outlierRateNeeded = threshold * globalCount / aggregates[momentsBaseIdx];

        if (useStages[0]) {
            // Simple checks on min and max
            if (aggregates[maxIdx] < cutoff || outlierRateNeeded > 1.0) {
                return Action.PRUNE;
            }
            if (aggregates[minIdx] >= cutoff && outlierRateNeeded <= 1.0) {
                return Action.KEEP;
            }
        }
        numAfterNaiveCheck++;

        if (useStages[1]) {
            // Markov bounds
            long markovStart = System.nanoTime();
            double min = aggregates[minIdx];
            double max = aggregates[maxIdx];
            double[] powerSums = Arrays.copyOfRange(aggregates, momentsBaseIdx, logMomentsBaseIdx);
            double[] xMinusMinMoments = MathUtil.shiftPowerSum(powerSums, 1, min);
            double[] maxMinusXMoments = MathUtil.shiftPowerSum(powerSums, -1, max);
            for (int i = 1; i < logMomentsBaseIdx - momentsBaseIdx; i++) {
                double cutoffLowerBound = Math.max(0.0, 1 - (xMinusMinMoments[i] / powerSums[0]) / Math.pow(cutoff - min, i));
                double cutoffUpperBound = Math.min(1.0, (maxMinusXMoments[i] / powerSums[0]) / Math.pow(max - cutoff, i));
                double outlierRateUpperBound = 1.0 - cutoffLowerBound;
                double outlierRateLowerBound = 1.0 - cutoffUpperBound;
                if (outlierRateUpperBound < outlierRateNeeded) {
                    markovBoundTime += System.nanoTime() - markovStart;
                    return Action.PRUNE;
                }
                if (outlierRateLowerBound >= outlierRateNeeded) {
                    markovBoundTime += System.nanoTime() - markovStart;
                    return Action.KEEP;
                }
            }
            markovBoundTime += System.nanoTime() - markovStart;
        }

        // TODO: can we do Markov bounds with log moments? According to Wikipedia, since log is not a
        // non-negative function over the non-negative reals, it cannot be used as an extension to Markov.
        numAfterMarkovBound++;

        CMomentSketch ms = sketchFromAggregates(aggregates);
        if (useStages[2]) {
            // Moments-based bounds
            long momentStart = System.nanoTime();
            double[] bounds = ms.boundGreaterThanThreshold(cutoff);
            if (bounds[1] < outlierRateNeeded) {
                momentBoundTime += System.nanoTime() - momentStart;
                return Action.PRUNE;
            }
            if (bounds[0] >= outlierRateNeeded) {
                momentBoundTime += System.nanoTime() - momentStart;
                return Action.KEEP;
            }
            momentBoundTime += System.nanoTime() - momentStart;
        }
        numAfterMomentBound++;

        // Maxent estimate
        long maxentStart = System.nanoTime();
        double outlierRateEstimate = ms.estimateGreaterThanThreshold(cutoff);
        long end = System.nanoTime();
        maxentTime += end - maxentStart;
        if (verbose && end - maxentStart > 0.010 * 1e9) {
            System.out.format("%d: %f\n", numAfterMomentBound, (end - maxentStart) / 1.e9);
            System.out.println(Arrays.toString(aggregates));
        }
        return (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : Action.PRUNE;
    }

    private Action getActionMaxent(double[] aggregates, double threshold) {
        double outlierRateNeeded = threshold * globalCount / aggregates[momentsBaseIdx];
        CMomentSketch ms = sketchFromAggregates(aggregates);
        double outlierRateEstimate = ms.estimateGreaterThanThreshold(cutoff);
        return (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : Action.PRUNE;
    }

    @Override
    public boolean isMonotonic() {
        return true;
    }

    public void setCascadeStages(boolean[] useStages) { this.useStages = useStages; }

    public void setVerbose(boolean verbose) { this.verbose = verbose; }
}
