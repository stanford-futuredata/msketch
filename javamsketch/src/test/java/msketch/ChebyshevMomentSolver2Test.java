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
//        double[] powerSums = {100000.0, 3744499.4281647149, 1233517017.009681};
        double[] logSums = {
//                100000.0, 51168.203748162814, 1208754.3928379791, 569972.49776283279, 28015772.359430037, -25767762.12031116, 1043864877.9561398
//                100000.0, 51168.203748162814, 1208754.3928379791, 569972.49776283279, 28015772.359430037, -25767762.12031116, 1043864877.9561398, -3677056205.4909549, 58152902615.413704
                100000.0, 51168.203748162814, 1208754.3928379791, 569972.49776283279, 28015772.359430037, -25767762.12031116, 1043864877.9561398, -3677056205.4909549, 58152902615.413704, -367850180269.72314, 4396966720066.8203
        };

        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                range[0], range[1], powerSums,
                logRange[0], logRange[1], logSums
        );
        solver.setVerbose(true);
        double[] l0 = new double[powerSums.length+logSums.length-1];
        solver.solve(l0, 1e-9);
        double[] ps = {.1, .5, .9, .99};
        double[] qs = solver.estimateQuantiles(ps);
        System.out.println(Arrays.toString(qs));
    }

    @Test
    public void testStandard() {
        double[] logRange = {-12.96899970738978,8.312480128125637};
        double[] logSums = {
                100000.0, 51168.203748162814, 1208754.3928379791, 569972.49776283279, 28015772.359430037, -25767762.12031116, 1043864877.9561398, -3677056205.4909549, 58152902615.413704, -367850180269.72314, 4396966720066.8203
        };
        ChebyshevMomentSolver solver = ChebyshevMomentSolver.fromPowerSums(
            logRange[0], logRange[1], logSums
        );
        solver.setVerbose(true);
        solver.solve(1e-8);
        double[] ps = {.1, .5, .9, .99};
        double[] qs = solver.estimateQuantiles(ps, logRange[0], logRange[1]);
        System.out.println(Arrays.toString(qs));
    }

}