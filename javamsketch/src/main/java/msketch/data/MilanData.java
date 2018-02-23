package msketch.data;

// 2013-11-01_09
public class MilanData extends MomentData {
    private final double min = 2.3314976995293306e-06;
    private final double max = 7936.2653798841566;
    private final double logMin = -12.968999707389781;
    private final double logMax = 8.9791980884111684;
    private final double[] powerSums = {
            24308139.0,
            895708136.64296079,
            279144233273.02856,
            238436915163396.25,
            3.7913867410827194e+17,
            9.1024129423546044e+20,
            2.9023293489053704e+24,
            1.1404072441825097e+28,
            5.3011600688342121e+31,
            2.8378031270494773e+35,
            1.7054473749994686e+39,
            1.1204250526571611e+43,
            7.8484315633113414e+46
    };
    private final double[] logSums = {
            24308139.0,
            12420049.706757378,
            293245315.42992228,
            131886982.50259101,
            6781335850.7644348,
            -6571640034.5625038,
            253168798811.84521,
            -919129472417.11865,
            14293827594309.152,
            -93264716269145.203,
            1113140641657524.2,
            -9711395910128630.0,
            1.085213731386595e+17
    };

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

