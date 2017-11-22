import argparse
import numpy as np
import pandas as pd

import msketch.mcalc as mc
import msketch.msketch as ms
import tdigest

parser = argparse.ArgumentParser(description='Moment Quantile Sketch')
parser.add_argument("file", type=str)
parser.add_argument("column", type=str)
parser.add_argument("--percentiles", type=float, nargs="+", default=[.1,.5,.9])

import reportgen

def main():
    args = parser.parse_args()
    data = pd.read_csv(args.file)[args.column]
    print("Loaded {} rows".format(len(data)))
    name = args.file.split("/")[-1]
    results = []
    results += true_q(data, args.percentiles, name)
    for k in [3, 5, 7, 9]:
        results += run_msketch(data, args.percentiles, name, k=k)
    print("Calculated MSketches")
    for delta in [1.0, 0.5, 0.2, 0.1]:
        results += run_tdigest(data, args.percentiles, name, delta=delta, k=25)
    print("Calculated T-Digests")
    r_df = pd.DataFrame(results).sort_values(
        ["data", "percentile", "method"]
    )[["percentile", "method", "estimate", "size", "param"]]
    r_df.reset_index(drop=True)
    r_df.to_csv("results.csv", index=False)
    print("Wrote results to results.csv")

    rg = reportgen.ReportGen(
        data,
        r_df,
        "report.html"
    )
    rg.gen_report()
    print("Generated report to report.html")

def true_q(data, ps, name="data"):
    results = []
    for p in ps:
        results.append({
            "data": name,
            "method": "true",
            "size": None,
            "param": None,
            "percentile": p,
            "estimate": np.percentile(data, p*100),
        })
    return results


def run_msketch(data, ps, name="data", k=5):
    s = ms.MSketch(k=k)
    s.sketch(data)
    c = mc.MCalc(s)
    c.calc()
    results = []
    for p in ps:
        results.append({
            "data": name,
            "method": "msketch",
            "size": (k+2)*8,
            "param": k,
            "percentile": p,
            "estimate": c.quantile(p),
        })
    return results


def run_tdigest(data, ps, name="data", k=25, delta=0.1):
    td = tdigest.TDigest(delta=delta, K=k)
    td.batch_update(data)
    results = []
    for p in ps:
        results.append({
            "data": name,
            "method": "tdigest",
            "size": len(td.C)*2*8,
            "param": delta,
            "percentile": p,
            "estimate": td.percentile(p*100),
        })
    return results


if __name__ == "__main__":
    main()