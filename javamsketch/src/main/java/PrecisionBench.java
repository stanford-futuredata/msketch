import io.CSVOutput;
import io.DataSource;
import io.SimpleCSVDataSource;
import sketches.MomentSketch;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.*;

public class PrecisionBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private Map<String, List<Double>> methods;
    private List<Integer> precisions;
    private List<Double> quantiles;
    private int numTrials;

    private boolean verbose = false;
    private boolean calcError = false;
    private boolean appendTimeStamp = true;

    public PrecisionBench(String confFile) throws IOException{
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        methods = conf.get("methods");
        precisions = conf.get("precisions");
        quantiles = conf.get("quantiles");
        numTrials = conf.get("numTrials");

        verbose = conf.get("verbose", false);
        calcError = conf.get("calcError", false);
        appendTimeStamp = conf.get("appendTimeStamp", false);
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        PrecisionBench bench = new PrecisionBench(confFile);

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

        for (int precision : precisions) {
            for (String sketchName : methods.keySet()) {
                List<Double> sizeParams = methods.get(sketchName);
                for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                    for (double sParam : sizeParams) {
                        System.gc();
                        if (verbose) {
                            System.out.println(sketchName + ":" + curTrial + "@" + (int) sParam + "p" + precision);
                        }
                        MomentSketch curSketch = (MomentSketch)SketchLoader.load(sketchName);
                        curSketch.setVerbose(verbose);
                        curSketch.setCalcError(calcError);
                        curSketch.setSizeParam(sParam);
                        curSketch.initialize();

                        startTime = System.nanoTime();
                        curSketch.add(data);
                        endTime = System.nanoTime();
                        long trainTime = endTime - startTime;

                        curSketch.convertToLowPrecision(precision);

                        double[] qs;
                        startTime = System.nanoTime();
                        try {
                            qs = curSketch.getQuantiles(quantiles);
                        } catch (Exception e) {
                            continue;
                        }
                        endTime = System.nanoTime();
                        long queryTime = endTime - startTime;
                        double[] errors = curSketch.getErrors();

                        for (int i = 0; i < qs.length; i++) {
                            double curP = quantiles.get(i);
                            double curQ = qs[i];
                            double curError = errors[i];

                            Map<String, String> curResults = new HashMap<>();
                            curResults.put("dataset", fileName);
                            curResults.put("sketch", curSketch.getName());
                            curResults.put("trial", String.format("%d", curTrial));
                            curResults.put("q", String.format("%f", curP));
                            curResults.put("quantile_estimate", String.format("%f", curQ));
                            curResults.put("bound_size", String.format("%f", curError));
                            curResults.put("space", String.format("%d", curSketch.getSize()));
                            curResults.put("size_param", String.format("%.2f", sParam));
                            curResults.put("train_time", String.format("%d", trainTime));
                            curResults.put("query_time", String.format("%d", queryTime));
                            curResults.put("precision", String.format("%d", precision));
                            curResults.put("n", String.format("%d", data.length));
                            results.add(curResults);
                        }
                    }
                }
            }
        }

        return results;
    }
}
