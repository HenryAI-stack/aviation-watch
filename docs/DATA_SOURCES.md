# Data sources

Only free sources that need no API key are used.

| Data | Source | License / terms | Used for |
|---|---|---|---|
| Airports, runways, frequencies | [OurAirports](https://ourairports.com/data/) | Public domain | Nearest, Direct-To, airport info |
| METAR / TAF | [aviationweather.gov Data API](https://aviationweather.gov/data/api/) (NOAA/NWS) | US government, free. Keep requests low and send a descriptive User-Agent. | Weather pages |
| Position | Watch GPS (platform LocationManager) | — | Navigation |
| Pressure | Watch barometer | — | Altimeter, flight-log detection |
| Heart rate / SpO₂ | Health Services / Samsung Health Sensor SDK | Free. Samsung SDK needs partner approval only for distribution. | Pulse Ox page |

Requests used by the app (worldwide ICAO coverage, plain-text responses decoded on
the watch, cached for 5 minutes):

```
https://aviationweather.gov/api/data/metar?ids=LOWW,LOWG&format=raw
https://aviationweather.gov/api/data/taf?ids=LOWW&format=raw
```

## Possible additions

- **OpenAIP** (airspace, navaids): free account and API key, CC BY-NC 4.0 data.
  Fine for personal non-commercial use.
- **Sunrise/sunset**: computed on the watch, no service needed.

## Disclaimer

None of these are official sources for flight planning. Always use official
briefings (AIS, NOTAMs, official MET) for flight preparation.
