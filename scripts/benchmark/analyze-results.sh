#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: analyze-results.sh [--results DIR] [--baseline FILE] [--report FILE] [--json FILE]
                         [--self-test]

Parses Android Macrobenchmark outputs, compares them with configured performance
budgets, and emits markdown + JSON summaries suitable for CI pipelines.

Options:
  --results DIR   Directory containing Macrobenchmark additional test outputs.
  --baseline FILE Baseline thresholds JSON (see config/testing/tooling).
  --report FILE   Markdown summary path to write (directories created).
  --json FILE     Machine-readable JSON summary path to write.
  --self-test     Run against bundled fixtures to validate the parser.
  -h, --help      Show this message and exit.
USAGE
}

die() {
  echo "[macrobenchmark] $*" >&2
  exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RESULTS_DIR=""
BASELINE_PATH=""
REPORT_PATH=""
JSON_PATH=""
SELF_TEST=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --results)
      RESULTS_DIR="$2"
      shift 2
      ;;
    --baseline)
      BASELINE_PATH="$2"
      shift 2
      ;;
    --report)
      REPORT_PATH="$2"
      shift 2
      ;;
    --json)
      JSON_PATH="$2"
      shift 2
      ;;
    --self-test)
      SELF_TEST=1
      shift 1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage
      die "Unknown argument: $1"
      ;;
  esac
done

if ! command -v python3 >/dev/null 2>&1; then
die "python3 is required to parse macrobenchmark outputs"
fi

if [[ ${SELF_TEST} -eq 1 ]]; then
  FIXTURE_ROOT="${SCRIPT_DIR}/fixtures"
  RESULTS_DIR="${FIXTURE_ROOT}/sample-results"
  BASELINE_PATH="${FIXTURE_ROOT}/baseline.json"
  REPORT_PATH="${PROJECT_ROOT}/macrobenchmark/build/reports/macrobenchmark/fixture-summary.md"
  JSON_PATH="${PROJECT_ROOT}/macrobenchmark/build/reports/macrobenchmark/fixture-summary.json"
  echo "[macrobenchmark] Running self-test with fixtures at ${FIXTURE_ROOT}" >&2
fi

: "${RESULTS_DIR:=${PROJECT_ROOT}/macrobenchmark/build/outputs/connected_android_test_additional_output/connected}"
: "${BASELINE_PATH:=${PROJECT_ROOT}/config/testing/tooling/macrobenchmark-baselines.json}"
: "${REPORT_PATH:=${PROJECT_ROOT}/macrobenchmark/build/reports/macrobenchmark/summary.md}"
: "${JSON_PATH:=${PROJECT_ROOT}/macrobenchmark/build/reports/macrobenchmark/summary.json}"

if [[ ! -d "${RESULTS_DIR}" ]]; then
die "Results directory not found: ${RESULTS_DIR}"
fi

if [[ ! -f "${BASELINE_PATH}" ]]; then
die "Baseline file not found: ${BASELINE_PATH}"
fi

export RESULTS_DIR
export BASELINE_PATH
export REPORT_PATH
export JSON_PATH

python3 <<'PY'
import json
import math
import os
import statistics
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

results_dir = Path(os.environ['RESULTS_DIR']).resolve()
baseline_path = Path(os.environ['BASELINE_PATH']).resolve()
report_path = Path(os.environ['REPORT_PATH']).resolve()
json_path = Path(os.environ['JSON_PATH']).resolve()
report_path.parent.mkdir(parents=True, exist_ok=True)
json_path.parent.mkdir(parents=True, exist_ok=True)

def read_json(path: Path):
    with path.open('r', encoding='utf-8') as handle:
        return json.load(handle)


def normalise_test_id(name: str) -> str:
    if '[' in name:
        name = name.split('[', 1)[0]
    return name.strip()


def collect_numbers(payload, prefix=""):
    if isinstance(payload, dict):
        for key, value in payload.items():
            new_prefix = f"{prefix}.{key}" if prefix else key
            yield from collect_numbers(value, new_prefix)
    elif isinstance(payload, (int, float)):
        yield prefix, float(payload)
    elif isinstance(payload, list):
        # Ignore raw samples; aggregated stats are sufficient.
        return


def summarise_text_samples(path: Path):
    raw = path.read_text(encoding='utf-8')
    tokens = [token.strip() for token in raw.replace('\n', ',').split(',') if token.strip()]
    values = []
    for token in tokens:
        try:
            values.append(float(token))
        except ValueError:
            continue
    if not values:
        return {}
    values.sort()

    def percentile(p: float) -> float:
        if not values:
            return math.nan
        if len(values) == 1:
            return values[0]
        rank = (len(values) - 1) * p
        lower = math.floor(rank)
        upper = math.ceil(rank)
        if lower == upper:
            return values[int(rank)]
        return values[lower] + (values[upper] - values[lower]) * (rank - lower)

    summary = {
        'count': len(values),
        'min': min(values),
        'max': max(values),
        'mean': statistics.mean(values),
        'median': statistics.median(values),
        'p90': percentile(0.90),
        'p95': percentile(0.95),
    }
    return summary


def load_results(root: Path):
    collected = defaultdict(lambda: defaultdict(dict))
    if not root.exists():
        return collected

    for path in root.rglob('*.json'):
        metric_name = path.stem
        try:
            relative = path.relative_to(root)
        except ValueError:
            continue
        parts = relative.parts
        if not parts:
            continue
        test_id = normalise_test_id(parts[0])
        data = read_json(path)
        values = {}
        for key, value in collect_numbers(data):
            if not key:
                continue
            short_key = key.split('.')[-1]
            # Prefer descriptive keys; retain both full path and short form.
            values.setdefault(key, value)
            values.setdefault(short_key, value)
        collected[test_id][metric_name].update(values)

    for path in root.rglob('*.txt'):
        metric_name = f"Outputs.{path.stem}"
        try:
            relative = path.relative_to(root)
        except ValueError:
            continue
        parts = relative.parts
        if not parts:
            continue
        test_id = normalise_test_id(parts[0])
        summary = summarise_text_samples(path)
        if summary:
            collected[test_id][metric_name].update(summary)
    return collected


def deep_copy_thresholds(source):
    copy = {}
    for metric_name, stats in source.items():
        copy[metric_name] = {stat: dict(limit) for stat, limit in stats.items()}
    return copy


@dataclass
class Evaluation:
    benchmark: str
    metric: str
    stat: str
    value: float
    limit_type: str
    limit_value: float
    status: str
    message: str


def format_limit(limit: dict) -> tuple[str, float]:
    if 'max' in limit:
        return 'max', float(limit['max'])
    if 'min' in limit:
        return 'min', float(limit['min'])
    raise ValueError('Unknown limit format: {limit}')


baseline = read_json(baseline_path)
default_thresholds = deep_copy_thresholds(baseline.get('defaults', {}))
benchmarks = baseline.get('benchmarks', {})
results = load_results(results_dir)

evaluations: list[Evaluation] = []
failures: list[str] = []
missing_results: list[str] = []
missing_baselines: list[str] = []

for benchmark_id, threshold_entry in benchmarks.items():
    merged_thresholds = deep_copy_thresholds(default_thresholds)
    for metric_name, stats in threshold_entry.get('metrics', {}).items():
        merged_thresholds.setdefault(metric_name, {})
        merged_thresholds[metric_name].update({k: dict(v) if isinstance(v, dict) else {'max': v} for k, v in stats.items()})
    benchmark_results = results.get(benchmark_id)
    if not benchmark_results:
        missing_results.append(benchmark_id)
        continue

    explicit_metrics = set(threshold_entry.get('metrics', {}).keys())

    for metric_name, stats in merged_thresholds.items():
        metric_results = benchmark_results.get(metric_name)
        if not metric_results:
            if metric_name in default_thresholds and metric_name not in explicit_metrics:
                # Skip optional defaults when the metric is not recorded for this benchmark.
                continue
            failures.append(f"{benchmark_id}: missing metric '{metric_name}' in results")
            continue
        for stat_name, limit in stats.items():
            try:
                limit_type, limit_value = format_limit(limit)
            except ValueError:
                failures.append(f"{benchmark_id}: metric '{metric_name}' stat '{stat_name}' has unsupported limit {limit}")
                continue
            value = metric_results.get(stat_name)
            if value is None:
                failures.append(
                    f"{benchmark_id}: metric '{metric_name}' missing stat '{stat_name}' in results"
                )
                continue
            status = 'PASS'
            message = ''
            if limit_type == 'max' and value > limit_value:
                status = 'FAIL'
                message = f"expected ≤ {limit_value}, observed {value}"
                failures.append(
                    f"{benchmark_id}/{metric_name}.{stat_name}: {message}"
                )
            elif limit_type == 'min' and value < limit_value:
                status = 'FAIL'
                message = f"expected ≥ {limit_value}, observed {value}"
                failures.append(
                    f"{benchmark_id}/{metric_name}.{stat_name}: {message}"
                )
            evaluations.append(
                Evaluation(
                    benchmark=benchmark_id,
                    metric=metric_name,
                    stat=stat_name,
                    value=value,
                    limit_type=limit_type,
                    limit_value=limit_value,
                    status=status,
                    message=message,
                )
            )

for benchmark_id in results.keys():
    if benchmark_id not in benchmarks:
        missing_baselines.append(benchmark_id)

summary_rows = []
for evaluation in evaluations:
    comparator = '≤' if evaluation.limit_type == 'max' else '≥'
    summary_rows.append(
        {
            'benchmark': evaluation.benchmark,
            'metric': evaluation.metric,
            'stat': evaluation.stat,
            'value': evaluation.value,
            'comparator': comparator,
            'limit': evaluation.limit_value,
            'status': evaluation.status,
            'message': evaluation.message,
        }
    )

status = 'PASS'
if failures or missing_results or missing_baselines:
    status = 'FAIL'

summary = {
    'status': status,
    'resultsDir': str(results_dir),
    'baseline': str(baseline_path),
    'metrics': summary_rows,
    'missingResults': missing_results,
    'missingBaselines': missing_baselines,
    'failures': failures,
}

with json_path.open('w', encoding='utf-8') as handle:
    json.dump(summary, handle, indent=2, sort_keys=False)

header = ["Benchmark", "Metric", "Stat", "Value", "Limit", "Status"]
lines = ["# Macrobenchmark Performance Summary", ""]
lines.append(f"- Results directory: `{results_dir}`")
lines.append(f"- Baseline: `{baseline_path}`")
if missing_results:
    lines.append(f"- Missing result suites: {', '.join(sorted(missing_results))}")
if missing_baselines:
    lines.append(f"- Missing baselines for: {', '.join(sorted(missing_baselines))}")
lines.append("")
lines.append("| " + " | ".join(header) + " |")
lines.append("| " + " | ".join(["---"] * len(header)) + " |")

if summary_rows:
    for row in summary_rows:
        value = f"{row['value']:.2f}"
        limit = f"{row['comparator']} {row['limit']:.2f}"
        status_cell = "✅ PASS" if row['status'] == 'PASS' else "❌ FAIL"
        lines.append(
            f"| {row['benchmark']} | {row['metric']} | {row['stat']} | {value} | {limit} | {status_cell} |")
else:
    lines.append("| _No metrics evaluated_ | | | | | |")

if failures:
    lines.append("")
    lines.append("## Failing Checks")
    for failure in failures:
        lines.append(f"- {failure}")

report_path.write_text("\n".join(lines) + "\n", encoding='utf-8')

print(status)
if status != 'PASS':
    sys.exit(1)
PY

exit_code=$?

if [[ $exit_code -ne 0 ]]; then
  die "Macrobenchmark performance regression detected. See ${REPORT_PATH}"
fi

echo "[macrobenchmark] Analysis complete. Report: ${REPORT_PATH}"
if [[ ${SELF_TEST} -eq 1 ]]; then
  echo "[macrobenchmark] Fixture JSON report: ${JSON_PATH}" >&2
fi
