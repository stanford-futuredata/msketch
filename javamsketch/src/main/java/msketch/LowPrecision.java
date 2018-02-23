package msketch;

public class LowPrecision {
    private int bits;

    public LowPrecision(int bits) {
        this.bits = bits;
    }

    public void encode(double min, double max, double logMin, double logMax, double[] powerSums, double[] logSums) {
        double[] minmax = getMinMax(min, max, logMin, logMax, powerSums, logSums);
        double min = minmax[0];
        double max = minmax[1];


    }

    public void decode() {

    }

    private void setParameters(double min, double max) {

    }

    private double[] getMinMax(double min, double max, double logMin, double logMax, double[] powerSums, double[] logSums) {
        double[] minmax = new double[2];

        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;
        for (double val : new double[]{min, max, logMin, logMax}) {
            double absVal = Math.abs(val);
            if (absVal < minVal) minVal = absVal;
            if (absVal > maxVal) maxVal = absVal;
        }
        for (double val : powerSums) {
            double absVal = Math.abs(val);
            if (absVal < minVal) minVal = absVal;
            if (absVal > maxVal) maxVal = absVal;
        }
        for (double val : logSums) {
            double absVal = Math.abs(val);
            if (absVal < minVal) minVal = absVal;
            if (absVal > maxVal) maxVal = absVal;
        }

        minmax[0] = minVal;
        minmax[1] = maxVal;
        return minmax;
    }
}
