"""Builds a web version of MEOWDOKI for testing on an iPhone (Safari › Share › Add to Home Screen).

Usage: python3 tools/pack_iphone_web.py <output dir>
Run tools/pack_web.py first: this reuses the Android assets and only makes their paths relative,
so the game works from any folder of a web host. Progress is saved in the browser (window.__meowWeb);
ads are placeholders because the Google ads SDK only exists in the Android app.
"""
import os
import re
import shutil
import sys

out = sys.argv[1]
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
assets = os.path.join(root, 'android', 'app', 'src', 'main', 'assets')
shutil.rmtree(out, ignore_errors=True)
shutil.copytree(assets, out)


def relative(path):
    text = open(path, encoding='utf-8').read()
    text = re.sub(r'([("\'])/(img|vendor)/', r'\1\2/', text)
    open(path, 'w', encoding='utf-8').write(text)


for name in ('Main.dc.html', 'support.js', 'App.dc.html'):
    relative(os.path.join(out, name))

icon = os.path.join(root, 'android', 'app', 'src', 'main', 'res', 'mipmap-xxxhdpi', 'ic_launcher.png')
shutil.copy(icon, os.path.join(out, 'icon.png'))

app = open(os.path.join(out, 'App.dc.html'), encoding='utf-8').read()
head = ('<meta name="apple-mobile-web-app-capable" content="yes">\n'
        '<meta name="mobile-web-app-capable" content="yes">\n'
        '<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">\n'
        '<meta name="apple-mobile-web-app-title" content="MEOWDOKI">\n'
        '<meta name="theme-color" content="#FCEFDF">\n'
        '<link rel="apple-touch-icon" href="icon.png">\n'
        '<script>window.__meowWeb = true;</script>\n')
assert '<title>MEOWDOKI</title>' in app
app = app.replace('<title>MEOWDOKI</title>', '<title>MEOWDOKI</title>\n' + head)
open(os.path.join(out, 'index.html'), 'w', encoding='utf-8').write(app)
assert '"/img/' not in open(os.path.join(out, 'Main.dc.html'), encoding='utf-8').read()
print('iPhone web build in', out)
