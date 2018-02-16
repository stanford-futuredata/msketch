package msketch;

import org.junit.Test;

import static org.junit.Assert.*;

public class SolveBasisSelectorTest {
    @Test
    public void testMilan() {
        double[] linscales = {-1.9949008094893061,10.974098897900475,3968.1326911078277,3968.13268877633};
        SolveBasisSelector sel = new SolveBasisSelector();
        sel.select(
            false, 7, 7,
                linscales[0], linscales[1], linscales[2], linscales[3]
        );
        assertEquals(sel.getKb(), 2);
        assertEquals(sel.getKa(), 7);
    }

}