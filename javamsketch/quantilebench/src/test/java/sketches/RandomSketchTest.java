package sketches;

import data.TestDataSource;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class RandomSketchTest {
    @Test
    public void testUniformNoMerge() throws Exception {
        int numTrials = 1;
        int n = 20000;
        double[] data = TestDataSource.getUniform(n + 1);
//        TestDataSource.shuffleArray(data);
        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);
        double[] sizeParams = {5.0, 10.0, 20.0, 50.0, 100.0};
        for (double sizeParam : sizeParams) {
            double[] averageQs = new double[ps.size()];
            double[] averageError = new double[ps.size()];
            for (int i = 0; i < numTrials; i++) {
                RandomSketch sketch = new RandomSketch();
                sketch.setSizeParam(sizeParam);
                sketch.initialize();
                sketch.add(data);

                double[] qs = sketch.getQuantiles(ps);

                for (int j = 0; j < qs.length; j++) {
                    averageQs[j] += qs[j];
                    averageError[j] += Math.abs(qs[j] - expectedQs[j]);
                }
            }
            for (int j = 0; j < averageQs.length; j++) {
                averageQs[j] /= numTrials;
                averageError[j] /= numTrials;
            }
            assertArrayEquals(expectedQs, averageQs, n / sizeParam);
        }
    }

    @Test
    public void testUniformWithMerge() throws Exception {
        int numTrials = 1;
        double[] sizeParams = {5.0, 10.0, 20.0, 50.0, 100.0};
        int n = 20000;
        double[] data = TestDataSource.getUniform(n+1);
//        TestDataSource.shuffleArray(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);

        DataGrouper grouper = new SeqDataGrouper(200);
        ArrayList<double[]> cellData = grouper.group(data);
        for (double sizeParam : sizeParams) {
            double[] averageQs = new double[ps.size()];
            double[] averageError = new double[ps.size()];
            for (int i = 0; i < numTrials; i++) {
                QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                        () -> {
                            RandomSketch newSketch = new RandomSketch();
                            newSketch.setSizeParam(sizeParam);
                            return newSketch;
                        },
                        cellData
                );
                double[] qs2 = mergedSketch.getQuantiles(ps);
                for (int j = 0; j < qs2.length; j++) {
                    averageQs[j] += qs2[j];
                    averageError[j] += Math.abs(qs2[j] - expectedQs[j]);
                }
            }
            for (int j = 0; j < averageQs.length; j++) {
                averageQs[j] /= numTrials;
                averageError[j] /= numTrials;
            }
            assertArrayEquals(expectedQs, averageQs, n / sizeParam);
        }
    }

    @Test
    public void testSmallCellMerge() throws Exception {
        int numTrials = 1;
        double[] sizeParams = {50.0};
        int n = 20000;
        double[] data = TestDataSource.getUniform(n+1);
//        TestDataSource.shuffleArray(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);

        DataGrouper grouper = new SeqDataGrouper(50);
        ArrayList<double[]> cellData = grouper.group(data);
        for (double sizeParam : sizeParams) {
            double[] averageQs = new double[ps.size()];
            double[] averageError = new double[ps.size()];
            for (int i = 0; i < numTrials; i++) {
                QuantileSketch mergedSketch = QuantileUtil.trainAndMerge(
                        () -> {
                            RandomSketch newSketch = new RandomSketch();
                            newSketch.setSizeParam(sizeParam);
                            return newSketch;
                        },
                        cellData
                );
                double[] qs2 = mergedSketch.getQuantiles(ps);
                for (int j = 0; j < qs2.length; j++) {
                    averageQs[j] += qs2[j];
                    averageError[j] += Math.abs(qs2[j] - expectedQs[j]);
                }
            }
            for (int j = 0; j < averageQs.length; j++) {
                averageQs[j] /= numTrials;
                averageError[j] /= numTrials;
            }
            assertArrayEquals(expectedQs, averageQs, n / sizeParam);
        }
    }

    @Test
    public void testMergeBuffers() throws Exception {
        ArrayList<Double> bufferOne = new ArrayList<>(Arrays.asList(1., 2., 4., 6., 9.));
        ArrayList<Double> bufferTwo = new ArrayList<>(Arrays.asList(0., 3., 5., 7., 8.));
        ArrayList<Double> target = new ArrayList<>();

        RandomSketch sketch = new RandomSketch();
        sketch.mergeBuffers(bufferOne, bufferTwo, target);

        assertTrue(target.get(0) == 0 || target.get(0) == 1);

        if (target.get(0) == 0) {
            assertEquals(new ArrayList<>(Arrays.asList(0., 2., 4., 6., 8.)), target);
        } else {
            assertEquals(new ArrayList<>(Arrays.asList(1., 3., 5., 7., 9.)), target);
        }
    }
}