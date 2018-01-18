package msketch.data;

public class RetailQuantityData extends MomentData{
    public static final double[] powerSums = {
            541909.0,
            5176450.0,
            25822261422.0,
            -745215241598.0,
            1.4680778526547758e+20,
            1.9195859734681039e+19,
            8.9883645056629936e+29,
            2.2528516771732062e+28,
            5.5448289206614458e+39,
            5.3232475666358104e+36,
            3.4438200308790149e+49,
            9.962234249902733e+44,
            2.1525281069658358e+59,
            1.7085535290287837e+53,
            1.3533396252305566e+69
    };

    @Override
    public double[] getPowerSums() {
        return powerSums;
    }

    @Override
    public double getMin() {
        return -80995.0;
    }

    @Override
    public double getMax() {
        return 80995.0;
    }
}
