package data;

public class TestDataSource {
    public static double[] getUniform(int n) {
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) {
            vals[i] = i;
        }
        return vals;
    }
}
