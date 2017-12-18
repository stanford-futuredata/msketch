package data;

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
}
