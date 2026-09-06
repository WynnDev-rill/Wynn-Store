#!/usr/bin/env python3
"""Public data -> validated, versioned snapshot. No keys and no generated game facts.

Official GMS shapes were verified against the public site's configuration.
Rone Arena is the fallback and Academy adapter (BSD-3-Clause; attribution in app).
MLBBHub's public catalog supplements current item recipes and costs from Liquipedia.
An unavailable source preserves its last successful payload and ORIGINAL timestamp.
"""
from __future__ import annotations

import argparse
import concurrent.futures
from datetime import datetime, timezone, timedelta
import hashlib
import html
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
NOW = datetime.now(timezone.utc)
STAMP = NOW.isoformat(timespec="seconds").replace("+00:00", "Z")
UA = "RoadToImmortal/1.0 (github.com/WynnDev-rill/Wynn-Store; community companion)"


def clean(value):
    if not value:
        return ""
    text = re.sub(r"<br\s*/?>", "\n", str(value), flags=re.I)
    text = html.unescape(re.sub(r"<[^>]+>", "", text))
    return re.sub(r"[ \t]+", " ", text).strip()


def stamp(value):
    if not value:
        return None
    if isinstance(value, (int, float)):
        return datetime.fromtimestamp(value / 1000, timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")
    return str(value) if "T" in str(value) else str(value) + "T00:00:00Z"


def latest(records):
    values = [x.get("_updatedAt") or x.get("updatedAt") or x.get("createdAt") for x in records]
    return stamp(max((v for v in values if isinstance(v, (int, float))), default=0))


def provenance(name, url, updated=None, checked=STAMP, status="official", note=""):
    return dict(name=name, url=url, updatedAt=updated, checkedAt=checked, status=status, note=note)


def percentage(v):
    if v is None:
        return None
    number = float(v) * 100
    if not 0 <= number <= 100:
        raise ValueError(f"Out-of-range rate: {v}")
    return round(number, 5)


def records(value):
    if not isinstance(value, dict) or value.get("code", 0) not in (0, 2000):
        raise ValueError("Upstream did not return a successful data envelope")
    data = value.get("data") or {}
    result = data.get("records") if isinstance(data, dict) else None
    if not isinstance(result, list):
        raise ValueError("Upstream records missing")
    total = data.get("total", len(result))
    if total > len(result):
        raise ValueError(f"Truncated page: {len(result)} of {total}")
    return result


class Fetcher:
    def __init__(self, cache, offline=False):
        self.cache, self.offline = Path(cache), offline
        self.cache.mkdir(parents=True, exist_ok=True)
        self.report = []

    def get(self, name, url, payload=None, ttl=0):
        path = self.cache / f"{name}.json"
        previous = json.loads(path.read_text()) if path.exists() else None
        if previous and (self.offline or (ttl and NOW - datetime.fromisoformat(previous["checkedAt"].replace("Z", "+00:00")) < timedelta(hours=ttl))):
            self.report.append(dict(name=name, status="cached", checkedAt=previous["checkedAt"]))
            return previous
        if not self.offline:
            for attempt in range(2):
                try:
                    req = urllib.request.Request(url, data=json.dumps(payload).encode() if payload is not None else None, headers={
                        "User-Agent": UA, "Accept": "application/json,text/html", "Content-Type": "application/json",
                        "X-Lang": "id", "Origin": "https://www.mobilelegends.com",
                    })
                    with urllib.request.urlopen(req, timeout=35) as response:
                        body = response.read(20_000_001)
                        if len(body) > 20_000_000:
                            raise ValueError("Response too large")
                        text = body.decode("utf-8")
                    # Cache only a valid success envelope, never API error responses.
                    if text.lstrip().startswith("{"):
                        parsed = json.loads(text)
                        if parsed.get("code", 0) not in (0, 2000):
                            raise ValueError("Upstream error envelope")
                    result = dict(text=text, checkedAt=STAMP, url=url)
                    path.write_text(json.dumps(result, ensure_ascii=False))
                    self.report.append(dict(name=name, status="ok", bytes=len(body)))
                    return result
                except (OSError, ValueError) as exc:
                    if isinstance(exc, urllib.error.HTTPError) and exc.code in (401, 403, 404, 405):
                        break  # Respect denied/deprecated sources; do not try to evade them.
                    if attempt == 0:
                        time.sleep(1.5)
            self.report.append(dict(name=name, status="stale" if previous else "unavailable"))
        if previous:
            return previous
        raise ValueError(f"Source unavailable: {name}")


def json_records(fetcher, name, url, payload=None, ttl=0):
    result = fetcher.get(name, url, payload, ttl)
    return records(json.loads(result["text"])), result["checkedAt"]


def normalize_heroes(rows, minimum=100):
    result = []
    for row in rows:
        d = row["data"]
        h = d.get("hero", {}).get("data", {})
        hero_id = int(d.get("hero_id") or h.get("heroid") or 0)
        if not hero_id or not h.get("name"):
            continue
        skills = []
        seen = set()
        for form, group in enumerate(h.get("heroskilllist") or [], 1):
            for skill in group.get("skilllist", []):
                key = str(skill.get("skillid") or skill.get("skillname"))
                if key in seen:
                    continue
                seen.add(key)
                skills.append(dict(id=key, name=clean(skill.get("skillname")), icon=skill.get("skillicon", ""),
                                   description=clean(skill.get("skilldesc")), cooldown=clean(skill.get("skillcd&cost")),
                                   tags=[clean(t.get("tagname", "")) if isinstance(t, dict) else clean(t) for t in skill.get("skilltag") or []], form=form))
        rel = d.get("relation") or {}
        def relation(key):
            v = rel.get(key) or {}
            return dict(ids=[int(i) for i in v.get("target_hero_id") or [] if str(i).isdigit()], description=clean(v.get("desc")))
        result.append(dict(id=hero_id, name=h["name"], icon=h.get("head") or d.get("head", ""),
                           portrait=d.get("painting") or d.get("head_big") or h.get("head", ""),
                           roles=[str(r).title() for r in h.get("sortlabel") or [] if r],
                           lanes=["EXP" if str(l).lower().replace(" lane", "") == "exp" else str(l).lower().replace(" lane", "").title() for l in h.get("roadsortlabel") or [] if l],
                           speciality=h.get("speciality") or [], story=clean(h.get("story")),
                           difficulty=int(float(h.get("difficulty") or 0)), skills=skills,
                           counters=relation("weak"), strongAgainst=relation("strong"), synergy=relation("assist"),
                           updatedAt=stamp(d.get("hero", {}).get("_updatedAt") or row.get("updatedAt"))))
    if len(result) < minimum:
        raise ValueError("Hero catalog unexpectedly small")
    ids = {h["id"] for h in result}
    for h in result:
        for key in ("counters", "strongAgainst", "synergy"):
            h[key]["ids"] = [i for i in h[key]["ids"] if i in ids and i != h["id"]]
    return sorted(result, key=lambda h: h["name"].casefold())


def normalize_meta(opponents, teammates, rank, days, checked):
    friends = {int(r["data"]["main_heroid"]): r["data"] for r in teammates}
    def edges(values):
        return [dict(heroId=int(e["heroid"]), winRate=percentage(e.get("hero_win_rate")),
                     delta=round(float(e["increase_win_rate"]) * 100, 5) if e.get("increase_win_rate") is not None else None,
                     appearance=percentage(e.get("hero_appearance_rate"))) for e in values if e.get("heroid")]
    entries = []
    for row in opponents:
        d = row["data"]
        if str(d.get("match_type")) != "0":
            continue
        hero_id = int(d["main_heroid"])
        entries.append(dict(heroId=hero_id, win=percentage(d.get("main_hero_win_rate")),
                            pick=percentage(d.get("main_hero_appearance_rate")), ban=percentage(d.get("main_hero_ban_rate")),
                            counters=edges(d.get("sub_hero") or []), strongAgainst=edges(d.get("sub_hero_last") or []),
                            synergy=edges(friends.get(hero_id, {}).get("sub_hero") or [])))
    if len(entries) < 50:
        raise ValueError(f"Insufficient rank coverage: {rank}")
    return dict(rank=rank, days=days,
                source=provenance("MLBB • GMS", "https://www.mobilelegends.com/rank", latest(opponents), checked,
                                  note="Agregat 7 hari. Matchup menampilkan win rate hero lawan dan selisih terhadap baseline hero tersebut; jumlah pertandingan tidak tersedia."), heroes=entries)


def next_props(text, key):
    """Decode public Next RSC string chunks as DATA; never eval scripts."""
    chunks = []
    for match in re.finditer(r"self\.__next_f\.push\(\[1,(\"(?:\\.|[^\"\\])*\")\]\)", text):
        try:
            chunks.append(json.loads(match.group(1)))
        except ValueError:
            pass
    combined = "".join(chunks)
    decoder = json.JSONDecoder()
    for match in re.finditer(r'"' + re.escape(key) + r'"\s*:\s*', combined):
        try:
            value, _ = decoder.raw_decode(combined[match.end():])
            if isinstance(value, list) and value and isinstance(value[0], dict):
                return value
        except ValueError:
            pass
    raise ValueError(f"Public page no longer exposes {key}")


def page_modified(text):
    values = re.findall(r'"dateModified"\s*:\s*"([^"\\]+)"', text)
    return stamp(values[0]) if values else None


def tags_for(text):
    text = text.lower()
    tags = []
    # Mechanical effect labels, not a hardcoded item list. Both source languages accepted.
    tests = {
        "Regen & shield": r"(reduc\w*|mengurangi).{0,90}(regen|healing|shield)|lifebane",
        "Magic burst": r"magic (defense|damage reduction)|magic damage.{0,90}(reduc|shield)|magic defense",
        "Physical burst": r"physical defense|physical damage.{0,90}(reduc|immune)|(reduc\w*|mengurangi).{0,45}physical damage|kebal.{0,40}physical",
        "Attack speed": r"(reduc\w*|mengurangi).{0,80}attack speed|attack speed.{0,40}(75%|slow)",
        "HP tinggi": r"(current|max|maksimum|saat ini).{0,25}(hp|health)|hp.{0,25}(target|lawan)",
        "Armor tinggi": r"physical penetration|penetrasi physical",
        "Magic defense": r"magic penetration|penetrasi magic",
    }
    for key, pattern in tests.items():
        if re.search(pattern, text, re.S):
            tags.append(key)
    return tags


def normalize_items(official, minimal, hub_text, checked):
    by_name = {re.sub(r"[^a-z0-9]", "", x["data"]["equipname"].lower()): x for x in minimal + official}
    items = {}
    for row in official:
        d = row["data"]
        if not d.get("equipicon", "").startswith("https://") or "dilepas" in d.get("equipname", "").lower():
            continue
        description = clean(d.get("equipskilldesc"))
        items[str(d["equipid"])] = dict(id=str(d["equipid"]), name=d["equipname"], icon=d.get("equipicon", ""), category=d.get("equiptypename") or "Lainnya",
            stats=[clean(x) for x in re.split(r"<br\s*/?>|\n", d.get("equiptips", "")) if clean(x)], description=description,
            tags=tags_for(description), source=provenance("Moonton via Rone Arena", "https://arena.rone.dev/web/academy/equipment/expanded", latest([row]), checked,
            status="community", note="Deskripsi Indonesia dari metadata Academy; tanggal revisi sumber ditampilkan terpisah dari waktu pemeriksaan."))
    if hub_text:
        for x in next_props(hub_text, "items"):
            match = by_name.get(re.sub(r"[^a-z0-9]", "", x["name"].lower()))
            item_id = str(match["data"]["equipid"]) if match else "hub:" + x["slug"]
            effects = [dict(name=clean(a.get("name")), description=clean(a.get("description"))) for a in x.get("abilities") or [] if clean(a.get("description"))]
            labels = {"hp":"HP", "hpregen":"HP Regen", "mp":"Mana", "mpregen":"Mana Regen", "physatk":"Physical Attack", "physdefense":"Physical Defense", "magicpower":"Magic Power", "magicdefense":"Magic Defense", "magicpenFlat":"Magic Penetration", "movespeed":"Movement Speed", "movespeedPct":"Movement Speed", "adaptiveatk":"Adaptive Attack", "adaptiveatkPct":"Adaptive Attack", "attackspeed":"Attack Speed", "cdreduction":"Pengurangan Cooldown", "critchance":"Critical Chance", "hybridsteal":"Hybrid Lifesteal", "lifesteal":"Lifesteal", "slowreduction":"Pengurangan Slow", "spellvamp":"Spell Vamp"}
            percent_keys = {"movespeedPct","adaptiveatkPct","attackspeed","cdreduction","critchance","hybridsteal","lifesteal","slowreduction","spellvamp"}
            stats = [f"+{v}{'%' if k in percent_keys else ''} {labels.get(k, k)}" for k, v in (x.get("stats") or {}).items()]
            description = clean(x.get("uniqueAttribute") or "")
            items[item_id] = dict(id=item_id, name=x["name"], icon=match["data"].get("equipicon") if match else "https://mlbbhub.com/images/items/" + x["iconFilename"],
                category=(x.get("categories") or ["Lainnya"])[0], price=x.get("cost"), stats=stats, description=description, effects=effects,
                tags=tags_for(" ".join([description] + stats + [e["description"] for e in effects])), recipe=[],
                source=provenance("MLBBHub / Liquipedia", x.get("sourcePage") or "https://mlbbhub.com/items", page_modified(hub_text), checked, "community",
                    "Katalog komunitas. Teks efek mengikuti bahasa sumber; tanggal adalah revisi katalog, bukan jaminan setiap atribut berubah pada tanggal itu."))
        slug_ids = {}
        hub_rows = next_props(hub_text, "items")
        for x in hub_rows:
            match = by_name.get(re.sub(r"[^a-z0-9]", "", x["name"].lower()))
            slug_ids[x["slug"]] = str(match["data"]["equipid"]) if match else "hub:" + x["slug"]
        for x in hub_rows:
            recipe = x.get("recipe") or []
            items[slug_ids[x["slug"]]]["recipe"] = [slug_ids[s] for r in recipe if (s := (r.get("slug") if isinstance(r, dict) else r)) in slug_ids]
    return sorted(items.values(), key=lambda i: (i["category"], i["name"]))


def normalize_preparation(rows, category, checked):
    result = []
    for row in rows:
        d = row["data"]
        if category == "Spell":
            skill = d.get("__data") or d
            result.append(dict(id="s:" + str(d.get("battleskillid") or skill.get("skillid")), name=skill.get("skillname", ""),
                icon=skill.get("skillicon", ""), category="Spell", description=clean(skill.get("skilldesc")), stats=[clean(d.get("skillshortdesc"))],
                source=provenance("Moonton via Rone Arena", "https://arena.rone.dev/web/academy/spells", latest([row]), checked, "community")))
        else:
            skill = d.get("emblemskill") or {}
            result.append(dict(id="t:" + str(d.get("giftid")), name=skill.get("skillname", ""), icon=skill.get("skillicon", ""),
                category="Talent " + str(d.get("gifttiers", "")), description=clean(skill.get("skilldescemblem") or skill.get("skilldesc")),
                source=provenance("Moonton via Rone Arena", "https://arena.rone.dev/web/academy/emblems", latest([row]), checked, "community")))
    return result


def normalize_badges(rows):
    keys = {1: "warrior", 2: "elite", 3: "master", 4: "grandmaster", 5: "epic", 6: "legend"}
    result = {}
    for row in rows:
        d = row["data"]
        key = keys.get(d["bigrank"])
        if d["bigrank"] == 7:
            start = d.get("rankid_start", 136)
            key = "immortal" if start >= 236 else "glory" if start >= 186 else "honor" if start >= 161 else "mythic"
        if key:
            result[key] = dict(key=key, name=key.title(), icon=d.get("icon", ""))
    return list(result.values())


class VisibleText(HTMLParser):
    def __init__(self):
        super().__init__(); self.parts = []; self.skip = 0
    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style"): self.skip += 1
    def handle_endtag(self, tag):
        if tag in ("script", "style"): self.skip = max(0, self.skip - 1)
    def handle_data(self, data):
        if not self.skip: self.parts.append(data)


def normalize_season(text, checked, url, now=NOW):
    parser = VisibleText(); parser.feed(text)
    visible = re.sub(r"\s+", " ", " ".join(parser.parts))
    current = re.search(r"current MLBB season is Season\s+(\d+)", visible, re.I)
    if not current:
        raise ValueError("Current season assertion missing; predictions rejected")
    number = int(current.group(1))
    dates = re.findall(r"Season\s+" + str(number) + r"\s+ends on\s+(?:\w+,\s+)?(\w+ \d{1,2}, \d{4})(?: \([^)]*\))?\s+at\s+(\d{2}:\d{2}) UTC", visible, re.I)
    ends = {datetime.strptime(date + " " + hour, "%B %d, %Y %H:%M").replace(tzinfo=timezone.utc) for date, hour in dates}
    if len(ends) != 1:
        raise ValueError("Ambiguous season reset dates")
    end = next(iter(ends))
    if end - now > timedelta(days=140):
        raise ValueError("Reset is implausibly far away")
    return dict(number=number, resetsAt=end.isoformat().replace("+00:00", "Z"), confidence="community" if end > now else "expired",
                source=provenance("MLBBHub", url, page_modified(text), checked, "community", "Jadwal komunitas, diperiksa terhadap countdown dalam game oleh penerbit. Tetap dapat berubah."))


def normalize_builds(rows, hero_id, lane, checked):
    builds, emblems = [], {}
    for row in rows:
        d = row["data"]
        for index, b in enumerate(d.get("build") or [], 1):
            ids = [str(i) for i in b.get("equipid") or []]
            if not ids:
                continue
            source = provenance("Moonton via Rone Arena", f"https://arena.rone.dev/web/academy/heroes/{hero_id}/builds", latest([row]), checked, "community",
                "Statistik kombinasi item inti pada rank Mythic dan lane ini. Bukan urutan pembelian enam item; sampel pertandingan tidak dipublikasikan.")
            e = b.get("emblem", {}).get("data", {})
            e_id = "e:" + str(e.get("emblemid") or b.get("runeid"))
            if e.get("emblemname"):
                emblems[e_id] = dict(id=e_id, name=e["emblemname"], icon=e.get("attriicon", ""), category="Emblem",
                    description=clean(e.get("emblemattr", {}).get("emblemattr")), source=source)
            builds.append(dict(heroId=hero_id, title="Item inti " + str(index), items=ids,
                spell="s:" + str(b["skillid"]) if b.get("skillid") else None,
                emblems=([e_id] if e_id in emblems else []) + ["t:" + str(t) for t in b.get("new_rune_skill") or []],
                winRate=percentage(b.get("build_win_rate")), pickRate=percentage(b.get("build_pick_rate")),
                rank="mythic", lane=lane, source=source))
    return builds, list(emblems.values())


def validate(catalog):
    assert catalog["schemaVersion"] == 1
    heroes = catalog["heroes"]; ids = {h["id"] for h in heroes}
    assert len(heroes) == len(ids) and len(ids) >= 100, "Invalid hero catalog"
    assert all(h["name"] and h["icon"].startswith("https://") for h in heroes)
    for section in ("items", "spells", "emblems"):
        rows = catalog[section]
        assert len({r["id"] for r in rows}) == len(rows), "Duplicate equipment IDs"
        assert all(r["name"] and r["icon"].startswith("https://") and "placeholder" not in r["icon"] for r in rows)
    for scope in catalog["meta"]:
        assert len({m["heroId"] for m in scope["heroes"]}) == len(scope["heroes"])
        for m in scope["heroes"]:
            assert m["heroId"] in ids
            for key in ("win", "pick", "ban"):
                assert m[key] is None or 0 <= m[key] <= 100
    equipment = {e["id"] for e in catalog["items"] + catalog["emblems"] + catalog["spells"]}
    for b in catalog["builds"]:
        assert b["heroId"] in ids and all(i in equipment for i in b["items"]), "Dangling build"
    assert len(catalog["items"]) >= 60 and len(catalog["spells"]) >= 10
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--skip-builds", action="store_true")
    parser.add_argument("--cache", default=str(ROOT / "pipeline/.cache"))
    args = parser.parse_args()
    cfg = json.loads((ROOT / "pipeline/providers.json").read_text())
    fetch = Fetcher(args.cache, args.offline)
    previous_path = ROOT / "data/catalog.json"
    previous = json.loads(previous_path.read_text()) if previous_path.exists() else {}
    base = dict(pageSize=500, pageIndex=1, filters=[], sorts=[])
    try:
        rows, checked = json_records(fetch, "heroes", f"{cfg['officialBase']}/{cfg['heroSource']}", base)
        heroes = normalize_heroes(rows)
        hero_source = provenance("MLBB • GMS", "https://www.mobilelegends.com", max((h.get("updatedAt") or "" for h in heroes), default="") or latest(rows), checked)
    except (ValueError, KeyError):
        # A documented independent public adapter, with last-good detailed records.
        # New identities may appear even if the primary GMS response shape changes.
        try:
            rows, checked = json_records(fetch, "fallback-heroes", cfg["communityBase"] + "/heroes?size=500&lang=id")
            basics = normalize_heroes(rows)
            old = {h["id"]: h for h in previous.get("heroes", [])}
            heroes = []
            for basic in basics:
                if basic["id"] in old:
                    heroes.append({**old[basic["id"]], "name": basic["name"], "icon": basic["icon"]})
                else:
                    try:
                        detail, _ = json_records(fetch, f"fallback-hero-{basic['id']}", cfg["communityBase"] + f"/heroes/{basic['id']}?lang=id", ttl=72)
                        heroes.extend(normalize_heroes(detail, minimum=1))
                    except (ValueError, KeyError):
                        heroes.append(basic)
            hero_source = provenance("Moonton via Rone Arena", "https://arena.rone.dev/web/heroes", latest(rows), checked, "community", "Provider cadangan. Identitas diperiksa; detail yang belum tersedia mempertahankan revisi terakhirnya.")
        except (ValueError, KeyError):
            if not previous.get("heroes"):
                raise ValueError("No valid hero provider or last-good snapshot")
            heroes, hero_source = previous["heroes"], previous["source"]
    catalog = dict(schemaVersion=1, generatedAt=STAMP, source=hero_source, heroes=heroes)
    academy = cfg["communityBase"] + "/academy"
    def optional(name, path):
        try: return json_records(fetch, name, academy + path + "?size=500&lang=id")
        except (ValueError, KeyError): return [], ""
    equipment, _ = optional("equipment", "/equipment")
    expanded, item_checked = optional("items", "/equipment/expanded")
    talents, talent_checked = optional("emblems", "/emblems")
    spells, spell_checked = optional("spells", "/spells")
    badges, _ = optional("ranks", "/ranks")
    try:
        hub = fetch.get("hub-items", cfg["itemsPage"], ttl=22)
        item_checked = hub["checkedAt"]
        hub_text = hub["text"]
    except ValueError:
        hub_text = None
    catalog["items"] = normalize_items(expanded, equipment, hub_text, item_checked) if expanded or hub_text else previous.get("items", [])
    catalog["emblems"] = normalize_preparation(talents, "Talent", talent_checked) if talents else previous.get("emblems", [])
    catalog["spells"] = normalize_preparation(spells, "Spell", spell_checked) if spells else previous.get("spells", [])
    catalog["ranks"] = normalize_badges(badges) if badges else previous.get("ranks", [])
    catalog["meta"] = []
    for rank, code in cfg["ranks"].items():
        try:
            camps = []
            for camp in ("0", "1"):
                payload = {**base, "filters": [{"field": "bigrank", "operator": "eq", "value": code}, {"field": "match_type", "operator": "eq", "value": camp}]}
                camps.append(json_records(fetch, f"meta-{rank}-{camp}", f"{cfg['officialBase']}/{cfg['metaSource']}", payload))
            catalog["meta"].append(normalize_meta(camps[0][0], camps[1][0], rank, cfg["windowDays"], min(c[1] for c in camps)))
        except (ValueError, KeyError):
            try:
                rows, checked = json_records(fetch, f"fallback-meta-{rank}", cfg["communityBase"] + f"/heroes/rank?size=500&rank={rank}&days={cfg['windowDays']}&lang=id")
                # The rank adapter does not promise complete matchup fields.
                # Retain rates only; do not relabel undated cached edges as fresh.
                minimal = [{"data": {**r["data"], "match_type": "0", "sub_hero": [], "sub_hero_last": []}} for r in rows]
                scope = normalize_meta(minimal, [], rank, cfg["windowDays"], checked)
                scope["source"] = provenance("Moonton via Rone Arena", "https://arena.rone.dev/web/heroes", latest(rows), checked, "community", "Statistik provider cadangan. Matchup dan sinergi tidak disertakan jika bidang sumber tidak lengkap.")
                catalog["meta"].append(scope)
            except (ValueError, KeyError):
                old = next((m for m in previous.get("meta", []) if m["rank"] == rank), None)
                if old: catalog["meta"].append(old)
    try:
        source = fetch.get("hub-season", cfg["seasonPage"])
        catalog["season"] = normalize_season(source["text"], source["checkedAt"], cfg["seasonPage"])
    except ValueError:
        catalog["season"] = previous.get("season", dict(confidence="unavailable", source=provenance("MLBBHub", cfg["seasonPage"], status="unavailable")))
    # Do not promote Academy's old version list to "current patch". A current page
    # may supply a patch label, always linked and dated as community metadata.
    catalog["patch"] = previous.get("patch", {})
    if hub_text:
        match = re.search(r"Patch\s+(\d+\.\d+\.\d+[a-z]?)", clean(hub_text))
        if match: catalog["patch"] = dict(version=match.group(1), title="Patch sumber komunitas", url=cfg["itemsPage"], updatedAt=page_modified(hub_text))
    def builds_for(h):
        lane = h["lanes"][0] if h["lanes"] else ""
        if not lane: return [], []
        name = f"build-{h['id']}-{lane.lower()}"
        try:
            rows, checked = json_records(fetch, name, f"{academy}/heroes/{h['id']}/builds?rank=mythic&lane={lane.lower()}&lang=id", ttl=22)
            if not args.offline: time.sleep(.45)
            return normalize_builds(rows, h["id"], lane, checked)
        except (ValueError, KeyError):
            return [b for b in previous.get("builds", []) if b["heroId"] == h["id"]], []
    catalog["builds"] = []
    extra = {}
    if not args.skip_builds:
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            for builds, emblems in pool.map(builds_for, heroes):
                catalog["builds"].extend(builds)
                for e in emblems: extra[e["id"]] = e
    else:
        catalog["builds"] = previous.get("builds", [])
    emblem_map = {e["id"]: e for e in catalog["emblems"]}
    for e in previous.get("emblems", []):
        if e["category"] == "Emblem": emblem_map[e["id"]] = e
    emblem_map.update(extra)
    catalog["emblems"] = list(emblem_map.values())
    item_ids = {i["id"] for i in catalog["items"]}
    # Fill referenced current item IDs from the official minimal catalog. Never
    # fabricate statistics/descriptions when only identity is available.
    for row in equipment:
        d = row["data"]; key = str(d["equipid"])
        if not d.get("equipicon", "").startswith("https://") or "dilepas" in d.get("equipname", "").lower():
            continue
        if key not in item_ids:
            catalog["items"].append(dict(id=key, name=d["equipname"], icon=d["equipicon"], category="Metadata", source=provenance("Moonton via Rone Arena", academy + "/equipment", latest([row]), item_checked, "community", "Identitas tambahan dari metadata Academy; atribut dan status aktif belum terverifikasi. Tidak digunakan dalam rekomendasi item.")))
            item_ids.add(key)
    catalog["builds"] = [b for b in catalog["builds"] if all(i in item_ids for i in b["items"])]
    validate(catalog)
    if previous and len(heroes) < len(previous["heroes"]) * .95:
        raise ValueError("Unexpected shrink: refusing to replace the last good catalog")
    (ROOT / "data").mkdir(exist_ok=True)
    content = json.dumps(catalog, ensure_ascii=False, separators=(",", ":"))
    temporary = previous_path.with_suffix(".tmp")
    temporary.write_text(content); temporary.replace(previous_path)
    (ROOT / "data/remote-config.json").write_text(json.dumps(dict(schemaVersion=1, refreshHours=6, catalogUrls=cfg["catalogUrls"], disabled=False), indent=2))
    (ROOT / "data/health.json").write_text(json.dumps(dict(generatedAt=STAMP, sha256=hashlib.sha256(content.encode()).hexdigest(), heroes=len(heroes), items=len(catalog["items"]), metaScopes=len(catalog["meta"]), builds=len(catalog["builds"]), sources=fetch.report), indent=2))
    print(json.dumps(dict(heroes=len(heroes), items=len(catalog["items"]), emblems=len(catalog["emblems"]), spells=len(catalog["spells"]), metaScopes=len(catalog["meta"]), builds=len(catalog["builds"]), season=catalog["season"])))


if __name__ == "__main__":
    main()
