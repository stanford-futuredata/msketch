import com.yahoo.memory.Memory;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanation;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import edu.stanford.futuredata.macrobase.datamodel.Schema;
import edu.stanford.futuredata.macrobase.ingest.CSVDataFrameParser;
import io.CSVOutput;
import macrobase.*;
import sketches.YahooSketch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class MBCascadesBench {
    String testName;
    double minSupport;
    double minGlobalRatio;
    double percentile;
    int numMoments;
    String momentCubeFilename;
    boolean doContainment;
    List<String> attributes;
    boolean verbose;
    String yahooFile;
    String groupsFile;
    double maxWarmupTime;
    double maxTrialTime;
    boolean useSupport;
    boolean useGlobalRatio;

    public MBCascadesBench(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        minSupport = conf.get("minSupport");
        minGlobalRatio = conf.get("minGlobalRatio");
        percentile = conf.get("percentile", 1.0);
        numMoments = conf.get("numMoments", 8);
        momentCubeFilename = conf.get("momentCubeFilename");
        doContainment = conf.get("doContainment", true);
        attributes = conf.get("attributes");
        verbose = conf.get("verbose", false);
        yahooFile = conf.get("yahooFile", "src/test/resources/milan_yahoo");
        groupsFile = conf.get("groupsFile");
        maxWarmupTime = conf.get("maxWarmupTime", 10.0);
        maxTrialTime = conf.get("maxTrialTime", 60.0);
        useSupport = conf.get("useSupport", true);
        useGlobalRatio = conf.get("useGlobalRatio", false);
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        MBCascadesBench bench = new MBCascadesBench(confFile);
        bench.run();
    }

    public void run() throws Exception {
        System.out.format("minSupport: %f, minGlobalRatio: %f, percentile: %f\n\n", minSupport, minGlobalRatio, percentile);

        List<Map<String, String>> results = new ArrayList<Map<String, String>>();

        results.add(runCascade(new boolean[]{false, false, false}));
        results.add(runCascade(new boolean[]{true, false, false}));
        results.add(runCascade(new boolean[]{true, true, false}));
        results.add(runCascade(new boolean[]{true, true, true}));

        results.add(runYahoo());
        results.add(runYahoo2());

        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(true);
        output.writeAllResults(results, testName);
    }

    public Map<String, String> runCascade(boolean[] useStages) throws Exception {
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
        summ.setMinRatioMetric(minGlobalRatio);
        summ.setAttributes(attributes);
        summ.setMinColumn("min");
        summ.setMaxColumn("max");
        summ.setMomentColumns(momentColumns);
        summ.setLogMomentColumns(logMomentColumns);
        summ.setPercentile(percentile);
        summ.setDoContainment(doContainment);
        summ.setUseStages(useStages);
        summ.setVerbose(false);
        summ.setUseSupport(useSupport);
        summ.setUseGlobalRatio(useGlobalRatio);
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            System.gc();
            summ.process(df);
        }
        System.out.println("Warmup finished");
        summ.setVerbose(false);
        int trialsDone = 0;
        summ.resetTime();
        long start = System.nanoTime();
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            System.gc();
            summ.process(df);
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;

        Map<String, String> result = new HashMap<String, String>();
        CascadeQualityMetric metric = (CascadeQualityMetric)summ.qualityMetricList.get(0);
        System.out.format("Overall time: %g\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("APL time: %g\n", summ.aplTime / (1.e9 * trialsDone));
        System.out.format("Query time: %g\n", summ.queryTime / (1.e9 * trialsDone));
        System.out.format("Merge time: %g\n", summ.mergeTime / (1.e9 * trialsDone));

        int prunedByNaive = metric.numEnterCascade - metric.numAfterNaiveCheck;
        int prunedByMarkov = metric.numAfterNaiveCheck - metric.numAfterMarkovBound;
        int prunedByMoments = metric.numAfterMarkovBound - metric.numAfterMomentBound;
        double overallThroughput = metric.numEnterCascade / (summ.actionTime / (1.e9 * trialsDone));
        double naiveThroughput = metric.numEnterCascade / ((summ.actionTime - summ.markovBoundTime - summ.momentBoundTime - summ.maxentTime) / (1.e9 * trialsDone));
        double markovThroughput = metric.numAfterNaiveCheck / (summ.markovBoundTime / (1.e9 * trialsDone));
        double momentThroughput = metric.numAfterMarkovBound / (summ.momentBoundTime / (1.e9 * trialsDone));
        double maxentThroughput = metric.numAfterMomentBound / (summ.maxentTime / (1.e9 * trialsDone));
        double naiveTime = (summ.actionTime - summ.markovBoundTime - summ.momentBoundTime - summ.maxentTime) / (double)summ.actionTime;
        double markovTime = summ.markovBoundTime / (double)summ.actionTime;
        double momentTime = summ.momentBoundTime / (double)summ.actionTime;
        double maxentTime = summ.maxentTime / (double)summ.actionTime;
        System.out.format("Avg. cascade: %f qps\n", trialsDone * metric.numEnterCascade * 1.e9 / summ.queryTime);
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

        result.put("avg_runtime", String.format("%f", timeElapsed / (1.e9 * trialsDone)));
        result.put("avg_apltime", String.format("%f", summ.aplTime / (1.e9 * trialsDone)));
        result.put("avg_querytime", String.format("%f", summ.queryTime / (1.e9 * trialsDone)));
        result.put("avg_mergetime", String.format("%f", summ.mergeTime / (1.e9 * trialsDone)));
        result.put("type", "cascade");
        result.put("min_support", String.format("%f", minSupport));
        result.put("min_globalratio", String.format("%f", minGlobalRatio));
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
        summ.setMinRatioMetric(minGlobalRatio);
        summ.setAttributes(attributes);
        summ.setPercentile(percentile);
        summ.setDoContainment(doContainment);
        summ.setUseSupport(useSupport);
        summ.setUseGlobalRatio(useGlobalRatio);
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            System.gc();
            summ.process(df, aggregateColumns);
        }
        System.out.println("Warmup finished");
        summ.resetTime();
        long start = System.nanoTime();
        int trialsDone = 0;
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            System.gc();
            summ.process(df, aggregateColumns);
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;
        System.out.format("Yahoo time: %g\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("APL time: %g\n", summ.aplTime / (1.e9 * trialsDone));
        System.out.format("Query time: %g\n", summ.queryTime / (1.e9 * trialsDone));
        System.out.format("Merge time: %g\n", summ.mergeTime / (1.e9 * trialsDone));
        System.out.format("Num results: %d\n\n", summ.aplResults.size());

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

        APLOutlierSummarizer summ = new APLOutlierSummarizer();
        summ.setCountColumn("counts");
        summ.setOutlierColumn("oCounts");
        summ.setMinSupport(minSupport);
        summ.setMinRatioMetric(minGlobalRatio);
        summ.setAttributes(attributes);
        summ.setUseSupport(useSupport);
        summ.setUseGlobalRatio(useGlobalRatio);
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            System.gc();
            double cutoff = yahoo2premerge(sketches);
            DataFrame input = yahoo2precompute(df, sketches, cutoff);
            summ.process(input);
//            HashMap<Integer, YahooSketch> aggs = yahoo2postmerge(summ.o1results, summ.encoded, df, sketches);
        }
        System.out.println("Warmup finished");
        summ.resetTime();
        long premergeTime = 0;
        long precomputationTime = 0;
        long postmergeTime = 0;
        long start = System.nanoTime();
        int trialsDone = 0;
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            System.gc();
            long premergeStart = System.nanoTime();
            double cutoff = yahoo2premerge(sketches);
            premergeTime += System.nanoTime() - premergeStart;
            System.out.format("Premerge: %f\n", (System.nanoTime() - premergeStart)/1.e9);
            long queryStart = System.nanoTime();
            DataFrame input = yahoo2precompute(df, sketches, cutoff);
            precomputationTime += System.nanoTime() - queryStart;
            System.out.format("Precomp: %f\n", (System.nanoTime() - queryStart)/1.e9);
            summ.process(input);
//            long mergeStart = System.nanoTime();
//            HashMap<Integer, YahooSketch> aggs = yahoo2postmerge(summ.o1results, summ.encoded, df, sketches);
//            postmergeTime += System.nanoTime() - mergeStart;
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;
        System.out.format("Yahoo 2 time: %g\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("APL time: %g\n", summ.aplTime / (1.e9 * trialsDone));
        System.out.format("Pre-merge time: %g\n", premergeTime / (1.e9 * trialsDone));
        System.out.format("Precomputation time: %g\n", precomputationTime / (1.e9 * trialsDone));
        System.out.format("Merge time: %g\n", summ.mergeTime / (1.e9 * trialsDone));
//        System.out.format("Post-merge time: %g\n", postmergeTime / (1.e9 * trialsDone));
        System.out.format("Num results: %d\n\n", summ.o1results.size());

        Map<String, String> result = new HashMap<String, String>();
        result.put("avg_runtime", String.format("%f", timeElapsed / (1.e9 * trialsDone)));
        result.put("avg_apltime", String.format("%f", (summ.aplTime + precomputationTime + premergeTime) / (1.e9 * trialsDone)));
        result.put("avg_querytime", String.format("%f", precomputationTime / (1.e9 * trialsDone)));
        result.put("avg_mergetime", String.format("%f", (summ.mergeTime + premergeTime) / (1.e9 * trialsDone)));
        result.put("type", "yahoo2");
        return result;
    }

    public double yahoo2premerge(YahooSketch[] sketches) throws Exception {
        YahooSketch globalSketch = new YahooSketch();
        globalSketch.setSizeParam(16.0);
        globalSketch.initialize();
        globalSketch.mergeYahoo(sketches);
        double cutoff = globalSketch.getQuantiles(Collections.singletonList(1.0-percentile/100.0))[0];
        return cutoff;
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