package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;

/**
 * Measures how interesting a subgroup is as a function of its linear aggregates.
 * Risk ratio, support, and deviation from mean are examples.
 */
public abstract class CascadeQualityMetric {
    public int numEnterCascade = 0;
    public int numAfterNaiveCheck = 0;
    public int numAfterMarkovBound = 0;
    public int numAfterMomentBound = 0;
    public int numEnterAction = 0;
    public long actionTime = 0;
    public long markovBoundTime = 0;
    public long momentBoundTime = 0;
    public long maxentTime = 0;
}
