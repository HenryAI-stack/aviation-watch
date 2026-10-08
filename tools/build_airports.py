#!/usr/bin/env python3
"""Build the compact airport database bundled with the watch app.

Source: OurAirports (https://ourairports.com/data/), released into the public
domain. Only the Python standard library is used — no installs, no API keys.

Usage:
    python3 tools/build_airports.py                       # worldwide
    python3 tools/build_airports.py --countries AT,DE,CH  # regional subset
    python3 tools/build_airports.py --input airports.csv  # use a local copy

Output columns: ident,name,type,lat,lon,elevation_ft
"""
import argparse
import csv
import io
import sys
import urllib.request
from pathlib import Path

SOURCE_URL = "https://davidmegginson.github.io/ourairports-data/airports.csv"
DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "wear/src/main/assets/airports.csv"
TYPES = {
    "large_airport": "LARGE",
    "medium_airport": "MEDIUM",
    "small_airport": "SMALL",
}


def read_source(path):
    if path:
        return Path(path).read_text(encoding="utf-8")
    req = urllib.request.Request(SOURCE_URL, headers={"User-Agent": "aviation-watch-build"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read().decode("utf-8")


def convert(text, countries=None):
    rows = []
    for row in csv.DictReader(io.StringIO(text)):
        kind = TYPES.get(row["type"])
        if kind is None:
            continue
        if countries and row["iso_country"] not in countries:
            continue
        rows.append([
            row["ident"],
            row["name"],
            kind,
            f'{float(row["latitude_deg"]):.5f}',
            f'{float(row["longitude_deg"]):.5f}',
            row["elevation_ft"],
        ])
    rows.sort(key=lambda r: r[0])
    return rows


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--input", help="local airports.csv instead of downloading")
    parser.add_argument("--countries", help="comma-separated ISO country codes, e.g. AT,DE")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    args = parser.parse_args(argv)

    countries = set(args.countries.upper().split(",")) if args.countries else None
    rows = convert(read_source(args.input), countries)

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["ident", "name", "type", "lat", "lon", "elevation_ft"])
        writer.writerows(rows)
    print(f"wrote {len(rows)} airports to {out}", file=sys.stderr)


if __name__ == "__main__":
    main()
