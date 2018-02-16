package msketch;

import msketch.chebyshev.ChebyshevPolynomial;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SolveBasisSelectorTest {
    @Test
    public void testMilan() {
        double[] linscales = {-1.9949008094893061,10.974098897900475,3968.1326911078277,3968.13268877633};
        double aMin = linscales[0] - linscales[1];
        double aMax = linscales[0] + linscales[1];
        double bMin = linscales[2] - linscales[3];
        double bMax = linscales[2] + linscales[3];
        SolveBasisSelector sel = new SolveBasisSelector();
        sel.select(
            false, new double[7], new double[7],
                aMin, aMax, bMin, bMax
        );
        assertEquals(2, sel.getKb());
        assertEquals(7, sel.getKa());
    }
}