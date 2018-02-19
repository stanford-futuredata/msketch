package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanationResult;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.apriori.APrioriSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.apriori.IntSet;
import edu.stanford.futuredata.macrobase.util.MacrobaseInternalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Class for handling the generic, algorithmic aspects of apriori explanation.
 * This class assumes that subgroups posses "aggregates" such as count and outlier_count
 * which can be combined additively. Then, we use APriori to find the subgroups which
 * are the most interesting as defined by "quality metrics" on these aggregates.
 */
public class APrioriLinear {
    Logger log = LoggerFactory.getLogger("APLSummarizer");

    // **Parameters**
    private QualityMetric[] qualityMetrics;
    private double[] thresholds;
    private boolean doContainment = true;

    public long mergeTime = 0;
    public long queryTime = 0;
    private long start;

    // Singleton viable sets for quick lookup
    private HashSet<Integer> singleNext;
    // Sets that has high enough support but not high risk ratio, need to be explored
    private HashMap<Integer, HashSet<IntSet>> setNext;
    // Aggregate values for all of the sets we saved
    private HashMap<Integer, HashMap<IntSet, double[]>> savedAggregates;

    public APrioriLinear(
            List<QualityMetric> qualityMetrics,
            List<Double> thresholds
    ) {
        this.qualityMetrics = qualityMetrics.toArray(new QualityMetric[0]);
        this.thresholds = new double[thresholds.size()];
        for (int i = 0; i < thresholds.size(); i++) {
            this.thresholds[i] = thresholds.get(i);
        }
        this.setNext = new HashMap<>(3);
        this.savedAggregates = new HashMap<>(3);
    }

    public List<APLExplanationResult> explain(
            final List<int[]> attributes,
            double[][] aggregateColumns
    ) {
        return explain(attributes, aggregateColumns, null);
    }

    public List<APLExplanationResult> explain(
            final List<int[]> attributes,
            double[][] aggregateColumns,
            Map<String, int[]> aggregationOps
    ) {
        final int numAggregates = aggregateColumns.length;
        final int numRows = aggregateColumns[0].length;

        // Quality metrics are initialized with global aggregates to
        // allow them to determine the appropriate relative thresholds
        double[] globalAggregates = new double[numAggregates];
        start = System.nanoTime();
        if (aggregationOps == null) {
            for (int j = 0; j < numAggregates; j++) {
                globalAggregates[j] = 0;
                double[] curColumn = aggregateColumns[j];
                for (int i = 0; i < numRows; i++) {
                    globalAggregates[j] += curColumn[i];
                }
            }
        } else {
            for (int j : aggregationOps.getOrDefault("add", new int[0])) {
                globalAggregates[j] = 0;
                double[] curColumn = aggregateColumns[j];
                for (int i = 0; i < numRows; i++) {
                    globalAggregates[j] += curColumn[i];
                }
            }
            for (int j : aggregationOps.getOrDefault("min", new int[0])) {
                double[] curColumn = aggregateColumns[j];
                globalAggregates[j] = curColumn[0];
                for (int i = 0; i < numRows; i++) {
                    globalAggregates[j] = Math.min(globalAggregates[j], curColumn[i]);
                }
            }
            for (int j : aggregationOps.getOrDefault("max", new int[0])) {
                double[] curColumn = aggregateColumns[j];
                globalAggregates[j] = curColumn[0];
                for (int i = 0; i < numRows; i++) {
                    globalAggregates[j] = Math.max(globalAggregates[j], curColumn[i]);
                }
            }
        }
        mergeTime += System.nanoTime() - start;
        start = System.nanoTime();
        for (QualityMetric q : qualityMetrics) {
            q.initialize(globalAggregates);
        }
        queryTime += System.nanoTime() - start;

        // Row store for more convenient access
        final double[][] aRows = new double[numRows][numAggregates];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numAggregates; j++) {
                aRows[i][j] = aggregateColumns[j][i];
            }
        }

        start = System.nanoTime();
        HashMap<Integer, double[]> aggregates = new HashMap<>();
        for (int i = 0; i < numRows; i++) {
            int[] curRowAttributes = attributes.get(i);
            for (int c = 0; c < curRowAttributes.length; c++) {
                int curCandidate = curRowAttributes[c];
                double[] candidateVal = aggregates.get(curCandidate);
                if (candidateVal == null) {
                    aggregates.put(curCandidate, Arrays.copyOf(aRows[i], numAggregates));
                } else if (aggregationOps == null) {
                    for (int a = 0; a < numAggregates; a++) {
                        candidateVal[a] += aRows[i][a];
                    }
                } else {
                    for (int a : aggregationOps.getOrDefault("add", new int[0])) {
                        candidateVal[a] += aRows[i][a];
                    }
                    for (int a : aggregationOps.getOrDefault("min", new int[0])) {
                        candidateVal[a] = Math.min(candidateVal[a], aRows[i][a]);
                    }
                    for (int a : aggregationOps.getOrDefault("max", new int[0])) {
                        candidateVal[a] = Math.max(candidateVal[a], aRows[i][a]);
                    }
                }
            }
        }

        mergeTime += System.nanoTime() - start;

        HashSet<Integer> curOrderSaved = new HashSet<>();
        int pruned = 0;
        for (int curCandidate: aggregates.keySet()) {
            double[] curAggregates = aggregates.get(curCandidate);
            QualityMetric.Action action = QualityMetric.Action.KEEP;
            start = System.nanoTime();
            for (int i = 0; i < qualityMetrics.length; i++) {
                QualityMetric q = qualityMetrics[i];
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

        HashMap<Integer, double[]> curSavedAggregates = new HashMap<>(curOrderSaved.size());
        for (int curSaved : curOrderSaved) {
            curSavedAggregates.put(curSaved, aggregates.get(curSaved));
        }

        List<APLExplanationResult> results = new ArrayList<>();
        for (int curSet : curSavedAggregates.keySet()) {
            double[] aggs = curSavedAggregates.get(curSet);
            double[] metrics = new double[qualityMetrics.length];
            for (int i = 0; i < metrics.length; i++) {
                metrics[i] = qualityMetrics[i].value(aggs);
            }
            results.add(
                    new APLExplanationResult(qualityMetrics, new IntSet(curSet), aggs, metrics)
            );
        }
        return results;
    }

    private ArrayList<IntSet> getCandidates(
            int[] set
    ) {
        ArrayList<IntSet> candidates = new ArrayList<>();
        for (int i : set) {
            candidates.add(new IntSet(i));
        }
        return candidates;
    }

    public void setDoContainment(boolean doContainment) { this.doContainment = doContainment; }
}
