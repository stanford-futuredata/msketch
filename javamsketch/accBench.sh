#!/usr/bin/env bash
java -Xmx10g -Xms10g -cp quantilebench/target/quantile-bench-1.0-SNAPSHOT.jar:$(cat quantilebench/cp.txt) AccuracyBench $@
