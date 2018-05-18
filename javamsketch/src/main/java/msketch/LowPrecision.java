package msketch;

import java.util.Random;

public class LowPrecision {
    private int bits;
    public int bitsForExponent;
    public int bitsForSignificand;
    private int bitsForSign = 1;
    public int minExponent; // base power of 2

    private Random rand;

    public double min;
    public double max;
    public double logMin;
    public double logMax;
    public double[] powerSums;
    public double[] logSums;
    public double[] totalSums;

    private static int maxItersWithoutImprovement = 50;

    public LowPrecision(int bits) {
        this.bits = bits;
        this.rand = new Random();
    }

    /* Kind of a hack but whatever. */
    public void encode(double min, double max, double logMin, double logMax, double[] totalSums) {
        encode(min, max, logMin, logMax, totalSums, new double[]{});
        this.totalSums = this.powerSums;
    }

    public void encode(double min, double max, double logMin, double logMax, double[] powerSums, double[] logSums) {
        double[] minmax = getMinMax(min, max, logMin, logMax, powerSums, logSums);
        double minVal = minmax[0];
        double maxVal = minmax[1];

        setParameters(minVal, maxVal);

        this.min = encodeValueRandomized(min);
        this.max = encodeValueRandomized(max);
        this.logMin = encodeValueRandomized(logMin);
        this.logMax = encodeValueRandomized(logMax);
        this.powerSums = new double[powerSums.length];
        for (int i = 0; i < powerSums.length; i++) {
            this.powerSums[i] = encodeValueRandomized(powerSums[i]);
        }
        this.logSums = new double[logSums.length];
        for (int i = 0; i < logSums.length; i++) {
            this.logSums[i] = encodeValueRandomized(logSums[i]);
        }
    }

    private void setParameters(double minVal, double maxVal) {
        final int maxPower2 = (int)Math.ceil(log(maxVal, 2));
        int minPower2 = (int)Math.floor(log(minVal, 2));

        // We successively lower minPower2 until we have the right tradeoff between number of bits
        // for the exponent and significand, based on minimizing the error of the minVal.
        int numExponentBits = numExponentBitsForRange(minPower2, maxPower2);

        // Not enough bits to span the logarithmic range
        if (numExponentBits > bits - bitsForSign) {
            bitsForExponent = bits - bitsForSign;
            bitsForSignificand = 0;
            minExponent = minPower2;
            return;
        }

        int numSignificandBits = bits - numExponentBits - bitsForSign;
        double encodedMinval = encodeValue(minVal, numExponentBits, numSignificandBits, minPower2, false);
        double minvalError = Math.abs(encodedMinval - minVal);
        int bestMinPower2 = minPower2;
        int numItersWithoutImprovement = 0;
        while (true) {
            minPower2--;
            numExponentBits = numExponentBitsForRange(minPower2, maxPower2);
            if (numExponentBits > bits - bitsForSign) {
                break;
            }
            numSignificandBits = bits - numExponentBits - bitsForSign;
            encodedMinval = encodeValue(minVal, numExponentBits, numSignificandBits, minPower2, false);
            double newMinvalError = Math.abs(encodedMinval - minVal);
            if (newMinvalError < minvalError) {
                bestMinPower2 = minPower2;
                numItersWithoutImprovement = 0;
            } else if (newMinvalError == minvalError) {
                numItersWithoutImprovement++;
                if (numItersWithoutImprovement == maxItersWithoutImprovement) {
                    break;
                }
            } else {
                break;
            }
            minvalError = newMinvalError;
        }

        bitsForExponent = numExponentBitsForRange(bestMinPower2, maxPower2);
        bitsForSignificand = bits - bitsForExponent - bitsForSign;
        minExponent = lowestExponent(maxPower2, bitsForExponent);
    }

    private double encodeValueDeterministic(double val) {
        return encodeValue(val, bitsForExponent, bitsForSignificand, minExponent, false);
    }

    private double encodeValueRandomized(double val) {
        return encodeValue(val, bitsForExponent, bitsForSignificand, minExponent, true);
    }

    private double encodeValue(double val, int numExponentBits, int numSignificandBits, int minPower2, boolean randomized) {
        int exponent = (int)Math.ceil(log(Math.abs(val) / (int)Math.pow(2, numSignificandBits+1), 2));
        if (exponent < minPower2) {
            // shouldn't happen
            exponent = minPower2;
        }
        double eps = Math.pow(2, exponent);

        if (randomized) {
            double probability = val / eps - (int) (val / eps);
            if (rand.nextDouble() < probability) {
                return Math.ceil(val / eps) * eps;
            } else {
                return Math.floor(val / eps) * eps;
            }
        } else {
            return Math.round(val / eps) * eps;
        }
    }

    private int numExponentBitsForRange(int minPower2, int maxPower2) {
        return (int)Math.ceil(log(maxPower2 - minPower2 + 1, 2));
    }

    private int lowestExponent(int maxPower2, int numBitsForExponent) {
        return maxPower2 - (int) Math.pow(2, numBitsForExponent) + 1;
    }

    /* Returns the minimum and maximum magnitude of all the statistics. */
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

    private double log(double val, int base) {
        return Math.log(val) / Math.log(base);
    }
}
