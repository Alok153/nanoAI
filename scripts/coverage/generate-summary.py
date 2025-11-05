#!/usr/bin/env python3
"""Generate a layer-based markdown coverage summary from a JaCoCo XML report."""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, Optional, Tuple
import xml.etree.ElementTree as ET


ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_LAYER_MAP = ROOT_DIR / "config" / "testing" / "coverage" / "layer-map.json"
DEFAULT_COVERAGE_METADATA = (
    ROOT_DIR / "config" / "testing" / "coverage" / "coverage-metadata.json"
)

DEFAULT_THRESHOLDS: Dict[str, float] = {
    "VIEW_MODEL": 75.0,
    "UI": 65.0,
    "DATA": 70.0,
}

LAYER_DISPLAY_NAMES: Dict[str, str] = {
    "VIEW_MODEL": "View Model",
    "UI": "UI",
    "DATA": "Data",
}

STATUS_ORDER = ("BELOW_TARGET", "ON_TARGET", "EXCEEDS_TARGET")


def parse_args(argv: Iterable[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jacoco_xml", type=Path, help="Path to the merged JaCoCo XML report")
    parser.add_argument("markdown_output", type=Path, help="Path to write the markdown summary")
    parser.add_argument(
        "--json-output",
        dest="json_output",
        type=Path,
        help="Optional path to write a JSON summary for automation consumers",
    )
    parser.add_argument(
        "--layer-map",
        dest="layer_map",
        type=Path,
        default=DEFAULT_LAYER_MAP,
        help=f"Regex mapping file for layer classification (default: {DEFAULT_LAYER_MAP})",
    )
    parser.add_argument(
        "--coverage-metadata",
        dest="coverage_metadata",
        type=Path,
        default=DEFAULT_COVERAGE_METADATA,
        help=(
            "Path to the coverage-metadata.json file that lists minimum percentages per layer "
            f"(default: {DEFAULT_COVERAGE_METADATA})"
        ),
    )
    return parser.parse_args(list(argv)[1:])


def load_layer_map(path: Path) -> Tuple[Tuple[str, Tuple[re.Pattern, ...]], Optional[str]]:
    if not path.exists():
        raise FileNotFoundError(f"Layer map not found: {path}")
    data = json.loads(path.read_text(encoding="utf-8"))
    default_layer = data.get("_default")
    patterns = []
    for layer, regex_list in data.items():
        if layer == "_default":
            continue
        compiled = tuple(re.compile(expression) for expression in regex_list)
        patterns.append((layer, compiled))
    return tuple(patterns), default_layer


def classify_layer(class_name: str, patterns: Tuple[Tuple[str, Tuple[re.Pattern, ...]], ...], default_layer: Optional[str]) -> Optional[str]:
    normalized = class_name.replace(".", "/")
    for layer, compiled_list in patterns:
        for regex in compiled_list:
            if regex.search(normalized):
                return layer
    return default_layer


def load_thresholds(metadata_path: Path) -> Dict[str, float]:
    thresholds = dict(DEFAULT_THRESHOLDS)
    if not metadata_path.exists():
        print(
            f"[coverage] metadata not found at {metadata_path}, falling back to defaults",
            file=sys.stderr,
        )
        return thresholds

    try:
        payload = json.loads(metadata_path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as error:
        print(
            f"[coverage] failed to parse metadata {metadata_path}: {error}. Using defaults",
            file=sys.stderr,
        )
        return thresholds

    for entry in payload.get("metrics", []):
        layer = entry.get("layer")
        minimum = entry.get("minimumPercent")
        if layer and isinstance(minimum, (int, float)):
            thresholds[layer] = float(minimum)

    return thresholds


def iter_class_line_counters(root: ET.Element) -> Iterable[Tuple[str, int, int]]:
    for package in root.findall("package"):
        for class_elem in package.findall("class"):
            class_name = class_elem.get("name", "")
            if not class_name:
                continue
            for counter in class_elem.findall("counter"):
                if counter.get("type") == "LINE":
                    covered = int(counter.get("covered", "0"))
                    missed = int(counter.get("missed", "0"))
                    yield class_name, covered, missed
                    break


def compute_layer_metrics(
    xml_path: Path, layer_map: Path, layer_thresholds: Dict[str, float]
) -> Tuple[Dict[str, Dict[str, float]], Dict[str, int], Iterable[str]]:
    tree = ET.parse(xml_path)
    root = tree.getroot()

    patterns, default_layer = load_layer_map(layer_map)
    totals: Dict[str, Dict[str, float]] = {
        layer: {"covered": 0.0, "missed": 0.0} for layer in layer_thresholds
    }
    unmapped = []

    for class_name, covered, missed in iter_class_line_counters(root):
        layer = classify_layer(class_name, patterns, default_layer)
        if layer not in totals:
            unmapped.append(class_name)
            continue
        totals[layer]["covered"] += covered
        totals[layer]["missed"] += missed

    metrics: Dict[str, Dict[str, float]] = {}
    status_counts: Dict[str, int] = defaultdict(int)

    for layer, threshold in layer_thresholds.items():
        covered = totals[layer]["covered"]
        missed = totals[layer]["missed"]
        total = covered + missed
        coverage = 0.0 if total == 0 else (covered / total) * 100.0
        delta = coverage - threshold
        if coverage < threshold:
            status = "BELOW_TARGET"
        elif coverage > threshold:
            status = "EXCEEDS_TARGET"
        else:
            status = "ON_TARGET"
        status_counts[status] += 1
        metrics[layer] = {
            "coverage": coverage,
            "threshold": threshold,
            "delta": delta,
            "status": status,
        }

    for status in STATUS_ORDER:
        status_counts.setdefault(status, 0)

    return metrics, status_counts, unmapped


def format_percentage(value: float) -> str:
    return f"{value:.2f}%"


def format_delta(value: float) -> str:
    if value > 0:
        return f"+{value:.2f}pp"
    if value < 0:
        return f"-{abs(value):.2f}pp"
    return "0.00pp"


def write_markdown(
    path: Path,
    xml_path: Path,
    metrics: Dict[str, Dict[str, float]],
    status_counts: Dict[str, int],
    unmapped: Iterable[str],
    layer_thresholds: Dict[str, float],
) -> None:
    lines = ["# Coverage Summary", "", f"Source: `{xml_path}`", "", "| Layer | Coverage | Threshold | Delta | Status |", "| --- | ---: | ---: | ---: | --- |"]
    for layer in layer_thresholds:
        data = metrics[layer]
        lines.append(
            "| {layer_name} | {coverage} | {threshold} | {delta} | {status} |".format(
                layer_name=LAYER_DISPLAY_NAMES[layer],
                coverage=format_percentage(data["coverage"]),
                threshold=format_percentage(data["threshold"]),
                delta=format_delta(data["delta"]),
                status=data["status"],
            )
        )

    breakdown_parts = [f"{status}={status_counts[status]}" for status in STATUS_ORDER]
    lines.extend(["", f"Status breakdown: {', '.join(breakdown_parts)}"])

    unmapped_list = list(unmapped)
    if unmapped_list:
        lines.append("")
        lines.append(f"_Unmapped classes ({len(unmapped_list)}):_")
        for class_name in unmapped_list[:10]:
            lines.append(f"- {class_name}")
        if len(unmapped_list) > 10:
            lines.append("- ...")

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_json(
    path: Path,
    xml_path: Path,
    metrics: Dict[str, Dict[str, float]],
    status_counts: Dict[str, int],
    unmapped: Iterable[str],
    layer_thresholds: Dict[str, float],
) -> None:
    machine_metrics = {
        layer.lower().replace("_", ""): {
            "coverage": round(data["coverage"], 2),
            "threshold": data["threshold"],
            "status": data["status"],
            "delta": round(data["delta"], 2),
        }
        for layer, data in metrics.items()
    }

    payload = {
        "buildId": xml_path.stem,
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "layers": machine_metrics,
        "thresholds": {
            layer.lower().replace("_", ""): threshold for layer, threshold in layer_thresholds.items()
        },
        "statusBreakdown": status_counts,
        "unmappedClasses": list(unmapped),
    }

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main(argv: Iterable[str]) -> int:
    args = parse_args(list(argv))
    xml_path = args.jacoco_xml.resolve()
    markdown_path = args.markdown_output.resolve()
    metadata_path = args.coverage_metadata.resolve()

    if not xml_path.exists():
        print(f"JaCoCo XML report not found: {xml_path}", file=sys.stderr)
        return 2

    try:
        thresholds = load_thresholds(metadata_path)
        metrics, status_counts, unmapped = compute_layer_metrics(
            xml_path, args.layer_map.resolve(), thresholds
        )
    except FileNotFoundError as error:
        print(str(error), file=sys.stderr)
        return 3

    write_markdown(markdown_path, xml_path, metrics, status_counts, unmapped, thresholds)

    if args.json_output is not None:
        write_json(
            args.json_output.resolve(), xml_path, metrics, status_counts, unmapped, thresholds
        )

    print(f"Coverage summary written to {markdown_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
