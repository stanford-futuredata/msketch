#!/usr/bin/env bash
java -Xmx10g -Xms10g -cp target/java-msketch-1.0-SNAPSHOT.jar:$(cat cp.txt) AccuracyBench $@
