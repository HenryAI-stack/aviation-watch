#!/usr/bin/env python3
"""Build the compact airport database bundled with the watch app.

Source: OurAirports (https://ourairports.com/data/), released into the public
domain. Only the Python standard library is used — no installs, no API keys.

Usage:
    python3 tools/build_airports.py                       # worldwide
    python3 tools/build_airports.py --countries AT,DE,CH  # regional subset
    python3 tools/build_airports.py --source-dir data/    # use local copies of the
                                                          # OurAirports CSV files

Outputs (in wear/src/main/assets/, all sorted by airport ident):
    airports.csv     ident,name,type,lat,lon,elevation_ft,iata
    runways.csv      ident,designator,length_ft,width_ft,surface,lighted
    frequencies.csv  ident,type,description,mhz
"""
import argparse
import csv
import io
import sys
import urllib.request
from pathlib import Path

BASE_URL = "https://davidmegginson.github.io/ourairports-data/"
DEFAULT_OUTPUT_DIR = Path(__file__).resolve().parent.parent / "wear/src/main/assets"
TYPES = {
    "large_airport": "LARGE",
    "medium_airport": "MEDIUM",
    "small_airport": "SMALL",
}


def read_source(name, source_dir):
    if source_dir:
        return (Path(source_dir) / name).read_text(encoding="utf-8")
    req = urllib.request.Request(BASE_URL + name, headers={"User-Agent": "aviation-watch-build"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read().decode("utf-8")


def rows(text):
    return csv.DictReader(io.StringIO(text))


def convert_airports(text, countries=None):
    out = []
    for row in rows(text):
        kind = TYPES.get(row["type"])
        if kind is None:
            continue
        if countries and row["iso_country"] not in countries:
            continue
        out.append([
            row["ident"],
            row["name"],
            kind,
            f'{float(row["latitude_deg"]):.5f}',
            f'{float(row["longitude_deg"]):.5f}',
            row["elevation_ft"],
            row.get("iata_code", ""),
        ])
    out.sort(key=lambda r: r[0])
    return out


def convert_runways(text, idents):
    out = []
    for row in rows(text):
        ident = row["airport_ident"]
        if ident not in idents or row.get("closed") == "1":
            continue
        ends = [e for e in (row["le_ident"], row["he_ident"]) if e]
        out.append([
            ident,
            "/".join(ends),
            row["length_ft"],
            row["width_ft"],
            row["surface"].strip().upper(),
            "1" if row.get("lighted") == "1" else "0",
        ])
    out.sort(key=lambda r: (r[0], r[1]))
    return out


def convert_frequencies(text, idents):
    out = []
    for row in rows(text):
        ident = row["airport_ident"]
        if ident not in idents or not row["frequency_mhz"]:
            continue
        out.append([ident, row["type"].strip().upper(), row["description"].strip(), row["frequency_mhz"]])
    out.sort(key=lambda r: (r[0], r[1], r[3]))
    return out


def write(path, header, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, lineterminator="\n")
        writer.writerow(header)
        writer.writerows(data)
    print(f"wrote {len(data)} rows to {path}", file=sys.stderr)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--source-dir", help="directory with airports.csv, runways.csv and "
                                             "airport-frequencies.csv instead of downloading")
    parser.add_argument("--countries", help="comma-separated ISO country codes, e.g. AT,DE")
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    args = parser.parse_args(argv)

    countries = set(args.countries.upper().split(",")) if args.countries else None
    out_dir = Path(args.output_dir)

    airports = convert_airports(read_source("airports.csv", args.source_dir), countries)
    idents = {a[0] for a in airports}
    write(out_dir / "airports.csv", ["ident", "name", "type", "lat", "lon", "elevation_ft", "iata"], airports)
    write(out_dir / "runways.csv", ["ident", "designator", "length_ft", "width_ft", "surface", "lighted"],
          convert_runways(read_source("runways.csv", args.source_dir), idents))
    write(out_dir / "frequencies.csv", ["ident", "type", "description", "mhz"],
          convert_frequencies(read_source("airport-frequencies.csv", args.source_dir), idents))


if __name__ == "__main__":
    main()
