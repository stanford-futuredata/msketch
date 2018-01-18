package msketch.data;

import java.util.Arrays;

public abstract class MomentData {
    abstract public double[] getPowerSums();
    abstract public double getMin();
    abstract public double getMax();

    public double[] getPowerSums(int k) {
        return Arrays.copyOf(getPowerSums(), k);
    }
}
