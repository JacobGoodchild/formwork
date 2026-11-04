#!/usr/bin/env python3
"""Renders every sample form through Formwork.Host to PNG (what the designer canvas shows).
Usage: python3 tools/render_samples.py <out dir>"""
import base64, glob, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_samples as m

out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(m.ROOT, 'build/render')
os.makedirs(out, exist_ok=True)
for designer in sorted(glob.glob(os.path.join(m.ROOT, 'samples/*/*/*.Designer.cs'))):
    name = designer.split(os.sep)[-3] + '-' + os.path.basename(designer).replace('.Designer.cs', '')
    h = m.Host()
    try:
        r = h.call('open', {'path': designer})['result']
        with open(os.path.join(out, name + '.png'), 'wb') as f: f.write(base64.b64decode(r['png']))
        print(name, r['width'], 'x', r['height'], len(r['items']), 'items', len(r['tray']), 'in tray')
    finally:
        h.close()
