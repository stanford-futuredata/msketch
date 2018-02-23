package io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class CSVDataSource implements DataSource {
    public String fileName;
    public int column;
    public int limit = Integer.MAX_VALUE;
    public boolean hasHeader = true;

    public CSVDataSource(String fileName, int column) {
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
    public double[] get() throws IOException {
        Reader in = new FileReader(fileName);
        CSVFormat format = CSVFormat.RFC4180;
        if (hasHeader) {
            format = format.withFirstRecordAsHeader();
        }
        Iterable<CSVRecord> records = format.parse(in);

        ArrayList<Double> results = new ArrayList<>();
        int rowCount = 0;
        for (CSVRecord row : records) {
            results.add(Double.parseDouble(row.get(column)));
            rowCount++;
            if (rowCount >= limit) {
                break;
            }
        }
        in.close();

        double[] resultArr = new double[results.size()];
        for (int i = 0; i < resultArr.length; i++) {
            resultArr[i] = results.get(i);
        }

        return resultArr;
    }
}
