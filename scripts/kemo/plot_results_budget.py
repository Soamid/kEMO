import pathlib
import re
from typing import NamedTuple, Tuple

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

RESULTS_DIR_PATH: pathlib.Path = pathlib.Path('../results_budget')
PLOTS_DIR_PATH: pathlib.Path = pathlib.Path('../plots')

BENCHMARK_PATTERN = re.compile(r'(?P<benchmark>[\w|\-]+\d)_metrics_(?P<attempt>\d+)\.csv')

Colour = Tuple[float, float, float]


def percentage_rgb(r: float, g: float, b: float) -> Colour:
    max_rgb = 255.0
    return r / max_rgb, g / max_rgb, b / max_rgb


AGH_RED_COLOUR = percentage_rgb(167.0, 25.0, 4.0)
AGH_GREEN_COLOUR = percentage_rgb(0.0, 105.0, 60.0)
AGH_GREY_COLOUR = percentage_rgb(30.0, 30.0, 30.0)

COLOURS_FOR_OPTIMIZERS = {
    '': AGH_GREY_COLOUR,
    'PHGS': AGH_RED_COLOUR,
    'HOPSO': AGH_GREEN_COLOUR,
}

STANDARD_ALPHA = 0.2
STANDARD_LINE_WIDTH = 1.5


class BenchmarkDescriptor(NamedTuple):
    benchmark: str
    attempt: int


def identify_benchmark(metrics_path: pathlib.Path) -> BenchmarkDescriptor:
    groups = BENCHMARK_PATTERN.match(metrics_path.name)
    return BenchmarkDescriptor(groups['benchmark'], int(groups['attempt']))


def read_benchmark_attempt(metrics_path, optimizer_descriptor):
    benchmark_descriptor = identify_benchmark(metrics_path)
    metrics = pd.read_csv(metrics_path, engine='python', sep=', ')

    preprocessed_rows = []
    budget_levels = [x for x in np.arange(2000, 300001, 1000)]
    rows = [dict(x[1]) for x in metrics.iterrows()]
    level_pointer = 0
    row_pointer = 0
    while level_pointer < len(budget_levels):
        while row_pointer < len(rows) and rows[row_pointer]['NFE'] < budget_levels[level_pointer]:
            row_pointer += 1
        preprocessed_row = dict(rows[row_pointer - 1])
        preprocessed_row['NFE-Level'] = budget_levels[level_pointer]
        preprocessed_rows.append(preprocessed_row)
        level_pointer += 1
    preprocessed_metrics = pd.DataFrame(preprocessed_rows)
    metrics = preprocessed_metrics

    metrics['Benchmark'] = benchmark_descriptor.benchmark
    metrics['Optimizer'] = optimizer_descriptor.optimizer
    metrics['Meta-Optimizer'] = optimizer_descriptor.meta_optimizer
    metrics['Measurement'] = np.arange(metrics.shape[0])
    metrics['Attempt'] = benchmark_descriptor.attempt
    metrics = metrics.set_index(['Benchmark', 'Optimizer', 'Meta-Optimizer', 'Measurement', 'Attempt'])
    return metrics


class OptimizerDescriptor(NamedTuple):
    meta_optimizer: str
    optimizer: str


def identify_optimizer(directory_path: pathlib.Path) -> OptimizerDescriptor:
    splitted = directory_path.name.split('+')
    if len(splitted) == 1:
        return OptimizerDescriptor('', splitted[0])
    elif len(splitted) == 2:
        return OptimizerDescriptor(*splitted)
    else:
        raise ValueError("Incorrect algorithm directory path!")


def read_optimizer(optimizer_path: pathlib.Path) -> pd.DataFrame:
    optimizer_results = pd.DataFrame()
    optimizer_descriptor = identify_optimizer(optimizer_path)
    metrics_paths = optimizer_path.glob('*_metrics_*.csv')
    for metrics_path in metrics_paths:
        optimizer_results = pd.concat([optimizer_results, read_benchmark_attempt(metrics_path, optimizer_descriptor)])
    return optimizer_results


def read_results() -> pd.DataFrame:
    results = pd.DataFrame()
    optimizer_paths = [path for path in RESULTS_DIR_PATH.iterdir() if path.is_dir()]
    for optimizer_path in optimizer_paths:
        results = pd.concat([results, read_optimizer(optimizer_path)])
    return results


def sanitize_results(results: pd.DataFrame) -> pd.DataFrame:
    processed = results.groupby(
        ['Benchmark', 'Optimizer', 'Meta-Optimizer', 'Measurement']
    ).agg(
        {column: ['mean', 'std'] for column in results.columns}
    )

    for column in results.columns:
        processed[column, 'upper_limit'] = processed[column, 'mean'] + processed[column, 'std']
        processed[column, 'lower_limit'] = processed[column, 'mean'] - processed[column, 'std']
        processed[column, 'lower_limit'] = processed[column, 'lower_limit'].mask(
            processed[column, 'lower_limit'] < 0.0, 0.0
        )
    return processed


def power10(x: float) -> float:
    return 10 ** x


def main() -> None:
    results = read_results()
    columns = [column for column in results.columns if (column != 'NFE' and column != 'NFE-Level')]
    sanitized_results = sanitize_results(results)
    for benchmark in sanitized_results.index.get_level_values('Benchmark').unique():
        for optimizer in sanitized_results.index.get_level_values('Optimizer').unique():
            for column in columns:
                max_value = sanitized_results.loc[benchmark][column, 'upper_limit'].max()
                min_value = sanitized_results.loc[benchmark][column, 'lower_limit'].min()
                fig = plt.figure()
                ax = fig.add_subplot(1, 1, 1)
                try:
                    for meta_optimizer, colour in COLOURS_FOR_OPTIMIZERS.items():
                        series = sanitized_results.loc[benchmark, optimizer, meta_optimizer]
                        ax.fill_between(
                            series['NFE-Level', 'mean'], series[column, 'lower_limit'], series[column, 'upper_limit'],
                            alpha=STANDARD_ALPHA,
                            color=colour
                        )
                        ax.plot(
                            series['NFE-Level', 'mean'], series[column, 'mean'],
                            '-',
                            color=colour,
                            label=f"{meta_optimizer}+{optimizer}" if meta_optimizer != '' else optimizer,
                            linewidth=STANDARD_LINE_WIDTH,
                        )
                        ax.plot(
                            series['NFE-Level', 'mean'], series[column, 'lower_limit'],
                            ':',
                            color=colour,
                            alpha=1.0 - STANDARD_ALPHA,
                            linewidth=STANDARD_LINE_WIDTH*0.5,
                        )
                        ax.plot(
                            series['NFE-Level', 'mean'], series[column, 'upper_limit'],
                            ':',
                            color=colour,
                            alpha=1.0 - STANDARD_ALPHA,
                            linewidth=STANDARD_LINE_WIDTH*0.5,
                        )
                    ax.set_xlabel("NFE")
                    if column == 'Hypervolume':
                        rounded_max_value = np.round(max_value, 1)
                        if rounded_max_value < max_value:
                            rounded_max_value += 0.1
                        ax.set_yscale('function', functions=(power10, np.log10))
                        ax.set(xlim=(0, 300000), ylim=(0.0, rounded_max_value))
                        ax.set_ylabel("hypervolume")
                        ax.legend(loc='lower right', frameon=False)
                    elif column == 'InvertedGenerationalDistance':
                        ax.set_yscale('log')
                        effective_min = 0.1
                        while effective_min > min_value and effective_min > 0.0001:
                            effective_min /= 10.0
                        effective_max = 10.0
                        while effective_max < max_value and effective_max < 10.0:
                            effective_max *= 10.0
                        ax.set(xlim=(0, 300000), ylim=(effective_min, effective_max))
                        ax.set_ylabel("inverted generational distance")
                        ax.legend(loc='upper right', frameon=False)
                    elif column == 'Spacing':
                        ax.set_yscale('log')
                        effective_min = 0.1
                        while effective_min > min_value and effective_min > 0.0001:
                            effective_min /= 10.0
                        effective_max = 0.1
                        while effective_max < max_value and effective_max < 1.0:
                            effective_max += 0.1
                        ax.set(xlim=(0, 300000), ylim=(effective_min, effective_max))
                        ax.set_ylabel("spacing")
                        ax.legend(loc='upper right', frameon=False)
                    plt.tight_layout()
                    plt.savefig(PLOTS_DIR_PATH / f'{benchmark}_{optimizer}_{column}.pdf')
                except (KeyError, ValueError) as _:
                    pass
                plt.close()


if __name__ == '__main__':
    main()
