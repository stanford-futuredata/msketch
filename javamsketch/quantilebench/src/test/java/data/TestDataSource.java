package data;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TestDataSource {
    public static double[] getUniform(double start, double end, int length) {
        double[] vals = new double[length];
        double stepSize = (end - start) / (length-1);
        for (int i = 0; i < length; i++) {
            vals[i] = start + i * stepSize;
        }
        return vals;
    }

    public static double[] getUniform(int n) {
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) {
            vals[i] = i;
        }
        return vals;
    }

    public static double[] getGaussian(Random r, int n) {
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) {
            vals[i] = r.nextGaussian();
        }
        return vals;
    }

    public static void shuffleArray(double[] ar)
    {
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            double a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }
}
