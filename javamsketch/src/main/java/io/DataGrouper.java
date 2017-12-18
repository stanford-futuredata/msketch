package io;

import java.util.ArrayList;

public class DataGrouper {
    private double[] data;
    public DataGrouper(double[] data) {
        this.data = data;
    }

    public ArrayList<double[]> groupSequentially(int cellSize) {
        int n = data.length;
        int numCells = (int)Math.ceil(n/cellSize);
        ArrayList<double[]> cells = new ArrayList<>(numCells);
        for (int i = 0; i < numCells; i++) {
            int startIdx = i*cellSize;
            int endIdx = Math.min(
                    (i+1)*cellSize, n
            );
            double[] curCell = new double[endIdx - startIdx];
            for (int j = 0; j < curCell.length; j++) {
                curCell[j] = startIdx+j;
            }
            cells.add(curCell);
        }
        return cells;
    }
}
