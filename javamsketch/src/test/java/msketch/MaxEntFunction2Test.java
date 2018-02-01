package msketch;

import org.junit.Test;

import static org.junit.Assert.*;

public class MaxEntFunction2Test {
    @Test
    public void testSimple() {
        double[] aCoeffs = {0, -1};
        double[] bCoeffs = {0, 1};
        MaxEntFunction2 f = new MaxEntFunction2(
                true,
                aCoeffs,
                bCoeffs,
                5.05,
                4.95,
                2.220446049250313e-16,
                2.302585092994046
        );
        assertEquals(3.73002156214, f.zerothMoment(1e-8), 1e-8);

        double[][] pairwiseMoments = f.getPairwiseMoments(1e-8);
        assertEquals(3.73002156214, pairwiseMoments[0][0], 1e-8);
        assertEquals(0.4078218803, pairwiseMoments[1][3], 1e-8);
        assertEquals(1.12095675177, pairwiseMoments[1][1], 1e-8);
    }

}