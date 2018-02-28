#!/usr/bin/env bash
java -Xmx8g -Xms8g -cp target/java-msketch-1.0-SNAPSHOT.jar:$(cat cp.txt) LogMomentsLesion $@
