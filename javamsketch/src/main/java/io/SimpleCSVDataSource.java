package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class SimpleCSVDataSource implements DataSource{
    public String fileName;
    public int column;
    public int limit = Integer.MAX_VALUE;
    public boolean hasHeader = true;

    public SimpleCSVDataSource(String fileName, int column) {
        this.fileName = fileName;
        this.column = column;
    }

    public void setHasHeader(boolean flag) {
        this.hasHeader = flag;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public double[] get() throws Exception {
        BufferedReader bf = new BufferedReader(new FileReader(fileName));
        if (hasHeader) {
            bf.readLine();
        }
        ArrayList<Double> vals = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String curLine = bf.readLine();
            if (curLine == null) {
                break;
            }
            int colCount = 0;
            int startIdx = 0;
            int nextIdx = -1;
            while (colCount <= column) {
                startIdx = nextIdx+1;
                nextIdx = curLine.indexOf(',', startIdx);
                colCount++;
                if (nextIdx == -1) {
                    nextIdx = curLine.length();
                    break;
                }
            }
            vals.add(Double.parseDouble(curLine.substring(startIdx, nextIdx)));
        }

        double[] uVals = new double[vals.size()];
        for (int i = 0; i < uVals.length; i++) {
            uVals[i] = vals.get(i);
        }
        return uVals;
    }
}
