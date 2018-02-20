import com.yahoo.memory.Memory;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanation;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import edu.stanford.futuredata.macrobase.datamodel.Schema;
import edu.stanford.futuredata.macrobase.ingest.CSVDataFrameParser;
import io.CSVOutput;
import macrobase.APLMomentSummarizer;
import macrobase.APLOutlierSummarizer;
import macrobase.APLYahooSummarizer;
import macrobase.EstimatedSupportMetric;
import macrobase.SketchSupportMetric;
import sketches.YahooSketch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class MBCascadesBench {
    String testName;
    double minSupport = 0.1;
    double percentile = 1.0;
    String outlierColumn = "outliers1";
    int numWarmupTrials;
    int numTrials;
    int numMoments;
    String oracleCubeFilename;
    String momentCubeFilename;
    boolean doContainment;
    List<String> attributes;
    boolean verbose;
    String yahooFile;
    String groupsFile;
    double maxWarmupTime;
    double maxTrialTime;

    public MBCascadesBench(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        minSupport = conf.get("minSupport");
        percentile = conf.get("percentile");
        outlierColumn = conf.get("outlierColumn");
        numWarmupTrials = conf.get("numWarmupTrials", 20);
        numTrials = conf.get("numTrials", 10);
        numMoments = conf.get("numMoments", 8);
        oracleCubeFilename = conf.get("oracleCubeFilename");
        momentCubeFilename = conf.get("momentCubeFilename");
        doContainment = conf.get("doContainment", false);
        attributes = conf.get("attributes");
        verbose = conf.get("verbose", false);
        yahooFile = conf.get("yahooFile", "src/test/resources/milan_yahoo");
        groupsFile = conf.get("groupsFile");
        maxWarmupTime = conf.get("maxWarmupTime", 10.0);
        maxTrialTime = conf.get("maxTrialTime", 60.0);
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        MBCascadesBench bench = new MBCascadesBench(confFile);
        bench.run();
    }

    public void run() throws Exception {
        System.out.format("minSupport: %f, percentile: %f\n\n", minSupport, percentile);
//        testOracleOrder3();

        List<Map<String, String>> results = new ArrayList<Map<String, String>>();

        results.add(testCubeOrder3(true, new boolean[]{false, false, false}));
        results.add(testCubeOrder3(true, new boolean[]{true, false, false}));
        results.add(testCubeOrder3(true, new boolean[]{true, true, false}));
        results.add(testCubeOrder3(true, new boolean[]{true, true, true}));

        results.add(runYahoo());
        results.add(runYahoo2());

        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(true);
        output.writeAllResults(results, testName);
    }

    public void testOracleOrder3() throws Exception {
        List<String> requiredColumns = new ArrayList<>(attributes);
        Map<String, Schema.ColType> colTypes = new HashMap<>();
        colTypes.put("count", Schema.ColType.DOUBLE);
        colTypes.put(outlierColumn, Schema.ColType.DOUBLE);
        requiredColumns.add("count");
        requiredColumns.add(outlierColumn);
        CSVDataFrameParser loader = new CSVDataFrameParser(oracleCubeFilename, requiredColumns);
        loader.setColumnTypes(colTypes);
        DataFrame df = loader.load();

        APLOutlierSummarizer summ = new APLOutlierSummarizer();
        summ.setCountColumn("count");
        summ.setOutlierColumn(outlierColumn);
        summ.setMinSupport(minSupport);
        summ.setMinRatioMetric(10.0);
        summ.setAttributes(attributes);
        summ.setDoContainment(doContainment);
        summ.onlyUseSupport(true);
        for (int i = 0; i < numWarmupTrials; i++) {
            summ.process(df);
        }
        long start = System.nanoTime();
        for (int i = 0; i < numTrials; i++) {
            summ.process(df);
        }
        long timeElapsed = System.nanoTime() - start;
        System.out.format("Oracle time: %g\n", timeElapsed / (1.e9 * numTrials));
        APLExplanation e = summ.getResults();
        System.out.format("Num results: %d\n\n", e.getResults().size());
        if (verbose) {
            System.out.println(e.prettyPrint());
        }
    }

    public Map<String, String> testCubeOrder3(boolean useCascade, boolean[] useStages) throws Exception {
        List<String> requiredColumns = new ArrayList<>(attributes);
        Map<String, Schema.ColType> colTypes = new HashMap<>();
        List<String> momentColumns = new ArrayList<>();
        for (int i = 0; i <= numMoments; i++) {
            colTypes.put("m" + i, Schema.ColType.DOUBLE);
            requiredColumns.add("m" + i);
            momentColumns.add("m" + i);
        }
        List<String> logMomentColumns = new ArrayList<>();
        for (int i = 1; i <= numMoments; i++) {
            colTypes.put("lm" + i, Schema.ColType.DOUBLE);
            requiredColumns.add("lm" + i);
            logMomentColumns.add("lm" + i);
        }
        colTypes.put("min", Schema.ColType.DOUBLE);
        colTypes.put("max", Schema.ColType.DOUBLE);
        requiredColumns.add("min");
        requiredColumns.add("max");
        CSVDataFrameParser loader = new CSVDataFrameParser(momentCubeFilename, requiredColumns);
        loader.setColumnTypes(colTypes);
        DataFrame df = loader.load();

        APLMomentSummarizer summ = new APLMomentSummarizer();
        summ.setMinSupport(minSupport);
        summ.setMinRatioMetric(10.0);
        summ.setAttributes(attributes);
        summ.setMinColumn("min");
        summ.setMaxColumn("max");
        summ.setMomentColumns(momentColumns);
        summ.setLogMomentColumns(logMomentColumns);
        summ.setPercentile(percentile);
        summ.setCascade(useCascade);
        summ.setDoContainment(doContainment);
        summ.setUseStages(useStages);
        summ.setVerbose(false);
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            summ.process(df);
        }
        System.out.println("Warmup finished");
        summ.setVerbose(false);
        long actionTime = 0;
        int trialsDone = 0;
        summ.resetTime();
        long start = System.nanoTime();
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            summ.process(df);
            EstimatedSupportMetric metric = (EstimatedSupportMetric)summ.qualityMetricList.get(0);
            actionTime += metric.actionTime;
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;

        Map<String, String> result = new HashMap<String, String>();
        EstimatedSupportMetric metric = (EstimatedSupportMetric)summ.qualityMetricList.get(0);
        System.out.format("Overall time: %g\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("APL time: %g\n", summ.aplTime / (1.e9 * trialsDone));
        System.out.format("Query time: %g\n", summ.queryTime / (1.e9 * trialsDone));
        System.out.format("Merge time: %g\n", summ.mergeTime / (1.e9 * trialsDone));
        if (useCascade) {
            int prunedByNaive = metric.numEnterCascade - metric.numAfterNaiveCheck;
            int prunedByMarkov = metric.numAfterNaiveCheck - metric.numAfterMarkovBound;
            int prunedByMoments = metric.numAfterMarkovBound - metric.numAfterMomentBound;
            double overallThroughput = metric.numEnterCascade / (metric.actionTime / 1.e9);
            double naiveThroughput = metric.numEnterCascade / ((metric.actionTime - metric.markovBoundTime - metric.momentBoundTime - metric.maxentTime) / 1.e9);
            double markovThroughput = metric.numAfterNaiveCheck / (metric.markovBoundTime / 1.e9);
            double momentThroughput = metric.numAfterMarkovBound / (metric.momentBoundTime / 1.e9);
            double maxentThroughput = metric.numAfterMomentBound / (metric.maxentTime / 1.e9);
            double naiveTime = (metric.actionTime - metric.markovBoundTime - metric.momentBoundTime - metric.maxentTime) / (double)metric.actionTime;
            double markovTime = metric.markovBoundTime / (double)metric.actionTime;
            double momentTime = metric.momentBoundTime / (double)metric.actionTime;
            double maxentTime = metric.maxentTime / (double)metric.actionTime;
            System.out.format("Avg. cascade: %f qps\n", trialsDone * metric.numEnterCascade * 1.e9 / actionTime);
            System.out.format("Cascade PTR\n\t" +
                            "Entered cascade: %d (%f qps)\n\t" +
                            "Pruned by naive checks: %d (%f) (%f qps) (%f)\n\t" +
                            "Pruned by Markov bounds: %d (%f) (%f qps) (%f)\n\t" +
                            "Pruned by moment bounds: %d (%f) (%f qps) (%f)\n\t" +
                            "Reached maxent: %d (%f) (%f qps) (%f)\n",
                    metric.numEnterCascade, overallThroughput,
                    prunedByNaive, prunedByNaive / (double)metric.numEnterCascade, naiveThroughput, naiveTime,
                    prunedByMarkov, prunedByMarkov / (double)metric.numEnterCascade, markovThroughput, markovTime,
                    prunedByMoments, prunedByMoments / (double)metric.numEnterCascade, momentThroughput, momentTime,
                    metric.numAfterMomentBound, metric.numAfterMomentBound / (double)metric.numEnterCascade, maxentThroughput, maxentTime);

            result.put("simple", String.format("%b", useStages[0]));
            result.put("markov", String.format("%b", useStages[1]));
            result.put("racz", String.format("%b", useStages[2]));
            result.put("simple_hit", String.format("%d", metric.numEnterCascade));
            result.put("markov_hit", String.format("%d", metric.numAfterNaiveCheck));
            result.put("racz_hit", String.format("%d", metric.numAfterMarkovBound));
            result.put("maxent_hit", String.format("%d", metric.numAfterMomentBound));
            result.put("simple_throughput", String.format("%f", naiveThroughput));
            result.put("markov_throughput", String.format("%f", markovThroughput));
            result.put("racz_throughput", String.format("%f", momentThroughput));
            result.put("maxent_throughput", String.format("%f", maxentThroughput));
            result.put("overall_throughput", String.format("%f", overallThroughput));
            result.put("simple_time", String.format("%f", naiveTime));
            result.put("markov_time", String.format("%f", markovTime));
            result.put("racz_time", String.format("%f", momentTime));
            result.put("maxent_time", String.format("%f", maxentTime));
        } else {
            double overallThroughput = trialsDone * 1.e9 * metric.numEnterAction / (metric.actionTime);
            System.out.format("Entered Maxent: %d (%f qps)\n", metric.numEnterAction, overallThroughput);
        }
        result.put("avg_runtime", String.format("%f", timeElapsed / (1.e9 * trialsDone)));
        result.put("avg_apltime", String.format("%f", summ.aplTime / (1.e9 * trialsDone)));
        result.put("avg_querytime", String.format("%f", summ.queryTime / (1.e9 * trialsDone)));
        result.put("avg_mergetime", String.format("%f", summ.mergeTime / (1.e9 * trialsDone)));
        result.put("type", "cascade");
        result.put("min_support", String.format("%f", minSupport));
        result.put("cube", momentCubeFilename);
        result.put("containment", String.format("%b", doContainment));
        APLExplanation e = summ.getResults();
        System.out.format("Num results: %d\n\n", e.getResults().size());
//        if (verbose) {
//            System.out.println(e.prettyPrint());
//        }
        return result;
    }

    public Map<String, String> runYahoo() throws Exception {
        List<String> requiredColumns = new ArrayList<>(attributes);
        Map<String, Schema.ColType> colTypes = new HashMap<>();
        CSVDataFrameParser loader = new CSVDataFrameParser(groupsFile, requiredColumns);
        loader.setColumnTypes(colTypes);
        DataFrame df = loader.load();
        YahooSketch[][] aggregateColumns = APLYahooSummarizer.getAggregateColumns(yahooFile);

        APLYahooSummarizer summ = new APLYahooSummarizer();
        summ.setMinSupport(minSupport);
        summ.setAttributes(attributes);
        summ.setPercentile(percentile);
        summ.setDoContainment(doContainment);
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            summ.process(df, aggregateColumns);
        }
        System.out.println("Warmup finished");
        summ.resetTime();
        long start = System.nanoTime();
        int trialsDone = 0;
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            summ.process(df, aggregateColumns);
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;
        System.out.format("Yahoo time: %g\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("APL time: %g\n", summ.aplTime / (1.e9 * trialsDone));
        System.out.format("Query time: %g\n", summ.queryTime / (1.e9 * trialsDone));
        System.out.format("Merge time: %g\n\n", summ.mergeTime / (1.e9 * trialsDone));

        Map<String, String> result = new HashMap<String, String>();
        result.put("avg_runtime", String.format("%f", timeElapsed / (1.e9 * trialsDone)));
        result.put("avg_apltime", String.format("%f", summ.aplTime / (1.e9 * trialsDone)));
        result.put("avg_querytime", String.format("%f", summ.queryTime / (1.e9 * trialsDone)));
        result.put("avg_mergetime", String.format("%f", summ.mergeTime / (1.e9 * trialsDone)));
        result.put("type", "yahoo1");
        return result;
    }

    public Map<String, String> runYahoo2() throws Exception {
        List<String> requiredColumns = new ArrayList<>(attributes);
        Map<String, Schema.ColType> colTypes = new HashMap<>();
        CSVDataFrameParser loader = new CSVDataFrameParser(groupsFile, requiredColumns);
        loader.setColumnTypes(colTypes);
        DataFrame df = loader.load();
        YahooSketch[] sketches = APLYahooSummarizer.getAggregateColumns(yahooFile)[0];

        YahooSketch globalSketch = new YahooSketch();
        globalSketch.setSizeParam(16.0);
        globalSketch.initialize();
        globalSketch.mergeYahoo(sketches);
        double cutoff = globalSketch.getQuantiles(Collections.singletonList(1.0-percentile/100.0))[0];

        APLOutlierSummarizer summ = new APLOutlierSummarizer();
        summ.setCountColumn("counts");
        summ.setOutlierColumn("oCounts");
        summ.setMinSupport(minSupport);
        summ.setMinRatioMetric(3.0);
        summ.onlyUseSupport(true);
        summ.setAttributes(attributes);
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            DataFrame input = yahoo2precompute(df, sketches, cutoff);
            summ.process(input);
            HashMap<Integer, YahooSketch> aggs = yahoo2postmerge(summ.o1results, summ.encoded, df, sketches);
        }
        System.out.println("Warmup finished");
        summ.resetTime();
        long precomputationTime = 0;
        long postmergeTime = 0;
        long start = System.nanoTime();
        int trialsDone = 0;
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            long queryStart = System.nanoTime();
            DataFrame input = yahoo2precompute(df, sketches, cutoff);
            precomputationTime += System.nanoTime() - queryStart;
            summ.process(input);
            long mergeStart = System.nanoTime();
            HashMap<Integer, YahooSketch> aggs = yahoo2postmerge(summ.o1results, summ.encoded, df, sketches);
            postmergeTime += System.nanoTime() - mergeStart;
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;
        System.out.format("Yahoo 2 time: %g\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("APL time: %g\n", summ.aplTime / (1.e9 * trialsDone));
        System.out.format("Precomputation time: %g\n", precomputationTime / (1.e9 * trialsDone));
        System.out.format("Merge time: %g\n", summ.mergeTime / (1.e9 * trialsDone));
        System.out.format("Post-merge time: %g\n\n", postmergeTime / (1.e9 * trialsDone));

        Map<String, String> result = new HashMap<String, String>();
        result.put("avg_runtime", String.format("%f", timeElapsed / (1.e9 * trialsDone)));
        result.put("avg_apltime", String.format("%f", (summ.aplTime + precomputationTime + postmergeTime) / (1.e9 * trialsDone)));
        result.put("avg_querytime", String.format("%f", precomputationTime / (1.e9 * trialsDone)));
        result.put("avg_mergetime", String.format("%f", (summ.mergeTime + postmergeTime) / (1.e9 * trialsDone)));
        result.put("type", "yahoo2");
        return result;
    }

    public DataFrame yahoo2precompute(DataFrame input, YahooSketch[] sketches, double cutoff) throws Exception {
        double[] counts = new double[sketches.length];
        double[] outlierCounts = new double[sketches.length];

        for (int j = 0; j < sketches.length; j++) {
            YahooSketch sketch = sketches[j];
            counts[j] = sketch.getCount();
            outlierCounts[j] = sketch.getCount() * (1.0 - sketch.getCDF(cutoff));
        }

        input.addDoubleColumn("counts", counts);
        input.addDoubleColumn("oCounts", outlierCounts);

        return input;
    }

    public HashMap<Integer, YahooSketch> yahoo2postmerge(List<Integer> o1results, List<int[]> attributes,
                                                         DataFrame df, YahooSketch[] sketches) {
        HashMap<Integer, YahooSketch> setAggregates = new HashMap<>();
        double sizeParam = sketches[0].sketch.getK();

        for (int i = 0; i < sketches.length; i++) {
            int[] curRowAttributes = attributes.get(i);
            for (int c = 0; c < curRowAttributes.length; c++) {
                int curCandidate = curRowAttributes[c];
                if (!o1results.contains(curCandidate)) {
                    continue;
                }
                YahooSketch candidateVal = setAggregates.get(curCandidate);
                if (candidateVal == null) {
                    YahooSketch agg = new YahooSketch();
                    agg.setSizeParam(sizeParam);
                    agg.initialize();
                    agg.mergeYahoo(sketches[i]);
                    setAggregates.put(curCandidate, agg);
                } else {
                    setAggregates.put(curCandidate, candidateVal.mergeYahoo(sketches[i]));
                }
            }
        }

        return setAggregates;
    }
}