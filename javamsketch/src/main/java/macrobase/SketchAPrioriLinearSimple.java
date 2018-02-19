package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.apriori.APrioriSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.apriori.IntSet;
import edu.stanford.futuredata.macrobase.util.MacrobaseInternalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sketches.YahooSketch;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Class for handling the generic, algorithmic aspects of apriori explanation.
 * This class assumes that subgroups posses "aggregates" such as count and outlier_count
 * which can be combined additively. Then, we use APriori to find the subgroups which
 * are the most interesting as defined by "quality metrics" on these aggregates.
 */
public class SketchAPrioriLinearSimple {
    Logger log = LoggerFactory.getLogger("APLSummarizer");

    // **Parameters**
    private SketchSupportMetric[] qualityMetrics;
    private double[] thresholds;
    private boolean doContainment = true;

    public long mergeTime = 0;
    public long queryTime = 0;
    private long start;


    public SketchAPrioriLinearSimple(
            List<SketchSupportMetric> qualityMetrics,
            List<Double> thresholds
    ) {
        this.qualityMetrics = qualityMetrics.toArray(new SketchSupportMetric[0]);
        this.thresholds = new double[thresholds.size()];
        for (int i = 0; i < thresholds.size(); i++) {
            this.thresholds[i] = thresholds.get(i);
        }
    }

    public List<APLSketchExplanationResult> explain(
            final List<int[]> attributes,
            YahooSketch[][] aggregateColumns
    ) throws Exception {
        final int numAggregates = aggregateColumns.length;
        final int numRows = aggregateColumns[0].length;
        final double sizeParam = aggregateColumns[0][0].sketch.getK();

        // Quality metrics are initialized with global aggregates to
        // allow them to determine the appropriate relative thresholds
        YahooSketch[] globalAggregates = new YahooSketch[numAggregates];
        start = System.nanoTime();
        for (int j = 0; j < numAggregates; j++) {
            YahooSketch globalSketch = new YahooSketch();
            globalSketch.setSizeParam(sizeParam);
            globalSketch.initialize();
            YahooSketch[] curColumn = aggregateColumns[j];
            ArrayList<YahooSketch> curSketches = new ArrayList<>(Arrays.asList(curColumn));
            globalSketch.mergeYahoo(curSketches);
            globalAggregates[j] = globalSketch;
        }
        mergeTime += System.nanoTime() - start;
        start = System.nanoTime();
        for (SketchSupportMetric q : qualityMetrics) {
            q.initialize(globalAggregates);
        }
        queryTime += System.nanoTime() - start;

        // Row store for more convenient access
        final YahooSketch[][] aRows = new YahooSketch[numRows][numAggregates];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numAggregates; j++) {
                aRows[i][j] = aggregateColumns[j][i];
            }
        }

        start = System.nanoTime();
        HashMap<Integer, YahooSketch[]> setAggregates = new HashMap<>();

        for (int i = 0; i < numRows; i++) {
            int[] curRowAttributes = attributes.get(i);
            for (int c = 0; c < curRowAttributes.length; c++) {
                int curCandidate = curRowAttributes[c];
                YahooSketch[] candidateVal = setAggregates.get(curCandidate);
                if (candidateVal == null) {
                    YahooSketch[] aggregates = new YahooSketch[numAggregates];
                    for (int a = 0; a < numAggregates; a++) {
                        YahooSketch agg = new YahooSketch();
                        agg.setSizeParam(sizeParam);
                        agg.initialize();
                        aggregates[a] = agg.mergeYahoo(aRows[i][0]);
                    }
                    setAggregates.put(curCandidate, aggregates);
                } else {
                    for (int a = 0; a < numAggregates; a++) {
                        candidateVal[a] = candidateVal[a].mergeYahoo(aRows[i][a]);
                    }
                }
            }
        }
        mergeTime += System.nanoTime() - start;

        HashSet<Integer> curOrderSaved = new HashSet<>();
        int pruned = 0;
        for (int curCandidate: setAggregates.keySet()) {
            YahooSketch[] curAggregates = setAggregates.get(curCandidate);
            QualityMetric.Action action = QualityMetric.Action.KEEP;
            start = System.nanoTime();
            for (int i = 0; i < qualityMetrics.length; i++) {
                SketchSupportMetric q = qualityMetrics[i];
                double t = thresholds[i];
                action = QualityMetric.Action.combine(action, q.getAction(curAggregates, t));
            }
            queryTime += System.nanoTime() - start;
            if (action == QualityMetric.Action.KEEP) {
                // if a set is already past the threshold on all metrics,
                // save it and no need for further exploration if we do containment
                curOrderSaved.add(curCandidate);
            } else {
                pruned++;
            }
        }

        HashMap<Integer, YahooSketch[]> curSavedAggregates = new HashMap<>(curOrderSaved.size());
        for (int curSaved : curOrderSaved) {
            curSavedAggregates.put(curSaved, setAggregates.get(curSaved));
        }

        List<APLSketchExplanationResult> results = new ArrayList<>();
        for (int curSet : curSavedAggregates.keySet()) {
            YahooSketch[] aggregates = curSavedAggregates.get(curSet);
            double[] metrics = new double[qualityMetrics.length];
            for (int i = 0; i < metrics.length; i++) {
                metrics[i] = qualityMetrics[i].value(aggregates);
            }
            results.add(
                    new APLSketchExplanationResult(qualityMetrics, new IntSet(curSet), aggregates, metrics)
            );
        }
        return results;
    }

    public void setDoContainment(boolean doContainment) { this.doContainment = doContainment; }
}
