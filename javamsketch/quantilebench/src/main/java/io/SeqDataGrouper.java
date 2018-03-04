package io;

import java.util.ArrayList;

public class SeqDataGrouper implements DataGrouper {
    private int cellSize;

    public SeqDataGrouper(int cellSize) {
        this.cellSize = cellSize;
    }

    @Override
    public ArrayList<double[]> group(double[] data) {
        int n = data.length;
        int numCells = (int)Math.ceil(n*1.0/cellSize);
        ArrayList<double[]> cells = new ArrayList<>(numCells);
        for (int i = 0; i < numCells; i++) {
            int startIdx = i*cellSize;
            int endIdx = Math.min(
                    (i+1)*cellSize, n
            );
            double[] curCell = new double[endIdx - startIdx];
            for (int j = 0; j < curCell.length; j++) {
                curCell[j] = data[startIdx+j];
            }
            cells.add(curCell);
        }
        return cells;
    }
}
