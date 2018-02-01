package sketches;

import java.io.IOException;

public class SketchLoader {
    public static QuantileSketch load(
            String sketchName
    ) throws IOException {
        if (sketchName.startsWith("moment")) {
            return new MomentSketch(1e-9);
        } else if (sketchName.startsWith("tdigest")) {
            return new TDigestSketch();
        } else if (sketchName.startsWith("yahoo")) {
            return new YahooSketch();
        } else if (sketchName.startsWith("spark_gk")) {
            return new SparkGKSketch();
        } else if (sketchName.startsWith("sampling")) {
            return new SamplingSketch();
        } else if (sketchName.startsWith("histogram")) {
            return new HistogramSketch();
        } else if (sketchName.startsWith("hmoment")) {
            return new HybridMomentSketch(1e-9);
        } else if (sketchName.startsWith("bothmoment")) {
            HybridMomentSketch m = new HybridMomentSketch(1e-9);
            m.setTryBoth(true);
            return m;
        }
        throw new IOException("Invalid Sketch");
    }
}
