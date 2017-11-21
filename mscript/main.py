import matplotlib
matplotlib.use('SVG')
import argparse
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

import msketch.mcalc as mc
import msketch.msketch as ms

parser = argparse.ArgumentParser(description='Moment Quantile Sketch')
parser.add_argument("file", type=str)
parser.add_argument("column", type=str)
parser.add_argument("--percentiles", type=float, nargs="+", default=[.1,.5,.9])

def main():
    args = parser.parse_args()
    data = pd.read_csv(args.file)[args.column]
    name = args.file.split("/")[-1]
    results = []
    results += msketch(data, args.percentiles, name)
    results += true_q(data, args.percentiles, name)
    r_df = pd.DataFrame(results).sort_values(
        ["data", "percentile", "method"]
    )[["percentile", "method", "estimate", "size"]]
    print(name)
    print(r_df)

def true_q(data, ps, name="data"):
    results = []
    for p in ps:
        results.append({
            "data": name,
            "method": "true",
            "size": None,
            "percentile": p,
            "estimate": np.percentile(data, p*100),
        })
    return results

def msketch(data, ps, name="data"):
    k = 5
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
            "percentile": p,
            "estimate": c.quantile(p),
        })
    return results

if __name__ == "__main__":
    main()