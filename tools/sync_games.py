#!/usr/bin/env python3
"""Single source of truth for the game frame-rate list.

Reads data/games.json and writes:
  res/raw/games.json   bundled in the APK (offline fallback)
  web/games.json       fetched by the app at runtime, served by the website
  README.md            table + FAQ name lists between <!-- games:* --> markers
  web/index.html       game cards + FAQ name lists between <!-- games:* --> markers

Run after editing data/games.json (tools/build.sh runs it too).
"""
import html, json, os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
SRC = os.path.join(ROOT, 'data', 'games.json')
CAPS = (120, 90, 60)


def load():
    with open(SRC, encoding='utf-8') as f:
        data = json.load(f)
    seen = set()
    for g in data['games']:
        for k in ('name', 'cap', 'packages', 'short', 'tip'):
            if k not in g:
                sys.exit(f'{g.get("name")}: missing "{k}"')
        if g['cap'] not in CAPS:
            sys.exit(f'{g["name"]}: cap must be one of {CAPS}')
        for p in g['packages']:
            if p in seen:
                sys.exit(f'duplicate package {p}')
            seen.add(p)
    return data


def write(rel, text):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    old = open(path, encoding='utf-8').read() if os.path.exists(path) else None
    if old != text:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
        print('updated', rel)


def replace_block(text, key, body, rel):
    pat = re.compile(r'(<!-- games:%s -->)(.*?)(<!-- /games:%s -->)' % (key, key), re.S)
    if not pat.search(text):
        sys.exit(f'{rel}: missing <!-- games:{key} --> markers')
    return pat.sub(lambda m: m.group(1) + body + m.group(3), text)


def names(games, cap):
    n = [g['name'] for g in games if g['cap'] == cap]
    return ', '.join(n[:-1]) + ' and ' + n[-1] if len(n) > 1 else ''.join(n)


def sync_faq():
    """data/faq.json -> res/raw/faq.json (bundled) + web/faq.json (fetched by the app daily)."""
    path = os.path.join(ROOT, 'data', 'faq.json')
    with open(path, encoding='utf-8') as f:
        faq = json.load(f)
    if not isinstance(faq.get('version'), int):
        sys.exit('faq.json: "version" must be an integer')
    ids = set()
    for it in faq['items']:
        if it.get('id') in ids:
            sys.exit(f'faq.json: duplicate id {it.get("id")}')
        ids.add(it.get('id'))
        for k in ('q', 'a'):
            if not it.get(k, {}).get('en'):
                sys.exit(f'faq.json: {it.get("id")} needs {k}.en')
    faq.pop('_format', None)
    compact = json.dumps(faq, ensure_ascii=False, separators=(',', ':')) + '\n'
    write('res/raw/faq.json', compact)
    write('web/faq.json', compact)


def main():
    data = load()
    games = data['games']
    compact = json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n'
    write('res/raw/games.json', compact)
    write('web/games.json', compact)
    sync_faq()

    # README table
    cols = {c: [g for g in games if g['cap'] == c] for c in CAPS}
    rows = max(len(v) for v in cols.values())
    md = ['', '| ✅ Up to 120 (turn it on in the game) | 🔵 90 max | 🟠 60 max (game\'s own limit) |', '|---|---|---|']
    for i in range(rows):
        cells = []
        for c in CAPS:
            g = cols[c][i] if i < len(cols[c]) else None
            cells.append(f'**{g["name"]}**: {g["short"]}' if g else '')
        md.append('| ' + ' | '.join(cells) + ' |')
    md.append('')
    readme_path = os.path.join(ROOT, 'README.md')
    readme = open(readme_path, encoding='utf-8').read()
    readme = replace_block(readme, 'table', '\n'.join(md), 'README.md')
    readme = replace_block(readme, 'names60', names(games, 60), 'README.md')
    readme = replace_block(readme, 'names120', names(games, 120), 'README.md')
    readme = replace_block(readme, 'names90', names(games, 90), 'README.md')
    write('README.md', readme)

    # Website cards
    head = {
        120: ('p-ok', 'Up to 120', "Has a high frame-rate option. Turn it on in the game's own settings; on many games it only appears on approved phones."),
        90: ('p-maybe', '90 max', 'Can go above 60, but not to 120.'),
        60: ('p-amb', '60 max', "The game itself stops at 60 on Android. Hyper120 can't make these faster."),
    }
    out = ['\n        <div class="caps reveal">']
    for c in CAPS:
        cls, label, desc = head[c]
        out.append('          <div class="card cap">')
        out.append(f'            <div class="cap-head"><span class="pill {cls}"><i></i>{label}</span><p>{html.escape(desc)}</p></div>')
        out.append('            <ul>')
        for g in cols[c]:
            out.append(f'              <li><b>{html.escape(g["name"])}</b><span>{html.escape(g["short"])}</span></li>')
        out.append('            </ul>')
        out.append('          </div>')
    out.append('        </div>\n        ')
    web_path = os.path.join(ROOT, 'web', 'index.html')
    page = open(web_path, encoding='utf-8').read()
    if '<!-- games:cards -->' in page:  # cards moved to supported.html (reads web/games.json live)
        page = replace_block(page, 'cards', '\n'.join(out), 'web/index.html')
    page = replace_block(page, 'names60', html.escape(names(games, 60)), 'web/index.html')
    page = replace_block(page, 'names120', html.escape(names(games, 120)), 'web/index.html')
    page = replace_block(page, 'names90', html.escape(names(games, 90)), 'web/index.html')
    write('web/index.html', page)
    print(f'{len(games)} games OK')


if __name__ == '__main__':
    main()
