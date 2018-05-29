#!/usr/bin/env bash
java -Xmx50g -Xms50g -cp quantilebench/target/quantile-bench-1.0-SNAPSHOT.jar:$(cat quantilebench/cp.txt) \
ParallelMergeBench $@
