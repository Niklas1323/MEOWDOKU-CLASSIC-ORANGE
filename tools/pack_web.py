"""Packs the MEOWDOKI design canvas files into the Android app's assets folder.

Usage: python3 tools/pack_web.py <dir with Main.dc.html> <dir with _blob/ + support.js> <dir with react + nunito files>
The WebView serves the assets folder at https://appassets.androidplatform.net/ (see MainActivity).
"""
import os
import re
import shutil
import sys

src_dc, src_site, vendor = sys.argv[1:4]
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out = os.path.join(root, 'android', 'app', 'src', 'main', 'assets')
shutil.rmtree(out, ignore_errors=True)
# note: Android's packager silently drops asset folders starting with '_', so images go to img/
os.makedirs(os.path.join(out, 'img'))
os.makedirs(os.path.join(out, 'vendor'))


def ext_of(data):
    if data[:8] == b'\x89PNG\r\n\x1a\n':
        return 'png'
    if data[:4] == b'RIFF' and data[8:12] == b'WEBP':
        return 'webp'
    if data[:3] == b'\xff\xd8\xff':
        return 'jpg'
    raise ValueError('unknown image type')


main = open(os.path.join(src_dc, 'Main.dc.html'), encoding='utf-8').read()
for blob in sorted(set(re.findall(r'/_blob/([0-9a-f]{32})', main))):
    data = open(os.path.join(src_site, '_blob', blob), 'rb').read()
    ext = ext_of(data)
    open(os.path.join(out, 'img', blob + '.' + ext), 'wb').write(data)
    main = main.replace('/_blob/' + blob, '/img/' + blob + '.' + ext)
open(os.path.join(out, 'Main.dc.html'), 'w', encoding='utf-8').write(main)

# runtime: load React from the app instead of the CDN (same files, same integrity hashes)
support = open(os.path.join(src_site, 'support.js'), encoding='utf-8').read()
for name in ('react', 'react-dom'):
    url = 'https://cdn.jsdelivr.net/npm/%s@18.3.1/umd/%s.production.min.js' % (name, name)
    assert url in support, url
    support = support.replace(url, '/vendor/%s.production.min.js' % name)
open(os.path.join(out, 'support.js'), 'w', encoding='utf-8').write(support)
for name in ('react', 'react-dom'):
    shutil.copy(os.path.join(vendor, name + '.production.min.js'), os.path.join(out, 'vendor'))
faces = []
for w in (500, 600, 700, 800, 900):
    fn = 'nunito-latin-%d-normal.woff2' % w
    shutil.copy(os.path.join(vendor, fn), os.path.join(out, 'vendor', fn))
    faces.append("@font-face{font-family:'Nunito';font-style:normal;font-weight:%d;font-display:swap;src:url(/vendor/%s) format('woff2')}" % (w, fn))

app = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">
<title>MEOWDOKI</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<style>%s
html,body{margin:0;height:100%%;overflow:hidden;background:#FCEFDF;-webkit-user-select:none;user-select:none;-webkit-touch-callout:none;overscroll-behavior:none}</style>
</helmet>
<div style="position: fixed; inset: 0; overflow: hidden; background: #FCEFDF">
  <dc-import name="Main" start="loading" hint-size="100%%,100%%"></dc-import>
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{}'>
class Component extends DCLogic {
  renderVals() {
    return {};
  }
}
</script>
</body>
</html>
""" % ''.join(faces)
open(os.path.join(out, 'App.dc.html'), 'w', encoding='utf-8').write(app)
assert '/_blob/' not in main
print('packed', len(os.listdir(os.path.join(out, 'img'))), 'images into', out)
