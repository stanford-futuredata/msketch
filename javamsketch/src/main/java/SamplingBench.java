import com.codahale.metrics.Sampling;
import io.CSVOutput;
import io.DataSource;
import io.SeqDataGrouper;
import io.SimpleCSVDataSource;
import sketches.QuantileSketch;
import sketches.ReservoirSamplingSketch;
import sketches.SamplingSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.*;

public class SamplingBench {
    private static String fileName = "../sampledata/shuttle.csv";
    private static int columnIdx = 0;
    private static int cellSize = 1000;

    private static List<Double> quantiles = Arrays.asList(0.1, 0.5, 0.9);
    private static int numTrials = 5000;

    private boolean verbose = false;

    public static void main(String[] args) throws Exception {
        run();
    }

    private static ArrayList<double[]> getCells() throws IOException {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        double[] data = source.get();
        SeqDataGrouper grouper = new SeqDataGrouper(cellSize);
        return grouper.group(data);
    }

    public static void run() throws Exception {
        ArrayList<double[]> cells = getCells();
        int numCells = cells.size();

        long startTime;
        long endTime;

        int m = quantiles.size();
        List<Double> sizeParams = Arrays.asList(5., 50., 500.);

        for (int i = 0; i < 1000; i++) {
            ArrayList<QuantileSketch> cellSketches = new ArrayList<>(numCells);
            for (int c = 0; c < numCells; c++) {
                double[] cellData = cells.get(c);
                SamplingSketch curSketch = new SamplingSketch();
                curSketch.setCalcError(true);
                curSketch.setSizeParam(50);
                curSketch.initialize();
                curSketch.add(cellData);
                cellSketches.add(curSketch);
            }

            SamplingSketch mergedSketch = new SamplingSketch();
            mergedSketch.setCalcError(true);
            mergedSketch.setSizeParam(50);
            mergedSketch.initialize();
            mergedSketch.merge(cellSketches);

            double[] qs = mergedSketch.getQuantiles(quantiles);
            double[] errors = mergedSketch.getErrors();
        }

        System.out.println("Yahoo datasketches");
        for (double sParam : sizeParams) {
            long trainTime = 0;
            long mergeTime = 0;
            long queryTime = 0;

            for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                startTime = System.nanoTime();
                ArrayList<QuantileSketch> cellSketches = new ArrayList<>(numCells);
                for (int i = 0; i < numCells; i++) {
                    double[] cellData = cells.get(i);
                    SamplingSketch curSketch = new SamplingSketch();
                    curSketch.setCalcError(true);
                    curSketch.setSizeParam(sParam);
                    curSketch.initialize();
                    curSketch.add(cellData);
                    cellSketches.add(curSketch);
                }
                endTime = System.nanoTime();
                trainTime += (endTime - startTime);

                startTime = System.nanoTime();
                SamplingSketch mergedSketch = new SamplingSketch();
                mergedSketch.setCalcError(true);
                mergedSketch.setSizeParam(sParam);
                mergedSketch.initialize();
                mergedSketch.merge(cellSketches);
                endTime = System.nanoTime();
                mergeTime += (endTime - startTime);

                startTime = System.nanoTime();
                double[] qs = mergedSketch.getQuantiles(quantiles);
                endTime = System.nanoTime();
                queryTime += (endTime - startTime);
                double[] errors = mergedSketch.getErrors();
            }

            System.out.format("k=%f Train/merge/query %g/%g/%g\n",
                    sParam,
                    trainTime / (1.0e9 * numTrials),
                    mergeTime / (1.0e9 * numTrials),
                    queryTime / (1.0e9 * numTrials));
        }

        System.out.println("Reservoir Sampling Sketch");
        for (double sParam : sizeParams) {
            long trainTime = 0;
            long mergeTime = 0;
            long queryTime = 0;

            for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                startTime = System.nanoTime();
                ArrayList<QuantileSketch> cellSketches = new ArrayList<>(numCells);
                for (int i = 0; i < numCells; i++) {
                    double[] cellData = cells.get(i);
                    ReservoirSamplingSketch curSketch = new ReservoirSamplingSketch();
                    curSketch.setCalcError(true);
                    curSketch.setSizeParam(sParam);
                    curSketch.initialize();
                    curSketch.add(cellData);
                    cellSketches.add(curSketch);
                }
                endTime = System.nanoTime();
                trainTime += (endTime - startTime);

                startTime = System.nanoTime();
                ReservoirSamplingSketch mergedSketch = new ReservoirSamplingSketch();
                mergedSketch.setCalcError(true);
                mergedSketch.setSizeParam(sParam);
                mergedSketch.initialize();
                mergedSketch.merge(cellSketches);
                endTime = System.nanoTime();
                mergeTime += (endTime - startTime);

                startTime = System.nanoTime();
                double[] qs = mergedSketch.getQuantiles(quantiles);
                endTime = System.nanoTime();
                queryTime += (endTime - startTime);
                double[] errors = mergedSketch.getErrors();
            }

            System.out.format("k=%f Train/merge/query %g/%g/%g\n",
                    sParam,
                    trainTime / (1.0e9 * numTrials),
                    mergeTime / (1.0e9 * numTrials),
                    queryTime / (1.0e9 * numTrials));
        }
    }
}
