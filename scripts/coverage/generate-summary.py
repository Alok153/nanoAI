#!/usr/bin/env python3
"""Generate a markdown coverage summary from a JaCoCo XML report."""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable, Tuple


def load_counters(report_path: Path) -> Iterable[Tuple[str, int, int]]:
    tree = ET.parse(report_path)
    root = tree.getroot()
    for counter in root.findall("counter"):
        ctype = counter.attrib.get("type", "UNKNOWN")
        covered = int(counter.attrib.get("covered", "0"))
        missed = int(counter.attrib.get("missed", "0"))
        total = covered + missed
        yield ctype, covered, total


def render_rows(counters: Iterable[Tuple[str, int, int]]) -> str:
    lines = ["| Counter | Covered | Total | % |", "| --- | ---: | ---: | ---: |"]
    for ctype, covered, total in counters:
        pct = 0.0 if total == 0 else (covered / total) * 100.0
        lines.append(f"| {ctype} | {covered} | {total} | {pct:.2f}% |")
    return "\n".join(lines)


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print(
            "Usage: generate-summary.py <jacoco-xml> <markdown-output>",
            file=sys.stderr,
        )
        return 1

    xml_path = Path(argv[1]).resolve()
    md_path = Path(argv[2]).resolve()

    if not xml_path.exists():
        print(f"JaCoCo XML report not found: {xml_path}", file=sys.stderr)
        return 2

    counters = list(load_counters(xml_path))
    md_path.parent.mkdir(parents=True, exist_ok=True)

    with md_path.open("w", encoding="utf-8") as fp:
        fp.write("# Coverage Summary\n\n")
        fp.write(f"Source: `{xml_path}`\n\n")
        fp.write(render_rows(counters))
        fp.write("\n")

    print(f"Markdown coverage summary written to {md_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
