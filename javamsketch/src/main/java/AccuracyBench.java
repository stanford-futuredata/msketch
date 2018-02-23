import io.CSVOutput;
import io.DataSource;
import io.SimpleCSVDataSource;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccuracyBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private Map<String, List<Double>> methods;
    private List<Double> quantiles;
    private int numTrials;
    private int numSolveTrials;

    private boolean verbose = false;
    private boolean calcError = false;
    private boolean appendTimeStamp = true;

    public AccuracyBench(String confFile) throws IOException{
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        methods = conf.get("methods");
        quantiles = conf.get("quantiles");
        numTrials = conf.get("numTrials");
        numSolveTrials = conf.get("numSolveTrials");

        verbose = conf.get("verbose", false);
        calcError = conf.get("calcError", false);
        appendTimeStamp = conf.get("appendTimeStamp", false);
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        AccuracyBench bench = new AccuracyBench(confFile);
//        System.in.read();

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(bench.appendTimeStamp);
        output.writeAllResults(results, bench.testName);
    }

    public List<Map<String, String>> run() throws Exception {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        long startTime = System.currentTimeMillis();
        double[] data = source.get();
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;
        System.out.println("Loaded Data in: "+loadTime);
        List<Map<String, String>> results = new ArrayList<>();

        for (String sketchName : methods.keySet()) {
            List<Double> sizeParams = methods.get(sketchName);
            for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                for (double sParam : sizeParams) {
                    System.gc();
                    System.out.println(sketchName + ":" + curTrial + "@" + (int) sParam);
                    QuantileSketch curSketch = SketchLoader.load(sketchName);
                    curSketch.setVerbose(verbose);
                    curSketch.setCalcError(calcError);
                    curSketch.setSizeParam(sParam);
                    curSketch.initialize();

                    startTime = System.nanoTime();
                    curSketch.add(data);
                    endTime = System.nanoTime();
                    long trainTime = endTime - startTime;
                    if (verbose) {
                        System.out.println("Trained Sketch");
                    }

                    double[] qs = curSketch.getQuantiles(quantiles);
                    double[] errors = curSketch.getErrors();

                    curSketch.setCalcError(false);
                    startTime = System.nanoTime();
                    for (int curSolveTrial = 0; curSolveTrial < numSolveTrials; curSolveTrial++) {
                        curSketch.getQuantiles(quantiles);
                    }
                    endTime = System.nanoTime();
                    long queryTime = (endTime - startTime) / numSolveTrials;
                    curSketch.setCalcError(calcError);

                    for (int i = 0; i < qs.length; i++) {
                        double curP = quantiles.get(i);
                        double curQ = qs[i];
                        double curError = errors[i];

                        Map<String, String> curResults = new HashMap<>();
                        curResults.put("dataset", fileName);
                        curResults.put("sketch", curSketch.getName());
                        curResults.put("trial", String.format("%d",curTrial));
                        curResults.put("q", String.format("%f", curP));
                        curResults.put("quantile_estimate", String.format("%f", curQ));
                        curResults.put("bound_size", String.format("%f", curError));
                        curResults.put("space", String.format("%d", curSketch.getSize()));
                        curResults.put("size_param", String.format("%.2f", sParam));
                        curResults.put("train_time", String.format("%d", trainTime));
                        curResults.put("query_time", String.format("%d", queryTime));
                        curResults.put("n", String.format("%d", data.length));
                        results.add(curResults);
                    }
                }
            }
        }

        return results;
    }
}
