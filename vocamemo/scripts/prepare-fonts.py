#!/usr/bin/env python3
"""Regenerate the offline typefaces from the checked source versions.

uv run --with fonttools==4.61.1 scripts/prepare-fonts.py jakarta.ttf literata.ttf
"""

import argparse
from hashlib import sha256
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

parser = argparse.ArgumentParser()
parser.add_argument("jakarta", type=Path, help="PlusJakartaSans[wght].ttf from Google Fonts")
parser.add_argument("literata", type=Path, help="Literata[opsz,wght].ttf from Google Fonts")
args = parser.parse_args()

for source, digest in (
    (args.jakarta, "89b3fb38aa0d275d7a731d0d817a4f1622b316b4d7fbdedcf02ee9099ff68bc8"),
    (args.literata, "b41138c9373112f32abb589cc22e8674b06ed4048b0c513be922bdd26f274440"),
):
    if sha256(source.read_bytes()).hexdigest() != digest:
        parser.error(f"Unexpected Google Fonts source checksum for {source}")

output = Path(__file__).resolve().parents[1] / "app/src/main/res/font"
for source, name, axes in (
    (args.jakarta, "jakarta_regular", {"wght": 400}),
    (args.jakarta, "jakarta_medium", {"wght": 500}),
    (args.jakarta, "jakarta_semibold", {"wght": 600}),
    (args.jakarta, "jakarta_bold", {"wght": 700}),
    (args.literata, "literata_medium", {"opsz": 24, "wght": 500}),
):
    font = instantiateVariableFont(TTFont(source), axes, updateFontNames=source == args.jakarta)
    font.recalcTimestamp = False
    font.save(output / f"{name}.ttf")
