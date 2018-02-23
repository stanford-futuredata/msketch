package msketch.data;

public class OccupancyData extends MomentData {
    private final double min = 412.75;
    private final double max = 2076.5;
    private final double logMin = 6.022842082800238;
    private final double logMax = 7.638439063070808;
    private final double[] powerSums = {
            20560.0, 14197775.359523809, 11795382081.900866, 11920150330935.938,
            14243310876969824.0, 1.9248869180998238e+19, 2.8335762132634282e+22, 4.431640701816542e+25,
            7.2509584910158713e+28, 1.2290081330972746e+32};
    private final double[] logSums = {
            20560.0, 132778.81355561133, 860423.75561972987, 5595528.9043199299,
            36524059.16578535, 239323723.78677931, 1574401576.9855776, 10399585507.478024,
            68980678228.532593, 459495821550.01648};

    @Override
    public double[] getPowerSums() {
        return powerSums;
    }

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public double getMax() {
        return max;
    }

    @Override
    public double[] getLogSums() {
        return logSums;
    }

    @Override
    public double getLogMin() {
        return logMin;
    }

    @Override
    public double getLogMax() {
        return logMax;
    }
}
