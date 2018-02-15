package msketch;

import msketch.data.MomentData;
import msketch.data.OccupancyData;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ChebyshevMomentSolver2Test {
    @Test
    public void testMilan() {
        double[] logRange = {-12.96899970738978,8.312480128125637};
        double[] range = {2.3314976995293306e-06,4074.4055108548055};

        double[] powerSums = {100000.0, 3744499.4281647149, 1233517017.009681, 1185616080484.6663, 2139875342176598.2};
        double[] logSums = {
                100000.0, 51168.203748162814, 1208754.3928379791, 569972.49776283279,
                28015772.359430037, -25767762.12031116, 1043864877.9561398, -3677056205.4909549,
                58152902615.413704, -367850180269.72314, 4396966720066.8203
        };

        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                range[0], range[1], powerSums,
                logRange[0], logRange[1], logSums
        );
        solver.solve(1e-9);
        double[] ps = {.1, .5, .9, .99};
        double[] qs = solver.estimateQuantiles(ps);
        assertEquals(3.0, qs[1], 1.0);
        assertEquals(476.0, qs[3], 20.0);
    }

    @Test
    public void testDruid() {
        double[] range = {2.331497699529331E-6,7936.265379884158};
        double[] logRange = new double[2];
        logRange[0] = Math.log(range[0]);
        logRange[1] = Math.log(range[1]);

        double[] powerSums = {1.2814767E7, 4.6605082350179493E8, 1.309887742552026E11, 1.0548055486726956E14, 1.74335364401727808E17, 4.676320625451096E20, 1.713914827616506E24, 7.732420316935072E27};
        double[] logSums = {1.2814767E7, 8398321.384180075, 1.5347415740933093E8, 9.186643473957856E7, 3.4859819726620092E9, -2.9439576163248196E9, 1.2790650864340628E11, -4.578233220122189E11};

        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                range[0], range[1], powerSums,
                logRange[0], logRange[1], logSums
        );
        solver.solve(1e-9);
        double[] ps = {.1, .5, .9, .99};
        double[] qs = solver.estimateQuantiles(ps);
    }

    @Test
    public void testOccupancy() {
        MomentData data = new OccupancyData();
        double[] range = {data.getMin(), data.getMax()};
        double[] logRange = {data.getLogMin(), data.getLogMax()};
        double[] powerSums = data.getPowerSums(9);
        double[] logSums = data.getLogSums(9);

        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                range[0], range[1], powerSums,
                logRange[0], logRange[1], logSums,
                5
        );
        solver.solve(1e-9);
        double[] ps = {.1, .5, .9, .99};
        double[] qs = solver.estimateQuantiles(ps);
        assertEquals(565.0, qs[1], 5.0);
    }
}