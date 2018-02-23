package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanationResult;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.apriori.APrioriSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.apriori.IntSet;
import edu.stanford.futuredata.macrobase.util.MacrobaseInternalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sketches.QuantileSketch;
import sketches.SketchLoader;
import sketches.YahooSketch;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Class for handling the generic, algorithmic aspects of apriori explanation.
 * This class assumes that subgroups posses "aggregates" such as count and outlier_count
 * which can be combined additively. Then, we use APriori to find the subgroups which
 * are the most interesting as defined by "quality metrics" on these aggregates.
 */
public class SketchAPrioriLinear {
    Logger log = LoggerFactory.getLogger("APLSummarizer");

    // **Parameters**
    private SketchSupportMetric[] qualityMetrics;
    private double[] thresholds;
    private boolean doContainment = true;

    public long mergeTime = 0;
    public long queryTime = 0;
    private long start;

    // **Cached values**

    // Singleton viable sets for quick lookup
    private HashSet<Integer> singleNext;
    // Sets that has high enough support but not high risk ratio, need to be explored
    private HashMap<Integer, HashSet<IntSet>> setNext;
    // Aggregate values for all of the sets we saved
    private HashMap<Integer, HashMap<IntSet, YahooSketch[]>> savedAggregates;

    public SketchAPrioriLinear(
            List<SketchSupportMetric> qualityMetrics,
            List<Double> thresholds
    ) {
        this.qualityMetrics = qualityMetrics.toArray(new SketchSupportMetric[0]);
        this.thresholds = new double[thresholds.size()];
        for (int i = 0; i < thresholds.size(); i++) {
            this.thresholds[i] = thresholds.get(i);
        }
        this.setNext = new HashMap<>(3);
        this.savedAggregates = new HashMap<>(3);
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
        for (int curOrder = 1; curOrder <= 3; curOrder++) {
            // Precalculate all possible candidate sets from "next" sets of
            // previous orders. We will focus on aggregating results for these
            // sets.
            final HashSet<IntSet> precalculatedCandidates = precalculateCandidates(curOrder);
            // Run the critical path of the algorithm--candidate generation--in parallel.
            final int curOrderFinal = curOrder;
            final int numThreads = 1; //Runtime.getRuntime().availableProcessors();
            // Group by and calculate aggregates for each of the candidates
            final ArrayList<HashMap<IntSet, YahooSketch[]>> threadSetAggregates = new ArrayList<>(numThreads);
            for (int i = 0; i < numThreads; i++) {
                threadSetAggregates.add(new HashMap<>());
            }
            final CountDownLatch doneSignal = new CountDownLatch(numThreads);
            start = System.nanoTime();
            for (int threadNum = 0; threadNum < numThreads; threadNum++) {
                final int startIndex = (numRows * threadNum) / numThreads;
                final int endIndex = (numRows * (threadNum + 1)) / numThreads;
                final HashMap<IntSet, YahooSketch[]> thisThreadSetAggregates = threadSetAggregates.get(threadNum);
                // Do the critical path calculation in a lambda
                Runnable APrioriLinearRunnable = () -> {
                        for (int i = startIndex; i < endIndex; i++) {
                            int[] curRowAttributes = attributes.get(i);
                            ArrayList<IntSet> candidates = getCandidates(
                                    curOrderFinal,
                                    curRowAttributes,
                                    precalculatedCandidates
                            );
                            int numCandidates = candidates.size();
                            for (int c = 0; c < numCandidates; c++) {
                                IntSet curCandidate = candidates.get(c);
                                YahooSketch[] candidateVal = thisThreadSetAggregates.get(curCandidate);
                                if (candidateVal == null) {
                                    YahooSketch[] aggregates = new YahooSketch[numAggregates];
                                    for (int a = 0; a < numAggregates; a++) {
                                        YahooSketch agg = new YahooSketch();
                                        agg.setSizeParam(sizeParam);
                                        agg.initialize();
                                        aggregates[a] = agg.mergeYahoo(aRows[i][0]);
                                    }
//                                    System.out.format("%s %d\n", Arrays.toString(curCandidate.values), aggregates[0].getCount());
                                    thisThreadSetAggregates.put(curCandidate, aggregates);
                                } else {
                                    for (int a = 0; a < numAggregates; a++) {
                                        candidateVal[a] = candidateVal[a].mergeYahoo(aRows[i][a]);
                                    }
//                                    System.out.format("%s %d\n", Arrays.toString(curCandidate.values), candidateVal[0].getCount());
                                }
                            }
                        }
                        doneSignal.countDown();
                };
                // Run numThreads lambdas in separate threads
                Thread APrioriLinearThread = new Thread(APrioriLinearRunnable);
                APrioriLinearThread.start();
            }
            try {
                doneSignal.await();
            } catch (InterruptedException ex) {ex.printStackTrace();}

            // Collect the threadSetAggregates into one big set of aggregates.
            HashMap<IntSet, YahooSketch[]> setAggregates = new HashMap<>();
            for (HashMap<IntSet, YahooSketch[]> set : threadSetAggregates) {
                for (Map.Entry<IntSet, YahooSketch[]> curCandidate : set.entrySet()) {
                    IntSet curCandidateKey = curCandidate.getKey();
                    YahooSketch[] curCandidateValue = curCandidate.getValue();
                    YahooSketch[] candidateVal = setAggregates.get(curCandidateKey);
                    if (candidateVal == null) {
                        setAggregates.put(curCandidateKey, Arrays.copyOf(curCandidateValue, numAggregates));
                    } else {
                        for (int a = 0; a < numAggregates; a++) {
                            candidateVal[a] = candidateVal[a].mergeYahoo(curCandidateValue[a]);
                        }
                    }
                }
            }
            mergeTime += System.nanoTime() - start;

            HashSet<IntSet> curOrderNext = new HashSet<>();
            HashSet<IntSet> curOrderSaved = new HashSet<>();
            int pruned = 0;
            for (IntSet curCandidate: setAggregates.keySet()) {
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
                    if (!doContainment) {
                        curOrderNext.add(curCandidate);
                    }
                } else if (action == QualityMetric.Action.NEXT) {
                    // otherwise if a set still has potentially good subsets,
                    // save it for further examination
                    curOrderNext.add(curCandidate);
                } else {
                    pruned++;
                }
            }

            HashMap<IntSet, YahooSketch[]> curSavedAggregates = new HashMap<>(curOrderSaved.size());
            for (IntSet curSaved : curOrderSaved) {
                curSavedAggregates.put(curSaved, setAggregates.get(curSaved));
            }
            savedAggregates.put(curOrder, curSavedAggregates);
            setNext.put(curOrder, curOrderNext);
            if (curOrder == 1) {
                singleNext = new HashSet<>(curOrderNext.size());
                for (IntSet i : curOrderNext) {
                    singleNext.add(i.get(0));
                }
            }
        }

        List<APLSketchExplanationResult> results = new ArrayList<>();
        for (int curOrder: savedAggregates.keySet()) {
            HashMap<IntSet, YahooSketch[]> curOrderSavedAggregates = savedAggregates.get(curOrder);
            for (IntSet curSet : curOrderSavedAggregates.keySet()) {
                YahooSketch[] aggregates = curOrderSavedAggregates.get(curSet);
                double[] metrics = new double[qualityMetrics.length];
                for (int i = 0; i < metrics.length; i++) {
                    metrics[i] = qualityMetrics[i].value(aggregates);
                }
                results.add(
                        new APLSketchExplanationResult(qualityMetrics, curSet, aggregates, metrics)
                );
            }
        }
        return results;
    }

    private HashSet<IntSet> precalculateCandidates(int curOrder) {
        if (curOrder < 3) {
            return null;
        } else {
            return APrioriSummarizer.getOrder3Candidates(
                    setNext.get(2),
                    singleNext
            );
        }
    }

    /**
     * Returns all candidate subsets of a given set and order
     * @param order size of the subsets
     * @return all subsets that can be built from smaller subsets in setNext
     */
    private ArrayList<IntSet> getCandidates(
            int order,
            int[] set,
            HashSet<IntSet> precalculatedCandidates
    ) {
        ArrayList<IntSet> candidates = new ArrayList<>();
        if (order == 1) {
            for (int i : set) {
                candidates.add(new IntSet(i));
            }
        } else {
            ArrayList<Integer> toExamine = new ArrayList<>();
            for (int v : set) {
                if (singleNext.contains(v)) {
                    toExamine.add(v);
                }
            }
            int numValidSingles = toExamine.size();

            if (order == 2) {
                for (int p1 = 0; p1 < numValidSingles; p1++) {
                    int p1v = toExamine.get(p1);
                    for (int p2 = p1 + 1; p2 < numValidSingles; p2++) {
                        int p2v = toExamine.get(p2);
                        candidates.add(new IntSet(p1v, p2v));
                    }
                }
            } else if (order == 3) {
                HashSet<IntSet> pairNext = setNext.get(2);
                for (int p1 = 0; p1 < numValidSingles; p1++) {
                    int p1v = toExamine.get(p1);
                    for (int p2 = p1 + 1; p2 < numValidSingles; p2++) {
                        int p2v = toExamine.get(p2);
                        IntSet pair1 = new IntSet(p1v, p2v);
                        if (pairNext.contains(pair1)) {
                            for (int p3 = p2 + 1; p3 < numValidSingles; p3++) {
                                int p3v = toExamine.get(p3);
                                IntSet curSet = new IntSet(p1v, p2v, p3v);
                                if (precalculatedCandidates.contains(curSet)) {
                                    candidates.add(curSet);
                                }
                            }
                        }
                    }
                }
            } else {
                throw new MacrobaseInternalError("High Order not supported");
            }
        }
        return candidates;
    }

    public void setDoContainment(boolean doContainment) { this.doContainment = doContainment; }
}
