package io;

import data.TestDataSource;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class SeqDataGrouperTest {
    @Test
    public void testSimple() {
        double[] data = TestDataSource.getUniform(-2, 3, 1000);
        SeqDataGrouper g = new SeqDataGrouper(90);
        ArrayList<double[]> cells = g.group(data);
        assertEquals(12, cells.size());
        assertEquals(10, cells.get(11).length);
        assertEquals(3.0, cells.get(11)[9], 0.0);
    }

}