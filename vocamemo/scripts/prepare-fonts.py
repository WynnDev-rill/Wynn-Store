#!/usr/bin/env python3
"""Development only: uv run --with fonttools==4.61.1 scripts/prepare-fonts.py.
Instantiate official Nunito's wght axis so API26+ renders deliberate weights.
"""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
root=Path(__file__).resolve().parents[1]
for name,weight in [('nunito',400),('nunito_medium',500),('nunito_semibold',600),('nunito_bold',700),('nunito_extrabold',800)]:
 font=instantiateVariableFont(TTFont(root/'design-assets/Nunito-variable.ttf'),{'wght':weight},inplace=False)
 font.recalcTimestamp=False
 font.save(root/f'app/src/main/res/font/{name}.ttf')
 print(name,weight)
