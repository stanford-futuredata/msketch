package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class GroupedCSVDataSource {
    public String fileName;
    public int limit = Integer.MAX_VALUE;
    public boolean hasHeader = true;

    public GroupedCSVDataSource(String fileName) {
        this.fileName = fileName;
    }

    public void setHasHeader(boolean flag) {
        this.hasHeader = flag;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public ArrayList<double[]> get() throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(fileName));
        if (hasHeader) {
            bf.readLine();
        }
        ArrayList<double[]> vals = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String curLine = bf.readLine();
            if (curLine == null) {
                break;
            }
            String[] rawGroup = curLine.substring(curLine.indexOf('[')+1, curLine.lastIndexOf(']')).split(",");
            double[] group = new double[rawGroup.length];
            for (int j = 0; j < group.length; j++) {
                group[j] = Double.parseDouble(rawGroup[j]);
            }
            vals.add(group);
        }

        return vals;
    }
}
