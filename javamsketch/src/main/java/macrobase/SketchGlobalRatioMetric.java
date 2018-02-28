package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric.Action;
import sketches.YahooSketch;

import java.util.Collections;

/**
 * Measures the relative outlier rate w.r.t. the global outlier rate
 */
public class SketchGlobalRatioMetric implements SketchQualityMetric {
    private double quantile;  // eg, 0.99
    public double cutoff;
    private double globalCount;
    private double tolerance = 1e-10;
    private boolean useCascade = false;
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

    public SketchGlobalRatioMetric(double quantile, double tolerance, boolean useCascade) {
        this.quantile = quantile;
        this.tolerance = tolerance;
        this.useCascade = useCascade;
    }

    public String name() {
        return "sketch_support";
    }

    public SketchGlobalRatioMetric initialize(YahooSketch[] globalAggregates) throws Exception {
        YahooSketch globalSketch = globalAggregates[0];
        globalCount = globalSketch.getCount() * (1.0 - quantile);
        cutoff = globalSketch.getQuantiles(Collections.singletonList(quantile))[0];
        return this;
    }

    public double value(YahooSketch[] aggregates) {
        YahooSketch sketch = aggregates[0];
        return (1.0 - sketch.getCDF(cutoff)) / (1.0 - quantile);
    }

    public Action getAction(YahooSketch[] aggregates, double threshold) {
        Action action;
        numEnterAction++;

        long start = System.nanoTime();
        YahooSketch sketch = aggregates[0];
        double outlierRateNeeded = threshold * (1.0 - quantile);
        double outlierRateEstimate = (1.0 - sketch.getCDF(cutoff));
        action = (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : Action.PRUNE;
        actionTime += System.nanoTime() - start;

        return action;
    }

    public boolean isMonotonic() {
        return false;
    }

    public void setCascadeStages(boolean[] useStages) { this.useStages = useStages; }

    public void setVerbose(boolean verbose) { this.verbose = verbose; }
}