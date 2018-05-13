import io.CSVOutput;
import io.DataSource;
import io.SeqDataGrouper;
import io.SimpleCSVDataSource;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.*;

public class ParallelMergeBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private int cellSize;
    private List<Double> cellFractions;
    private List<Integer> numMergeThreads;
    private int numDuplications;

    private Map<String, List<Double>> methods;
    private List<Double> quantiles;
    private int numTrials;
    private int numSolveTrials;

    private boolean verbose;
    private boolean calcError;
    private boolean appendTimeStamp;

    public ParallelMergeBench(String confFile) throws IOException{
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        cellSize = conf.get("cellSize");
        List<Double> defaultCellFractions = Arrays.asList(1.0);
        cellFractions = conf.get("cellFractions", defaultCellFractions);
        numMergeThreads = conf.get("numMergeThreads");
        numDuplications = conf.get("numDuplications", 1);

        methods = conf.get("methods");
        quantiles = conf.get("quantiles");
        numTrials = conf.get("numTrials");
        numSolveTrials = conf.get("numSolveTrials", 1);

        verbose = conf.get("verbose", false);
        calcError = conf.get("calcError", false);
        appendTimeStamp = conf.get("appendTimeStamp", false);
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        ParallelMergeBench bench = new ParallelMergeBench(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(bench.appendTimeStamp);
        output.writeAllResults(results, bench.testName);
    }

    private ArrayList<double[]> getCells() throws IOException {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        double[] data = source.get();
        if (numDuplications > 1) {
            double[] dupData = new double[data.length * numDuplications];
            for (int i = 0; i < numDuplications; i++) {
                System.arraycopy(data, 0, dupData, data.length * i, data.length);
            }
            data = dupData;
        }
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

//        System.in.read();
        List<Map<String, String>> results = new ArrayList<>();

        int m = quantiles.size();

        for (String sketchName : methods.keySet()) {
            List<Double> sizeParams = methods.get(sketchName);
            for (double sParam : sizeParams) {
                startTime = System.nanoTime();
                int numCells = cells.size();
                ArrayList<QuantileSketch> cellSketches = new ArrayList<>(numCells);
                for (int i = 0; i < numCells; i++) {
                    double[] cellData = cells.get(i);
                    QuantileSketch curSketch = SketchLoader.load(sketchName);
                    curSketch.setCalcError(calcError);
                    curSketch.setSizeParam(sParam);
                    curSketch.initialize();
                    curSketch.add(cellData);
                    cellSketches.add(curSketch);
                }
                endTime = System.nanoTime();
                long trainTime = endTime - startTime;

                for (int mergeFractionIdx = 0; mergeFractionIdx < cellFractions.size(); mergeFractionIdx++) {
                    double curFraction = cellFractions.get(mergeFractionIdx);
                    int numCellSketchesToMerge = (int) (curFraction * numCells);
                    ArrayList<QuantileSketch> cellSketchesToMerge = new ArrayList<>(numCellSketchesToMerge);
                    for (int i = 0; i < numCellSketchesToMerge; i++) {
                        cellSketchesToMerge.add(cellSketches.get(i));
                    }

                    for (int numThreads : numMergeThreads) {
                        for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                            System.gc();
                            System.out.println(sketchName + ":" + (int) sParam + "@" + numThreads + "#" + curTrial);

                            startTime = System.nanoTime();
                            QuantileSketch mergedSketch = SketchLoader.load(sketchName);
                            mergedSketch.setCalcError(calcError);
                            mergedSketch.setSizeParam(sParam);
                            mergedSketch.setVerbose(verbose);
                            mergedSketch.initialize();
                            QuantileSketch dummy = mergedSketch.parallelMerge(cellSketchesToMerge, numThreads);
                            endTime = System.nanoTime();
                            long mergeTime = endTime - startTime;
                            if (dummy == null) {
                                // for failures on TDigest
                                curTrial--;
                                continue;
                            }

                            System.gc();

                            double[] qs = new double[1];
                            startTime = System.nanoTime();
                            for (int i = 0; i < numSolveTrials; i++) {
                                qs = mergedSketch.getQuantiles(quantiles);
                            }
                            endTime = System.nanoTime();
                            long queryTime = (endTime - startTime) / numSolveTrials;
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
                                curResults.put("n", String.format("%d", cellSketchesToMerge.size()));
                                curResults.put("num_threads", String.format("%d", numThreads));
                                results.add(curResults);
                            }
                        }
                    }
                }
            }
        }

        return results;
    }
}
