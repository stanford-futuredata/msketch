import matplotlib
matplotlib.use('SVG')
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np


class ReportGen:
    def __init__(
            self,
            raw_data,
            results_df,
            base_dir="results/",
            filename="report.html"
    ):
        self.raw_data = raw_data
        self.results_df = results_df
        self.base_dir = base_dir
        self.filename = "report.html"

    def gen_images(self):
        plt.figure()
        _ = plt.hist(self.raw_data, bins=120, log=True)
        plt.grid(axis="both")
        plt.title("Data Histogram")
        plt.tight_layout()
        plt.savefig(self.base_dir+"hist.svg")
        plt.close()

        df = self.results_df
        percentiles = sorted(df["percentile"].unique())
        self.n_charts = len(percentiles)
        for i in range(len(percentiles)):
            p = percentiles[i]
            cdf = df[
                (np.abs(df["percentile"] - p) < 1e-10) &
                (df["method"] != "true")
                ]
            tvalue = df[
                (np.abs(df["percentile"] - p) < 1e-10) &
                (df["method"] == "true")
                ]["estimate"].iloc[0]
            methods = []
            values = []
            for r in cdf.itertuples():
                methods.append("{}@{}".format(r.method, r.size))
                values.append(r.estimate)
            n = len(cdf)
            x_pos = np.arange(n)

            fig = plt.figure(figsize=(4, 4))
            ax = fig.gca()
            ax.set_title("P{:.3f}".format(p))
            ax.scatter(
                x_pos,
                values,
                color="C0"
            )
            ax.plot(
                x_pos, np.repeat(tvalue, len(x_pos)),
                label="true value",
                color="C3"
            )
            ax.grid(axis='x')
            ax.set_xticks(x_pos)
            _ = ax.set_xticklabels(methods, rotation="vertical")
            if i == 0:
                ax.set_ylabel("value")
            ax.set_xlabel("method @ size")
            fig.tight_layout()
            fig.savefig(self.base_dir+"report_{}.svg".format(i))
            plt.close()

    def gen_report(self):
        self.gen_images()
        html_str = """
<!doctype html>
<html lang="en">
  <head>
    <title>MSketch Report</title>
    <link rel="stylesheet" href="report.css">
  </head>
  <body>
    <h1>Original Distribution</h1>
    <img 
      src="hist.svg" 
      alt="Histogram of Distribution"/>
    <h1>Quantile Accuracy</h1>
    {chart_strs}
    <h1>Raw Results Table</h1>
    {table_str}
  </body>
</html>
""".format(
            chart_strs = "\n".join([self.gen_chart_str(i) for i in range(self.n_charts)]),
            table_str = self.results_df.to_html()
        )
        with open(self.base_dir+self.filename, "w") as f:
            f.write(html_str)

    def gen_chart_str(self, i, last=False):
        return """
    <img 
      src="report_{}.svg" 
      alt="Quantile Accuracies"
      /> 
""".format(
            i
        )

