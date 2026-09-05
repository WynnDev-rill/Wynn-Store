"""Read-only integration diagnosis, usable on GitHub's ordinary runner network."""
import concurrent.futures
import json
import pathlib
import urllib.request
import urllib.error

OUT = pathlib.Path("probe-output")
OUT.mkdir(exist_ok=True)
URLS = {
    "rone-heroes": "https://arena.rone.dev/api/heroes?size=500&lang=id",
    "rone-equipment": "https://arena.rone.dev/api/academy/equipment?size=500&lang=id",
    "rone-items": "https://arena.rone.dev/api/academy/equipment/expanded?size=500&lang=id",
    "rone-emblems": "https://arena.rone.dev/api/academy/emblems?size=500&lang=id",
    "rone-spells": "https://arena.rone.dev/api/academy/spells?size=500&lang=id",
    "rone-ranks": "https://arena.rone.dev/api/academy/ranks?size=500&lang=id",
    "rone-build": "https://arena.rone.dev/api/academy/heroes/1/builds?rank=mythic&lane=gold&lang=id",
    "rone-version": "https://arena.rone.dev/api/academy/meta/version?lang=id",
    "hub-season": "https://mlbbhub.com/server-time/season-schedule",
    "hub-items": "https://mlbbhub.com/items",
    "official-academy": "https://www.mobilelegends.com/academy/guide",
    "official-raw-meta": "https://share.bi.moonton.net/web/hero_predict/latest/hero_predict_7.json",
}


def get(pair):
    name, url = pair
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "RoadToImmortal/1.0 (WynnDev-rill/Wynn-Store; data integration)", "Accept": "application/json,text/html"})
        with urllib.request.urlopen(req, timeout=40) as response:
            body = response.read(25_000_000)
            (OUT / f"{name}.txt").write_bytes(body)
            return {"name": name, "status": response.status, "bytes": len(body), "url": response.url, "lastModified": response.headers.get("Last-Modified")}
    except urllib.error.HTTPError as error:
        return {"name": name, "status": error.code, "error": error.read(500).decode(errors="replace")}
    except Exception as error:
        return {"name": name, "error": str(error)}


with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    result = list(pool.map(get, URLS.items()))
(OUT / "report.json").write_text(json.dumps(result, indent=2))
print(json.dumps(result, indent=2))

