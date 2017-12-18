import io.CSVOutput;
import io.DataGrouper;
import io.DataSource;
import io.SimpleCSVDataSource;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MergeBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private int cellSize;

    private Map<String, List<Double>> methods;
    private List<Double> quantiles;
    private int numTrials;

    public MergeBench(String confFile) throws IOException{
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        cellSize = conf.get("cellSize");

        methods = conf.get("methods");
        quantiles = conf.get("quantiles");
        numTrials = conf.get("numTrials");
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        MergeBench bench = new MergeBench(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.writeAllResults(results, bench.testName);
    }

    private ArrayList<double[]> getCells() throws IOException {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        double[] data = source.get();
        DataGrouper grouper = new DataGrouper(data);
        return grouper.groupSequentially(cellSize);
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
//        double[] trueQuantiles = QuantileUtil.getTrueQuantiles(quantiles, data);

        for (String sketchName : methods.keySet()) {
            List<Double> sizeParams = methods.get(sketchName);
            for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                for (double sParam : sizeParams) {
                    System.gc();
                    System.out.println(sketchName+":"+curTrial+"@"+(int)sParam);

                    startTime = System.nanoTime();
                    int numCells = cells.size();
                    QuantileSketch[] cellSketches = new QuantileSketch[numCells];
                    for (int i = 0; i < numCells; i++) {
                        double[] cellData = cells.get(i);
                        QuantileSketch curSketch = SketchLoader.load(sketchName);
                        curSketch.setCalcError(true);
                        curSketch.setSizeParam(sParam);
                        curSketch.initialize();
                        curSketch.add(cellData);
                        cellSketches[i] = curSketch;
                    }
                    endTime = System.nanoTime();
                    long trainTime = endTime - startTime;

                    startTime = System.nanoTime();
                    QuantileSketch mergedSketch = SketchLoader.load(sketchName);
                    mergedSketch.setCalcError(true);
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
                        curResults.put("trial", String.format("%d",curTrial));
                        curResults.put("q", String.format("%f", curP));
//                    curResults.put("quantile_true", String.format("%f", trueQuantiles[i]));
                        curResults.put("quantile_estimate", String.format("%f", curQ));
                        curResults.put("bound_size", String.format("%f", curError));
                        curResults.put("space", String.format("%d", mergedSketch.getSize()));
                        curResults.put("size_param", String.format("%.2f", sParam));
                        curResults.put("train_time", String.format("%d", trainTime));
                        curResults.put("merge_time", String.format("%d", mergeTime));
                        curResults.put("query_time", String.format("%d", queryTime));
                        curResults.put("n", String.format("%d", numCells));
                        results.add(curResults);
                    }
                }
            }
        }

        return results;
    }
}
