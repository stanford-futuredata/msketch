package msketch;

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
        double[] l0 = new double[powerSums.length+logSums.length-1];
        solver.solve(l0, 1e-9);
        double[] ps = {.1, .5, .9, .99};
        double[] qs = solver.estimateQuantiles(ps);
        assertEquals(3.0, qs[1], 1.0);
        assertEquals(476.0, qs[3], 20.0);
    }


    @Test
    public void testOccupancy() {
        double[] range = {412.75,2076.5};
        double[] logRange = {6.022842082800238,7.638439063070808};
        double[] powerSums = {20560.0, 14197775.359523809, 11795382081.900866, 11920150330935.938, 14243310876969824.0, 1.9248869180998238e+19, 2.8335762132634282e+22};
//        double[] powerSums = {20560.0, 14197775.359523809, 11795382081.900866, 11920150330935.938, 14243310876969824.0, 1.9248869180998238e+19, 2.8335762132634282e+22, 4.431640701816542e+25, 7.2509584910158713e+28, 1.2290081330972746e+32};
        double[] logSums = {20560.0, 132778.81355561133, 860423.75561972987, 5595528.9043199299, 36524059.16578535, 239323723.78677931, 1574401576.9855776};
//        double[] logSums = {20560.0, 132778.81355561133, 860423.75561972987, 5595528.9043199299, 36524059.16578535, 239323723.78677931, 1574401576.9855776, 10399585507.478024, 68980678228.532593, 459495821550.01648};

        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                range[0], range[1], powerSums,
                logRange[0], logRange[1], logSums
        );
        double[] l0 = new double[powerSums.length+logSums.length-1];
        solver.solve(l0, 1e-9);
        double[] ps = {.1, .5, .9, .99};
        double[] qs = solver.estimateQuantiles(ps);
        assertEquals(565.0, qs[1], 5.0);

    }
}