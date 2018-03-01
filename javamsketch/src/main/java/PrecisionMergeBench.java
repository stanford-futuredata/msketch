import io.CSVOutput;
import io.DataSource;
import io.SeqDataGrouper;
import io.SimpleCSVDataSource;
import sketches.CMomentSketch;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrecisionMergeBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private int cellSize;

    private Map<String, List<Double>> methods;
    private List<Integer> precisions;
    private List<Double> quantiles;
    private int numTrials;

    private boolean verbose;
    private boolean calcError;
    private boolean appendTimeStamp;

    public PrecisionMergeBench(String confFile) throws IOException{
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        cellSize = conf.get("cellSize");

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
        PrecisionMergeBench bench = new PrecisionMergeBench(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(bench.appendTimeStamp);
        output.writeAllResults(results, bench.testName);
    }

    private ArrayList<double[]> getCells() throws IOException {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        double[] data = source.get();
        SeqDataGrouper grouper = new SeqDataGrouper(cellSize);
        return grouper.group(data);
    }

    public List<Map<String, String>> run() throws Exception {
        System.out.println("Loading Data");
        long startTime = System.currentTimeMillis();
        ArrayList<double[]> cells = getCells();
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;
        System.out.println("Loaded Data in: "+loadTime);


        List<Map<String, String>> results = new ArrayList<>();

        int m = quantiles.size();

        for (int precision : precisions) {
            for (String sketchName : methods.keySet()) {
                List<Double> sizeParams = methods.get(sketchName);
                for (double sParam : sizeParams) {
                    startTime = System.nanoTime();
                    int numCells = cells.size();
                    ArrayList<QuantileSketch> cellSketches = new ArrayList<>(numCells);
                    for (int i = 0; i < numCells; i++) {
                        double[] cellData = cells.get(i);
                        CMomentSketch curSketch = new CMomentSketch(1e-9);
                        curSketch.setVerbose(verbose);
                        curSketch.setCalcError(calcError);
                        curSketch.setSizeParam(sParam);
                        curSketch.initialize();
                        curSketch.add(cellData);
                        curSketch.convertToLowPrecision(precision);
                        cellSketches.add(curSketch);
                    }
                    endTime = System.nanoTime();
                    long trainTime = endTime - startTime;

                    for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                        System.gc();
                        System.out.println(sketchName + ":" + (int) sParam + "#" + curTrial);

                        startTime = System.nanoTime();
                        CMomentSketch mergedSketch = new CMomentSketch(1e-9);
                        mergedSketch.setVerbose(verbose);
                        mergedSketch.setCalcError(calcError);
                        mergedSketch.setSizeParam(sParam);
                        mergedSketch.initialize();
                        mergedSketch.merge(cellSketches);
                        endTime = System.nanoTime();
                        long mergeTime = endTime - startTime;

                        System.gc();

                        startTime = System.nanoTime();
                        double[] qs = mergedSketch.getQuantiles(quantiles);
                        endTime = System.nanoTime();
                        long queryTime = endTime - startTime;
                        double[] errors = mergedSketch.getErrors();

                        for (int i = 0; i < qs.length; i++) {
                            double curP = quantiles.get(i);
                            double curQ = qs[i];
                            double curError = errors[i];

                            Map<String, String> curResults = new HashMap<>();
                            curResults.put("dataset", fileName);
                            curResults.put("sketch", mergedSketch.getName());
                            curResults.put("trial", String.format("%d", curTrial));
                            curResults.put("q", String.format("%f", curP));
                            curResults.put("quantile_estimate", String.format("%f", curQ));
                            curResults.put("bound_size", String.format("%f", curError));
                            curResults.put("space", String.format("%d", mergedSketch.getSize()));
                            curResults.put("size_param", String.format("%.2f", sParam));
                            curResults.put("train_time", String.format("%d", trainTime));
                            curResults.put("merge_time", String.format("%d", mergeTime));
                            curResults.put("query_time", String.format("%d", queryTime));
                            curResults.put("precision", String.format("%d", precision));
                            curResults.put("n", String.format("%d", numCells));
                            results.add(curResults);
                        }
                    }
                }
            }
        }

        return results;
    }
}
