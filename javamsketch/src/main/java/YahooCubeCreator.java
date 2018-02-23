import sketches.YahooSketch;

import java.io.*;
import java.util.ArrayList;

public class YahooCubeCreator {
    static String inputFile;
    static String outputFile;
    static double sizeParam;

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        inputFile = conf.get("inputFile");
        outputFile = conf.get("outputFile");
        sizeParam = conf.get("sizeParam");

        YahooCubeCreator creator = new YahooCubeCreator();
        ArrayList<double[]> vals = creator.get();
        ArrayList<byte[]> sketches = new ArrayList<>();
        for (double[] values : vals) {
            YahooSketch sketch = new YahooSketch();
            sketch.setSizeParam(sizeParam);
            sketch.initialize();
            sketch.add(values);
            sketches.add(sketch.sketch.toByteArray());
        }

        FileOutputStream fos = new FileOutputStream(outputFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(sketches);
        oos.close();
    }

    public ArrayList<double[]> get() throws IOException {
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
