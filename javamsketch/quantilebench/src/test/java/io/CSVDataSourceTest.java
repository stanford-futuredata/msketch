package io;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CSVDataSourceTest {
    @Test
    public void testLoadCSV() throws Exception {
        CSVDataSource s = new CSVDataSource("src/test/resources/test.csv", 0);
        s.setHasHeader(true);
        double[] col = s.get();
        assertEquals(1.0, col[0], 0.0);
    }

    @Test
    public void testLoadCSV2() throws Exception {
        SimpleCSVDataSource s = new SimpleCSVDataSource("src/test/resources/test.csv", 1);
        s.setHasHeader(true);
        double[] col = s.get();
        assertEquals(9.0, col[1], 0.0);
    }
}