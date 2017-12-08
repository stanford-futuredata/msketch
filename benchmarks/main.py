import pandas as pd
import numpy as np
import os
from benchmarks.csketch import CSketch
import benchmarks.csolver as csolver

def shuttle():
    df = pd.read_csv("sampledata/shuttle.csv", usecols=["0"])
    data = df["0"]
    n = len(data)
    sdata = np.sort(data)
    print(len(data))
    s = CSketch(8)
    s.train(data)
    lambdas,steps = csolver.solve(s.m, 1e-10)
    f = csolver.gen_pdf(lambdas)
    ps = [0.001, 0.01, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.99, 0.999]
    results = []
    for p in ps:
        q = csolver.quantile(f, p, tol=1e-9)
        q = ((q+1)/2)*(s.max - s.min) + s.min
        q_p = np.searchsorted(sdata, q)*1.0 / n

        q_true = np.percentile(data, 100*p)

        results.append({
            "p": p,
            "q_est": q,
            "p_q": q_p,
            "q_true": q_true,
            "q_delta": abs(q_p - p)
        })

    df = pd.DataFrame(results)
    df.to_csv("results.csv", index=False)


def main():
    print("Benchmark Script")
    shuttle()


if __name__ == "__main__":
    main()