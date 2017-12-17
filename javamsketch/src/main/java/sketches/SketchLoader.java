package sketches;

import java.io.IOException;

public class SketchLoader {
    public static QuantileSketch load(
            String sketchName
    ) throws IOException {
        if (sketchName.contains("moment")) {
            return new MomentSketch(1e-9);
        } else if (sketchName.contains("tdigest")) {
            return new TDigestSketch();
        } else if (sketchName.contains("yahoo")) {
            return new YahooSketch();
        } else if (sketchName.contains("spark_gk")) {
            return new SparkGKSketch();
        } else if (sketchName.contains("sampling")) {
            return new SamplingSketch();
        }
        throw new IOException("Invalid Sketch");
    }
}
