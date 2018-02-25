package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import sketches.CMomentSketch;

import java.util.Arrays;
import java.util.Collections;

public class ThresholdAlerter extends CascadeObject {
    private double quantile;  // eg, 0.99
    private double threshold;
    private boolean useCascade = true;
    private boolean[] useStages;
    private boolean verbose;

    public ThresholdAlerter(double quantile, double threshold, boolean useCascade) {
        this.quantile = quantile;
        this.threshold = threshold;
        this.useCascade = useCascade;
    }

    public boolean checkAlert(CMomentSketch sketch) {
        boolean doAlert;
        long start = System.nanoTime();
        if (useCascade) {
            doAlert = checkAlertCascade(sketch);
        } else {
            numEnterAction++;
            doAlert = checkAlertMaxent(sketch);
        }
        actionTime += System.nanoTime() - start;
        return doAlert;
    }

    private boolean checkAlertCascade(CMomentSketch sketch) {
        numEnterCascade++;
        double outlierRateNeeded = 1.0 - quantile;

        if (useStages[0]) {
            // Simple checks on min and max
            if (sketch.getMax() < threshold) {
                return false;
            }
            if (sketch.getMin() >= threshold) {
                return true;
            }
        }
        numAfterNaiveCheck++;

        if (useStages[1]) {
            // Markov bounds
            long markovStart = System.nanoTime();
            QualityMetric.Action action = MarkovBound.isPastThreshold(outlierRateNeeded, threshold, sketch);
            if (action != null) {
                markovBoundTime += System.nanoTime() - markovStart;
                if (action == QualityMetric.Action.PRUNE) {
                    return false;
                } else {
                    return true;
                }
            }
            markovBoundTime += System.nanoTime() - markovStart;
        }
        numAfterMarkovBound++;

        if (useStages[2]) {
            // Moments-based bounds
            long momentStart = System.nanoTime();
            double[] bounds = sketch.boundGreaterThanThreshold(threshold);
            if (bounds[1] < outlierRateNeeded) {
                momentBoundTime += System.nanoTime() - momentStart;
                return false;
            }
            if (bounds[0] >= outlierRateNeeded) {
                momentBoundTime += System.nanoTime() - momentStart;
                return true;
            }
            momentBoundTime += System.nanoTime() - momentStart;
        }
        numAfterMomentBound++;

        // Maxent estimate
        long maxentStart = System.nanoTime();
        double outlierRateEstimate = sketch.estimateGreaterThanThreshold(threshold);
        long end = System.nanoTime();
        maxentTime += end - maxentStart;
        if (verbose && end - maxentStart > 0.010 * 1e9) {
            System.out.format("%d: %f\n", numAfterMomentBound, (end - maxentStart) / 1.e9);
        }
        return outlierRateEstimate >= outlierRateNeeded;
    }

    private boolean checkAlertMaxent(CMomentSketch sketch) {
        double outlierRateNeeded = 1.0 - quantile;
        double outlierRateEstimate = sketch.estimateGreaterThanThreshold(threshold);
        return outlierRateEstimate >= outlierRateNeeded;
    }

    public void setCascadeStages(boolean[] useStages) { this.useStages = useStages; }

    public void setVerbose(boolean verbose) { this.verbose = verbose; }
}
