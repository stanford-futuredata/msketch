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

    public List<Integer> o1results;

    public enum AggregationOp {
        MIN, MAX, SUM;
    }

    public APrioriLinear(
            List<QualityMetric> qualityMetrics,
            List<Double> thresholds
    ) {
        this.qualityMetrics = qualityMetrics.toArray(new QualityMetric[0]);
        this.thresholds = new double[thresholds.size()];
        for (int i = 0; i < thresholds.size(); i++) {
            this.thresholds[i] = thresholds.get(i);
        }
    }

    public List<APLExplanationResult> explain(
            final List<int[]> attributes,
            double[][] aggregateColumns,
            int numSingletons
    ) {
        return explain(attributes, aggregateColumns, numSingletons, Collections.nCopies(aggregateColumns.length, AggregationOp.SUM));
    }

    public List<APLExplanationResult> explain(
            final List<int[]> attributes,
            double[][] aggregateColumns,
            int numSingletons,
            List<AggregationOp> aggregationOps
    ) {
        final int numAggregates = aggregateColumns.length;
        final int numRows = aggregateColumns[0].length;

        // Quality metrics are initialized with global aggregates to
        // allow them to determine the appropriate relative thresholds

        double[] initialAggregate = new double[numAggregates];
        for (int a = 0; a < numAggregates; a++) {
            switch (aggregationOps.get(a)) {
                case MIN:
                    initialAggregate[a] = Double.MAX_VALUE;
                    break;
                case MAX:
                    initialAggregate[a] = -Double.MAX_VALUE;
                    break;
                case SUM:
                    break;
            }
        }

        // TODO: for some reason globalAggregates[0] comes out to 0 when it should be positive
        double[] globalAggregates = Arrays.copyOf(initialAggregate, numAggregates);
        start = System.nanoTime();
        for (int j = 0; j < numAggregates; j++) {
            globalAggregates[j] = 0;
            double[] curColumn = aggregateColumns[j];
            switch (aggregationOps.get(j)) {
                case MIN:
                    for (int i = 0; i < numRows; i++) {
                        globalAggregates[j] = Math.min(globalAggregates[j], curColumn[i]);
                    }
                    break;
                case MAX:
                    for (int i = 0; i < numRows; i++) {
                        globalAggregates[j] = Math.max(globalAggregates[j], curColumn[i]);
                    }
                    break;
                case SUM:
                    for (int i = 0; i < numRows; i++) {
                        globalAggregates[j] += curColumn[i];
                    }
                    break;
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
        List<double[]> aggregates = new ArrayList<>();
        for (int i = 0; i < numSingletons; i++) {
            aggregates.add(Arrays.copyOf(initialAggregate, numAggregates));
        }
        for (int i = 0; i < numRows; i++) {
            int[] curRowAttributes = attributes.get(i);
            for (int c = 0; c < curRowAttributes.length; c++) {
                int curCandidate = curRowAttributes[c];
                double[] candidateVal = aggregates.get(curCandidate);
                for (int a = 0; a < numAggregates; a++) {
                    switch (aggregationOps.get(a)) {
                        case MIN:
                            candidateVal[a] = Math.min(candidateVal[a], aRows[i][a]);
                            break;
                        case MAX:
                            candidateVal[a] = Math.max(candidateVal[a], aRows[i][a]);
                            break;
                        case SUM:
                            candidateVal[a] += aRows[i][a];
                            break;
                    }
                }
            }
        }
        mergeTime += System.nanoTime() - start;

        HashSet<Integer> curOrderSaved = new HashSet<>();
        start = System.nanoTime();
        for (int curCandidate = 0; curCandidate < numSingletons; curCandidate++) {
            double[] curAggregates = aggregates.get(curCandidate);
            QualityMetric.Action action = QualityMetric.Action.KEEP;
            for (int i = 0; i < qualityMetrics.length; i++) {
                QualityMetric q = qualityMetrics[i];
                double t = thresholds[i];
                action = QualityMetric.Action.combine(action, q.getAction(curAggregates, t));
            }
            if (action == QualityMetric.Action.KEEP) {
                curOrderSaved.add(curCandidate);
            }
        }
        queryTime += System.nanoTime() - start;

        HashMap<Integer, double[]> curSavedAggregates = new HashMap<>(curOrderSaved.size());
        for (int curSaved : curOrderSaved) {
            curSavedAggregates.put(curSaved, aggregates.get(curSaved));
        }

        List<APLExplanationResult> results = new ArrayList<>();
        o1results = new ArrayList<>();
        for (int curSet : curSavedAggregates.keySet()) {
            double[] aggs = curSavedAggregates.get(curSet);
            double[] metrics = new double[qualityMetrics.length];
            for (int i = 0; i < metrics.length; i++) {
                metrics[i] = qualityMetrics[i].value(aggs);
            }
            o1results.add(curSet);
            results.add(
                    new APLExplanationResult(qualityMetrics, new IntSet(curSet), aggs, metrics)
            );
        }
        return results;
    }

    public void setDoContainment(boolean doContainment) { this.doContainment = doContainment; }
}
