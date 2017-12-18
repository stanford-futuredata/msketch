package io;

import data.TestDataSource;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class DataGrouperTest {
    @Test
    public void testSimple() {
        double[] data = TestDataSource.getUniform(1000);
        DataGrouper g = new DataGrouper(data);
        ArrayList<double[]> cells = g.groupSequentially(90);
        assertEquals(12, cells.size());
        assertEquals(10, cells.get(11).length);
    }

}