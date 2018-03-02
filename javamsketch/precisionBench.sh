#!/usr/bin/env bash
java -Xmx32g -Xms32g -cp target/java-msketch-1.0-SNAPSHOT.jar:$(cat cp.txt) PrecisionBench $@
