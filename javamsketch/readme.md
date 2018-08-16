# Moments Sketch Code

This repository contains an implementation of the moments sketch 
maximum entropy solver, as well as benchmark implementations of a variety
of other quantile summaries.

The core moment sketch solver code is in the msolver package, while
the benchmarking and reference implementations are in the quantilebench
package.

### Compiling and Running

First, generate an up-to-date classpath. This only needs to be done once.
```
mvn install
./genCP.sh
```

Install the solver code:
```
cd msolver && mvn install && cd ..
```

Build the benchmark code:
```
cd quantilebench && mvn package && cd ..
```

Run an example accuracy benchmark with the moments sketch:
```
./accBench confs/test_gauss_2.json
```
Results for the example workload are saved in `results/test_gauss_2.csv`

Merge benchmarks can be run using `./mergeBench`

### Important configuration parameters

- fileName: input csv file
- columnIdx: which column contains the metric of interest
- methods: list of quantile summaries one would like to benchmark, 
parameterized by sketch size
    - `"cmoments": [11.0]` means to run a moments sketch 
    with up to order 10 moments


### Moments Sketch Internals

To call the java code for the solver directly, if you have collected the
statistics you can use the `ChebyshevMomentSolver2` class. See the
`ChebyshevMomentSolver2Test` for example usage.