First, generate and up-to-date classpath
```
./genCP.sh
```

To run the benchmark:

```
mvn package
./accBench.sh confs/shuttle_acc.json
```