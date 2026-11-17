#!/usr/bin/env python3
"""Render res/drawable-nodpi/widget_preview.png (widget picker preview) with headless Chrome.

Mirrors res/layout/widget_hyper120.xml. Usage: python tools/make_widget_preview.py [chrome.exe]
"""
import pathlib, shutil, subprocess, sys, tempfile, time

ROOT = pathlib.Path(__file__).resolve().parent.parent
CHROME = sys.argv[1] if len(sys.argv) > 1 else r'C:\Program Files\Google\Chrome\Application\chrome.exe'
OUT = ROOT / 'res' / 'drawable-nodpi' / 'widget_preview.png'
icon = (ROOT / 'web' / 'assets' / 'icon.svg').as_uri()
W, H = 260, 170  # dp; rendered at 2x

page = f'''<html><head><style>
html,body{{margin:0;background:transparent}}
.w{{box-sizing:border-box;width:{W}px;height:{H}px;padding:14px;border-radius:24px;display:flex;flex-direction:column;
  background:linear-gradient(135deg,#0E2233,#11161F 55%,#141530);border:1px solid rgba(56,189,248,.22);
  font-family:Roboto,"Segoe UI",sans-serif;color:#F1F5F9}}
.top{{display:flex;align-items:center;gap:8px;font-size:13px;font-weight:700}}
.top img{{width:22px;height:22px}} .top span{{flex:1}} .st{{color:#2DD4BF;font-size:11px}}
.big{{flex:1;display:flex;align-items:flex-end;gap:4px}}
.n{{font-size:44px;font-weight:900;line-height:.9;color:#E0F2FE;letter-spacing:-1px}}
.hz{{font-size:16px;font-weight:700;color:#8B97AC;margin-bottom:2px}}
.apps{{font-size:11px;color:#8B97AC;margin-top:4px}}
.row{{display:flex;gap:8px;margin-top:10px}}
.b{{flex:1;height:34px;border-radius:14px;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700}}
.b1{{background:#1A2130;border:1px solid #2A3448}} .b2{{background:linear-gradient(90deg,#38BDF8,#818CF8);color:#04121C}}
</style></head><body><div class="w">
<div class="top"><img src="{icon}"><span>Hyper120</span><b class="st">● Active</b></div>
<div class="big"><span class="n">120</span><span class="hz">Hz</span></div>
<div class="apps">12 apps</div>
<div class="row"><div class="b b1">Turn off</div><div class="b b2">→ 90Hz</div></div>
</div></body></html>'''

with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
    tmp = pathlib.Path(tmp)
    html = tmp / 'w.html'
    html.write_text(page, encoding='utf-8')
    png = tmp / 'w.png'
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=2',
                    '--default-background-color=00000000', f'--user-data-dir={tmp / "p"}',
                    f'--window-size={W},{H}', f'--screenshot={png}', html.as_uri()], check=True, capture_output=True)
    for _ in range(100):
        if png.exists() and png.stat().st_size > 0:
            break
        time.sleep(0.1)
    time.sleep(0.3)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(png, OUT)
    print('wrote', OUT.relative_to(ROOT))
