package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import msketch.MathUtil;
import sketches.CMomentSketch;

import java.util.Arrays;
import java.util.Collections;

/**
 * Measures the relative outlier rate w.r.t. the global outlier rate
 */
public class EstimatedGlobalRatioMetric extends CascadeQualityMetric implements QualityMetric {
    private int minIdx = 0;
    private int maxIdx = 1;
    private int momentsBaseIdx = 2;
    private int logMomentsBaseIdx = 2 + 6;
    private double quantile;  // eg, 0.99
    private double cutoff;
    private double globalCount;
    private double tolerance = 1e-10;
    private boolean useCascade = true;
    private boolean[] useStages;
    private boolean verbose;

    // Statistics
//    public int numEnterCascade = 0;
//    public int numAfterNaiveCheck = 0;
//    public int numAfterMarkovBound = 0;
//    public int numAfterMomentBound = 0;
//    public int numEnterAction = 0;
//    public long actionTime = 0;
//    public long markovBoundTime = 0;
//    public long momentBoundTime = 0;
//    public long maxentTime = 0;

    public EstimatedGlobalRatioMetric(int minIdx, int maxIdx, int momentsBaseIdx,
                                      int logMomentsBaseIdx, double quantile, double tolerance, boolean useCascade) {
        this.minIdx = minIdx;
        this.maxIdx = maxIdx;
        this.momentsBaseIdx = momentsBaseIdx;
        this.logMomentsBaseIdx = logMomentsBaseIdx;  // points to the first log moment (not zeroth)
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
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        double[] powerSums = Arrays.copyOfRange(aggregates, momentsBaseIdx, logMomentsBaseIdx);
        double[] logSums = new double[aggregates.length - logMomentsBaseIdx + 1];
        logSums[0] = aggregates[momentsBaseIdx];
        System.arraycopy(aggregates, logMomentsBaseIdx, logSums, 1, aggregates.length - logMomentsBaseIdx);
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
        return ms.estimateGreaterThanThreshold(cutoff) / (1.0 - quantile);
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
        double outlierRateNeeded = threshold * (1.0 - quantile);

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
            double logMin = Math.log(min);
            double logMax = Math.log(max);
            double[] powerSums = Arrays.copyOfRange(aggregates, momentsBaseIdx, logMomentsBaseIdx);
            double[] logSums = new double[aggregates.length - logMomentsBaseIdx + 1];
            logSums[0] = aggregates[momentsBaseIdx];
            System.arraycopy(aggregates, logMomentsBaseIdx, logSums, 1, aggregates.length - logMomentsBaseIdx);
            double[] outlierRateBounds = MarkovBound.getOutlierRateBounds(cutoff, min, max, logMin, logMax, powerSums, logSums);

            if (outlierRateBounds[1] < outlierRateNeeded) {
                markovBoundTime += System.nanoTime() - markovStart;
                return Action.PRUNE;
            }
            if (outlierRateBounds[0] >= outlierRateNeeded) {
                markovBoundTime += System.nanoTime() - markovStart;
                return Action.KEEP;
            }

            markovBoundTime += System.nanoTime() - markovStart;
        }
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
        double outlierRateNeeded = threshold * (1.0 - quantile);
        CMomentSketch ms = sketchFromAggregates(aggregates);
        double outlierRateEstimate = ms.estimateGreaterThanThreshold(cutoff);
        return (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : Action.PRUNE;
    }

    @Override
    public boolean isMonotonic() {
        return false;
    }

    public void setCascadeStages(boolean[] useStages) { this.useStages = useStages; }

    public void setVerbose(boolean verbose) { this.verbose = verbose; }
}
