package sketches;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public interface QuantileSketch {
    String getName();
    int getSize();
    double getSizeParam();

    void setSizeParam(double sizeParam);
    void setCalcError(boolean flag);
    default void setVerbose(boolean flag) {return;}
    void initialize();

    void add(double[] data);
    QuantileSketch merge(List<QuantileSketch> sketches, int startIndex, int endIndex);
    default QuantileSketch merge(List<QuantileSketch> sketches) { return merge(sketches, 0, sketches.size()); };
    default QuantileSketch parallelMerge(ArrayList<QuantileSketch> sketches, int numThreads) {
        int numSketches = sketches.size();
        final CountDownLatch doneSignal = new CountDownLatch(numThreads);
        QuantileSketch[] mergedSketches = new QuantileSketch[numThreads];
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            final int curThreadNum = threadNum;
            final int startIndex = (numSketches * threadNum) / numThreads;
            final int endIndex = (numSketches * (threadNum + 1)) / numThreads;
            Runnable ParallelMergeRunnable = () -> {
                QuantileSketch mergedSketch;
                try {
                    mergedSketch = SketchLoader.load(this.getName());
                } catch (IOException e) {
                    mergedSketch = null;
                }
                mergedSketch.setSizeParam(this.getSizeParam());
                mergedSketch.initialize();
                mergedSketches[curThreadNum] = mergedSketch.merge(sketches, startIndex, endIndex);
                doneSignal.countDown();
            };
            Thread ParallelMergeThread = new Thread(ParallelMergeRunnable);
            ParallelMergeThread.start();
        }
        try {
            doneSignal.await();
        } catch (InterruptedException ex) {ex.printStackTrace();}

        return merge(Arrays.asList(mergedSketches), 0, mergedSketches.length);
    }

    double[] getQuantiles(List<Double> ps) throws Exception;
    double[] getErrors();
}
