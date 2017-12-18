#!/usr/bin/env bash
java -cp target/java-msketch-1.0-SNAPSHOT.jar:$(cat cp.txt) MSketchBench
