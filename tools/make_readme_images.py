#!/usr/bin/env python3
"""Build README + website images from phone screenshots.

Usage: python tools/make_readme_images.py <screenshot_dir>
Expects 1200x2670 captures named 01_home.png, 02_list.png, 03_tester.png, 04_about.png
(adb exec-out screencap -p > 01_home.png).
Writes docs/images/*.png and copies them to web/assets/.
"""
import os, shutil, sys
from PIL import Image, ImageDraw, ImageFilter

SRC = sys.argv[1]
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'docs', 'images')
WEB = os.path.join(ROOT, 'web', 'assets')
os.makedirs(OUT, exist_ok=True)
BG = (8, 11, 18)


def phone(name, width=540):
    im = Image.open(os.path.join(SRC, name)).convert('RGB')
    w, h = im.size
    im = im.crop((0, int(h * 0.045), w, h - int(h * 0.024)))  # drop status bar + gesture bar
    im = im.resize((width, int(im.height * width / w)), Image.LANCZOS)
    mask = Image.new('L', im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius=int(width * 0.075), fill=255)
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    # hairline frame so dark screens separate from dark backgrounds
    ImageDraw.Draw(out).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius=int(width * 0.075),
                                          outline=(255, 255, 255, 34), width=max(1, width // 260))
    return out


def save(img, name):
    path = os.path.join(OUT, name)
    img.save(path, optimize=True)
    shutil.copyfile(path, os.path.join(WEB, name))
    print(f'{name}: {img.size[0]}x{img.size[1]}, {os.path.getsize(path) // 1024} KB')


shots = {'01_home.png': 'home.png', '02_list.png': 'apps.png',
         '03_tester.png': 'tester.png', '04_about.png': 'about.png'}
for src, dst in shots.items():
    save(phone(src, 400), dst)

# Hero banner: three phones on near-black with sky/indigo/teal glows
W, H = 1600, 900
banner = Image.new('RGBA', (W, H), BG + (255,))
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow)
gd.ellipse((W * 0.22, -H * 0.35, W * 0.78, H * 0.85), fill=(56, 189, 248, 120))
gd.ellipse((-W * 0.1, H * 0.35, W * 0.4, H * 1.2), fill=(99, 102, 241, 90))
gd.ellipse((W * 0.62, H * 0.45, W * 1.1, H * 1.25), fill=(45, 212, 191, 60))
banner = Image.alpha_composite(banner, glow.filter(ImageFilter.GaussianBlur(130)))
specs = [('03_tester.png', 330, -7, (230, 150)), ('02_list.png', 330, 7, (1040, 150)), ('01_home.png', 380, 0, (610, 70))]
for name, width, angle, (x, y) in specs:
    p = phone(name, width)
    shadow = Image.new('RGBA', (p.width + 120, p.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((60, 70, p.width + 60, p.height + 60), radius=40, fill=(0, 0, 0, 190))
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    if angle:
        p = p.rotate(angle, resample=Image.BICUBIC, expand=True)
        shadow = shadow.rotate(angle, resample=Image.BICUBIC, expand=True)
    banner.alpha_composite(shadow, (max(0, x - 60), max(0, y - 40)))
    banner.alpha_composite(p, (x, y))
save(banner.convert('RGB'), 'banner.png')
