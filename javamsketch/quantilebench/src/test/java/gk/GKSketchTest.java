package gk;

import data.TestDataSource;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.spark.sql.catalyst.util.QuantileSummaries;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class GKSketchTest {
    @Test
    public void testSimple() {
        double[] xs = TestDataSource.getUniform(100);
        GKSketch s = new GKSketch(.05);
        s.add(xs);
        s.add(xs);
//        System.out.println(s.toString());
//        System.out.println(s.quantile(.5));
    }

    @Test
    public void mergeTest() {
        ArrayList<double[]> cellData = new ArrayList<>();
        int numCells = 50;
        int nPerCell = 1000;
        double[] totalData = new double[numCells * nPerCell];
        int totalIdx = 0;

        double eps = 0.01;
        GKSketch globalSummary = new GKSketch(eps);
        for (int cellIdx = 0; cellIdx < numCells; cellIdx++) {
            double[] curData = new double[nPerCell];
            for (int i = 0; i < nPerCell; i++) {
                double curValue;
                if (cellIdx % 2 == 0) {
                    curValue = cellIdx * 200 + i;
                } else {
                    curValue = i*2000 + cellIdx;
                }
                curData[i] = curValue;
                totalData[totalIdx] = curValue;
                totalIdx++;
            }
            globalSummary.add(curData);
            cellData.add(curData);
        }

        Percentile p = new Percentile();
        double targetP = .45;
        double q = p.evaluate(totalData, targetP*100);
        double qll = p.evaluate(totalData, (targetP-2*eps)*100);
        double ql = p.evaluate(totalData, (targetP-eps)*100);
        double qh = p.evaluate(totalData, (targetP+eps)*100);
        double qhh = p.evaluate(totalData, (targetP+2*eps)*100);
        System.out.println(qll+","+ql+",["+q+"],"+qh+","+qhh);

//        System.out.println("Global Size: "+globalSummary.getTuples().size());
//        System.out.println("Global Quantile: "+globalSummary.quantile(targetP));

        ArrayList<GKSketch> summaries = new ArrayList<>();
        for (double[] curData : cellData) {
            GKSketch s = new GKSketch(eps);
            s.add(curData);
            summaries.add(s);
        }

        GKSketch merged = new GKSketch(eps);
        for (GKSketch toMerge : summaries) {
            merged.merge(toMerge);
        }

//        System.out.println("Merged Size: "+merged.getTuples().size());
//        System.out.println("Merged Quantile: "+merged.quantile(targetP));
//        System.out.println(merged.toString());
    }

}