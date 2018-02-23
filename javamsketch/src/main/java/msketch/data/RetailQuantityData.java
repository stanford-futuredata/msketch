package msketch.data;

public class RetailQuantityData extends MomentData{
    public static final double[] powerSums = {
            531285.0, 5660981.0, 13127647799.0, 943385744203541.0,
            7.3401290527335825e+19, 5.7374634895753686e+24, 4.4941878460622702e+29, 3.5267533869172936e+34,
            2.7724146420399472e+39, 2.1831198887574202e+44, 1.7219100191391005e+49
    };

    public static final double[] logSums = {
            531285.0, 733706.08385088702, 1803377.9327264477, 5313341.6192785092,
            18079041.058607381, 69790609.377532199, 302287816.89519858, 1456494992.1214058,
            7765406800.8637161, 45788298431.40226, 299590569873.87885
    };

    @Override
    public double[] getPowerSums() {
        return powerSums;
    }

    @Override
    public double[] getLogSums() {
        return logSums;
    }

    @Override
    public double getMin() {
        return 1.0;
    }

    @Override
    public double getMax() {
        return 80995.0;
    }

    @Override
    public double getLogMin() {
        return 0.0;
    }

    @Override
    public double getLogMax() {
        return Math.log(80995.0);
    }
}
