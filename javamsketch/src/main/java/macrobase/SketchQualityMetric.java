package macrobase;

import com.yahoo.sketches.theta.Sketch;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import sketches.YahooSketch;

/**
 * Measures how interesting a subgroup is as a function of its linear aggregates.
 * Risk ratio, support, and deviation from mean are examples.
 */
public interface SketchQualityMetric {
    String name();
    SketchQualityMetric initialize(YahooSketch[] globalAggregates) throws Exception;
    double value(YahooSketch[] aggregates);
    boolean isMonotonic();

    // can override for more fancy tight quality metric bounds
    default double maxSubgroupValue(YahooSketch[] aggregates) {
        if (isMonotonic()) {
            return value(aggregates);
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }

    default QualityMetric.Action getAction(YahooSketch[] aggregates, double threshold) {
        if (isPastThreshold(aggregates, threshold)) {
            return QualityMetric.Action.KEEP;
        } else if (canPassThreshold(aggregates, threshold)) {
            return QualityMetric.Action.NEXT;
        } else {
            return QualityMetric.Action.PRUNE;
        }
    }

    default boolean isPastThreshold(YahooSketch[] aggregates, double threshold) {
        return value(aggregates) >= threshold;
    }

    default boolean canPassThreshold(YahooSketch[] aggregates, double threshold) {
        return maxSubgroupValue(aggregates) >= threshold;
    }
}
