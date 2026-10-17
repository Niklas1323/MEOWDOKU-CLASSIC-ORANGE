"""Writes the Privacy Policy and Terms of Service as public web pages (English + German) into a folder,
taken word for word from the game (Main.dc.html), so the store listings and AdMob can link to them.

Usage: python3 tools/make_legal_pages.py <Main.dc.html> <output dir>
"""
import os
import re
import sys

src, out = sys.argv[1:3]
html = open(src, encoding='utf-8').read()
docs = re.findall(r'<div class="mdk-doc" style="flex: 1;[^>]*>\n<sc-if value="\{\{isEn\}\}"[^>]*>\n(.*?)</sc-if>\n<sc-if value="\{\{isDe\}\}"[^>]*>\n(.*?)</sc-if>\n', html, re.S)
assert len(docs) == 2, len(docs)

PAGE = '''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>MEOWDOKI – {title_en} / {title_de}</title>
<style>
body{{margin:0;background:#FCEFDF;color:#2B1A14;font:16px/1.6 'Nunito','Trebuchet MS',system-ui,sans-serif}}
main{{max-width:760px;margin:0 auto;padding:32px 20px 64px}}
h1{{font-size:30px;margin:0 0 4px}} h2{{font-size:24px;margin:40px 0 4px;color:#B4531A}} h3{{font-size:18px;margin:22px 0 6px}}
nav{{display:flex;gap:12px;margin:12px 0 8px}} nav a{{padding:6px 14px;border-radius:16px;background:#FFF8EE;color:#B4531A;text-decoration:none;font-weight:700}}
a{{color:#B4531A}} ul{{padding-left:22px}} hr{{border:none;border-top:2px solid #EBD3B6;margin:40px 0}}
</style>
</head>
<body>
<main>
<h1>MEOWDOKI</h1>
<nav><a href="#en">English</a><a href="#de">Deutsch</a><a href="{other}">{other_label}</a></nav>
<section id="en" lang="en"><h2>{title_en}</h2>
{en}</section>
<hr>
<section id="de" lang="de"><h2>{title_de}</h2>
{de}</section>
</main>
</body>
</html>
'''
os.makedirs(out, exist_ok=True)
for (en, de), name, ten, tde, other, olabel in (
        (docs[0], 'privacy.html', 'Privacy Policy', 'Datenschutzerklärung', 'terms.html', 'Terms / Nutzungsbedingungen'),
        (docs[1], 'terms.html', 'Terms of Service', 'Nutzungsbedingungen', 'privacy.html', 'Privacy / Datenschutz')):
    open(os.path.join(out, name), 'w', encoding='utf-8').write(PAGE.format(title_en=ten, title_de=tde, en=en, de=de, other=other, other_label=olabel))
print('wrote privacy.html and terms.html to', out)
