package macrobase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MarkovBoundTest {
    int numMoments = 4;
    double[] cutoffs = {20.0, 50.0, 80.0};

    @Test
    public void testSimple() {
        double[] data = new double[100];
        double[] quantiles = new double[cutoffs.length];
        for (int i = 0; i < 100; i++) {
            data[i] = i;
            for (int j = 0; j < cutoffs.length; j++) {
                if (data[i] < cutoffs[j]) {
                    quantiles[j] += 1.0;
                }
            }
        }
        for (int j = 0; j < quantiles.length; j++) {
            quantiles[j] /= data.length;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double logMin = Double.MAX_VALUE;
        double logMax = -Double.MAX_VALUE;
        double[] powerSums = new double[numMoments];
        double[] logSums = new double[numMoments];
        for (int i = 0; i < data.length; i++) {
            min = Math.min(min, data[i]);
            max = Math.max(max, data[i]);
            double val = 1.0;
            powerSums[0] += val;
            for (int j = 1; j < powerSums.length; j++) {
                val *= data[i];
                powerSums[j] += val;
            }

            if (data[i] > 0) {
                double logData = Math.log(data[i]);
                logMin = Math.min(logMin, logData);
                logMax = Math.max(logMax, logData);
                double logVal = 1.0;
                logSums[0] += logVal;
                for (int j = 1; j < logSums.length; j++) {
                    logVal *= logData;
                    logSums[j] += logVal;
                }
            }
        }

        for (int i = 0; i < cutoffs.length; i++) {
            double cutoff = cutoffs[i];
            double[] bounds = MarkovBound.getOutlierRateBounds(cutoff, min, max, logMin, logMax, powerSums, logSums, true);
            double[] standardBounds = MarkovBound.getOutlierRateBounds(cutoff, min, max, logMin, logMax, powerSums, logSums, false);
            assertTrue(bounds[0] <= 1.0 - quantiles[i]);
            assertTrue(bounds[1] >= 1.0 - quantiles[i]);
            assertTrue(bounds[0] <= standardBounds[0]);
            assertTrue(bounds[1] >= standardBounds[1]);
        }
    }
}
