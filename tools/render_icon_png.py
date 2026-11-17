#!/usr/bin/env python3
"""Render web/assets/icon.svg to PNGs with headless Chrome (no cairo needed).

Usage: python tools/render_icon_png.py [chrome.exe]
Writes docs/images/icon-512.png and web/assets/icon-512.png / icon-192.png.
"""
import pathlib, shutil, subprocess, sys, tempfile, time

ROOT = pathlib.Path(__file__).resolve().parent.parent
CHROME = sys.argv[1] if len(sys.argv) > 1 else r'C:\Program Files\Google\Chrome\Application\chrome.exe'
svg = (ROOT / 'web' / 'assets' / 'icon.svg').as_uri()

with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
    tmp = pathlib.Path(tmp)
    for size, outs in [(512, ['docs/images/icon-512.png', 'web/assets/icon-512.png']), (192, ['web/assets/icon-192.png'])]:
        html = tmp / f'icon{size}.html'
        html.write_text(f'<html><body style="margin:0;background:transparent">'
                        f'<img src="{svg}" width="{size}" height="{size}" style="display:block"></body></html>')
        png = tmp / f'icon{size}.png'
        subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars',
                        '--default-background-color=00000000', f'--user-data-dir={tmp / "profile"}',
                        f'--window-size={size},{size}', f'--screenshot={png}', html.as_uri()],
                       check=True, capture_output=True)
        for _ in range(100):  # chrome.exe may return before the screenshot is written
            if png.exists() and png.stat().st_size > 0:
                break
            time.sleep(0.1)
        time.sleep(0.3)
        for rel in outs:
            dst = ROOT / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(png, dst)
            print('wrote', rel)
