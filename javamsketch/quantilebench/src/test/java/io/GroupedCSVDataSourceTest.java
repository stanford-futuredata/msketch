package io;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class GroupedCSVDataSourceTest {
    @Test
    public void testLoadCSV() throws Exception {
        GroupedCSVDataSource s = new GroupedCSVDataSource("src/test/resources/grouped.csv");
        s.setHasHeader(true);
        ArrayList<double[]> groups = s.get();
        assertEquals(2, groups.size());
        assertArrayEquals(new double[]{2.0}, groups.get(0), 0.0);
        assertArrayEquals(new double[]{3.0, 4.0}, groups.get(1), 0.0);
    }
}