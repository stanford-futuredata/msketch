import io.CSVOutput;
import macrobase.ThresholdAlerter;
import sketches.CMomentSketch;
import sketches.QuantileSketch;
import sketches.YahooSketch;

import java.io.*;
import java.util.*;

public class SlidingWindowBench {
    String testName;
    String inputFile;
    double sizeParamYahoo;
    double sizeParamMSketch;
    int windowSize;
    double quantile;
    double threshold;
    double maxWarmupTime;
    double maxTrialTime;

    ArrayList<double[]> panes;

    public SlidingWindowBench(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        inputFile = conf.get("inputFile");
        sizeParamYahoo = conf.get("sizeParamYahoo");
        sizeParamMSketch = conf.get("sizeParamMSketch");
        windowSize = conf.get("windowSize");
        quantile = conf.get("quantile");
        threshold = conf.get("threshold");
        maxWarmupTime = conf.get("maxWarmupTime");
        maxTrialTime = conf.get("maxTrialTime");
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        SlidingWindowBench bench = new SlidingWindowBench(confFile);
        bench.run();
    }

    public void run() throws Exception {
        panes = getPanes();

        List<Map<String, String>> results = new ArrayList<Map<String, String>>();

        results.add(runCascade(new boolean[]{false, false, false}));
        results.add(runCascade(new boolean[]{true, false, false}));
        results.add(runCascade(new boolean[]{true, true, false}));
        results.add(runCascade(new boolean[]{true, true, true}));

        results.add(runYahoo());

        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(true);
        output.writeAllResults(results, testName);
    }

    public Map<String, String> runCascade(boolean[] cascadeStages) throws Exception {
        List<Boolean> alerts = new ArrayList<>();
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            System.gc();
            alerts = cascadeTrial(cascadeStages);
        }
        System.out.println("Warmup finished");
        int trialsDone = 0;
        long start = System.nanoTime();
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            System.gc();
            alerts = cascadeTrial(cascadeStages);
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;
        int numAlerts = Collections.frequency(alerts, true);

        System.out.format("Avg runtime: %f\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("Num alerts: %d\n\n", numAlerts);

        Map<String, String> result = new HashMap<String, String>();
        result.put("simple", String.format("%b", cascadeStages[0]));
        result.put("markov", String.format("%b", cascadeStages[1]));
        result.put("racz", String.format("%b", cascadeStages[2]));
        result.put("avg_runtime", String.format("%f", timeElapsed / (1.e9 * trialsDone)));
        return result;
    }

    private List<Boolean> cascadeTrial(boolean[] cascadeStages) {
        CMomentSketch sketch = new CMomentSketch(1e-9);
        sketch.setCalcError(false);
        sketch.setSizeParam(sizeParamMSketch);
        sketch.initialize();

        List<Boolean> alerts = new ArrayList<>();
        ThresholdAlerter alerter = new ThresholdAlerter(quantile, threshold, true);
        alerter.setCascadeStages(cascadeStages);

        // Create the initial window
        List<CMomentSketch> paneSketches = new ArrayList<>();
        Queue<Double> paneMins = new LinkedList<>();
        Queue<Double> paneMaxs = new LinkedList<>();
        Queue<Double> paneLogMins = new LinkedList<>();
        Queue<Double> paneLogMaxs = new LinkedList<>();
        int curPane = 0;
        for (; curPane < windowSize; curPane++) {
            sketch.add(panes.get(curPane));

            CMomentSketch paneSketch = new CMomentSketch(1e-9);
            paneSketch.setCalcError(false);
            paneSketch.setSizeParam(sizeParamMSketch);
            paneSketch.initialize();
            paneSketch.add(panes.get(curPane));
            paneSketches.add(paneSketch);
            paneMins.add(paneSketch.getMin());
            paneMaxs.add(paneSketch.getMax());
            paneLogMins.add(paneSketch.getLogMin());
            paneLogMaxs.add(paneSketch.getLogMax());
        }

        alerts.add(alerter.checkAlert(sketch));

        // Begin sliding window
        int firstPaneSketchIdx = 0;
        for (; curPane < panes.size(); curPane++) {
            CMomentSketch paneSketch = paneSketches.get(firstPaneSketchIdx);

            // Remove oldest pane
            paneMins.remove();
            paneMaxs.remove();
            paneLogMins.remove();
            paneLogMaxs.remove();
            double[] windowTotalSums = sketch.getTotalSums();
            double[] paneTotalSums = paneSketch.getTotalSums();
            for (int i = 0; i < windowTotalSums.length; i++) {
                windowTotalSums[i] -= paneTotalSums[i];
            }

            // Add new pane (by writing directly into the paneSketch object)
            paneSketch.reset();
            paneSketch.add(panes.get(curPane));
            paneMins.add(paneSketch.getMin());
            paneMaxs.add(paneSketch.getMax());
            paneLogMins.add(paneSketch.getLogMin());
            paneLogMaxs.add(paneSketch.getLogMax());
            paneTotalSums = paneSketch.getTotalSums();
            for (int i = 0; i < windowTotalSums.length; i++) {
                windowTotalSums[i] += paneTotalSums[i];
            }

            sketch.setMin(Collections.min(paneMins));
            sketch.setMax(Collections.max(paneMaxs));
            sketch.setLogMin(Collections.min(paneLogMins));
            sketch.setLogMax(Collections.max(paneLogMaxs));

            firstPaneSketchIdx = (firstPaneSketchIdx + 1) % windowSize;

            alerts.add(alerter.checkAlert(sketch));
        }

        return alerts;
    }

    public Map<String, String> runYahoo() throws Exception {
        List<Boolean> alerts = new ArrayList<>();
        long warmupStart = System.nanoTime();
        while (System.nanoTime() - warmupStart < maxWarmupTime * 1.e9) {
            System.gc();
            alerts = yahooTrial();
        }
        System.out.println("Warmup finished");
        int trialsDone = 0;
        long start = System.nanoTime();
        while (System.nanoTime() - start < maxTrialTime * 1.e9) {
            System.gc();
            alerts = yahooTrial();
            trialsDone++;
        }
        long timeElapsed = System.nanoTime() - start;
        int numAlerts = Collections.frequency(alerts, true);

        System.out.format("Avg runtime: %f\n", timeElapsed / (1.e9 * trialsDone));
        System.out.format("Num alerts: %d\n\n", numAlerts);

        Map<String, String> result = new HashMap<String, String>();
        result.put("avg_runtime", String.format("%f", timeElapsed / (1.e9 * trialsDone)));
        return result;
    }

    private List<Boolean> yahooTrial() throws Exception {
        List<Boolean> alerts = new ArrayList<>();

        // Create the initial window
        ArrayList<YahooSketch> paneSketches = new ArrayList<>();
        int curPane = 0;
        for (; curPane < windowSize; curPane++) {
            YahooSketch paneSketch = new YahooSketch();
            paneSketch.setCalcError(false);
            paneSketch.setSizeParam(sizeParamYahoo);
            paneSketch.initialize();
            paneSketch.add(panes.get(curPane));
            paneSketches.add(paneSketch);
        }

        // Create window sketch
        YahooSketch sketch = new YahooSketch();
        sketch.setCalcError(false);
        sketch.setSizeParam(sizeParamYahoo);
        sketch.initialize();
        sketch.mergeYahoo(paneSketches);

        alerts.add(sketch.getQuantile(quantile) >= threshold);

        // Begin sliding window
        int firstPaneSketchIdx = 0;
        for (; curPane < panes.size(); curPane++) {
            YahooSketch paneSketch = paneSketches.get(firstPaneSketchIdx);

            // Remove oldest pane
            paneSketch.initialize();

            // Add new pane (by writing directly into the paneSketch object)
            paneSketch.add(panes.get(curPane));

            // Update window sketch
            sketch.initialize();
            sketch.mergeYahoo(paneSketches);

            firstPaneSketchIdx = (firstPaneSketchIdx + 1) % windowSize;

            alerts.add(sketch.getQuantile(quantile) >= threshold);
        }

        return alerts;
    }

    public ArrayList<double[]> getPanes() throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(inputFile));
        bf.readLine();
        ArrayList<double[]> vals = new ArrayList<>();
        while (true) {
            String curLine = bf.readLine();
            if (curLine == null) {
                break;
            }
            int startIdx = curLine.indexOf('[') + 1;
            int endIdx = curLine.indexOf(']');
            String[] items = curLine.substring(startIdx, endIdx).split(",");
            double[] values = new double[items.length];
            for (int i = 0; i < items.length; i++) {
                values[i] = Double.parseDouble(items[i]);
            }
            vals.add(values);
        }

        return vals;
    }
}
