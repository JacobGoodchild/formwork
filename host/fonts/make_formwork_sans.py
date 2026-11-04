"""Makes "Formwork Sans" from Selawik: the same glyphs with vertical metrics that give Segoe UI's numbers at 9pt
(12 px em): a text height of 15 px (GDI's tmHeight; AutoSize labels are 15 tall in Visual Studio) and a line
height of 16 px with the line gap (Font.Height; single-line text boxes are 16 + 7 = 23 tall in Visual Studio).
In 2048 units: ascent 2048 + descent 512 = 2560 = 15.0 px, line gap 164 = 0.96 px.
The OFL requires a modified font to drop the Reserved Font Name, hence the new name.
Usage: python make_formwork_sans.py Selawik-Regular.ttf FormworkSans-Regular.ttf "Regular" """
import sys
from fontTools.ttLib import TTFont

src, dst, style = sys.argv[1], sys.argv[2], sys.argv[3]
f = TTFont(src)
hhea, os2 = f['hhea'], f['OS/2']
hhea.ascent, hhea.descent, hhea.lineGap = 2048, -512, 164
os2.usWinAscent, os2.usWinDescent = 2048, 512
os2.sTypoAscender, os2.sTypoDescender, os2.sTypoLineGap = 2048, -512, 164
family = 'Formwork Sans'
for rec in f['name'].names:
    if rec.nameID in (1, 16): rec.string = family
    elif rec.nameID == 4: rec.string = f'{family} {style}' if style != 'Regular' else family
    elif rec.nameID == 6: rec.string = f"FormworkSans-{style}"
    elif rec.nameID == 3: rec.string = f"Formwork Sans {style} 1.0"
f.save(dst)
