#!/usr/bin/env python3
"""Generate the Hyper120 icon in every format from one geometry.

Concept: a ring of 24 "frame ticks" that light up like a motion trail
(dim -> indigo -> sky -> teal head), around a forward double chevron.
More frames per sweep = higher refresh rate.

Writes:
  res/drawable/ic_launcher_fg.xml   adaptive-icon foreground (108dp, safe zone r=33)
  res/drawable/ic_launcher_bg.xml   adaptive-icon background
  res/mipmap/ic_launcher.xml        adaptive icon
  res/drawable/ic_launcher.xml      full-bleed composite (used inside the app)
  res/drawable/ic_tile.xml          monochrome Quick Settings tile
  web/assets/icon.svg               website favicon / logo
  docs/images/icon.svg              README logo
Run tools/render_icon_png.sh afterwards for PNG exports.
"""
import math, os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
BG = '#0A0D14'
DIM = (0x23, 0x2B, 0x3D)
RAMP = [(0x63, 0x66, 0xF1), (0x38, 0xBD, 0xF8), (0x2D, 0xD4, 0xBF)]  # indigo, sky, teal
N, R_IN, R_OUT, TICK_W = 24, 24.5, 31.5, 3.6
CX = CY = 54.0
LIT = 15  # how many ticks form the lit trail (head is the last one)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ramp(t):  # 0..1 across RAMP
    seg = min(int(t * (len(RAMP) - 1)), len(RAMP) - 2)
    local = t * (len(RAMP) - 1) - seg
    return lerp(RAMP[seg], RAMP[seg + 1], local)


def hexc(c):
    return '#%02X%02X%02X' % c


def ticks():
    out = []
    head = 7  # tick index of the trail head (clockwise from 12 o'clock)
    for i in range(N):
        ang = -math.pi / 2 + i * 2 * math.pi / N
        x1, y1 = CX + R_IN * math.cos(ang), CY + R_IN * math.sin(ang)
        x2, y2 = CX + R_OUT * math.cos(ang), CY + R_OUT * math.sin(ang)
        age = (head - i) % N  # 0 = head
        if age < LIT:
            t = 1 - age / (LIT - 1)
            col, alpha = ramp(t), 0.35 + 0.65 * t
        else:
            col, alpha = DIM, 1.0
        out.append((f'M{x1:.2f},{y1:.2f} L{x2:.2f},{y2:.2f}', hexc(col), round(alpha, 2)))
    return out


CHEV_BACK = 'M42.5,43 L53,54 L42.5,65'
CHEV_FRONT = 'M54,43 L64.5,54 L54,65'


def vector(body, size=108, vp=108):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{size}dp" android:height="{size}dp"\n'
            f'    android:viewportWidth="{vp}" android:viewportHeight="{vp}">\n{body}</vector>\n')


def vstroke(d, col, w, alpha=1.0):
    return (f'    <path android:pathData="{d}" android:strokeColor="{col}" android:strokeAlpha="{alpha}"\n'
            f'        android:strokeWidth="{w}" android:strokeLineCap="round" android:strokeLineJoin="round" />\n')


def fg_body(mono=None):
    s = ''
    for d, col, a in ticks():
        s += vstroke(d, mono or col, TICK_W, a if not mono else max(a, 0.45))
    s += vstroke(CHEV_BACK, mono or '#38BDF8', 7, 0.55 if not mono else 0.6)
    s += vstroke(CHEV_FRONT, mono or '#F1F5F9', 7)
    return s


def svg(rounded=True):
    s = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">',
         f'<rect width="108" height="108" rx="{24 if rounded else 0}" fill="{BG}"/>',
         '<radialGradient id="g" cx="0.5" cy="0.5" r="0.5"><stop offset="0" stop-color="#38BDF8" stop-opacity="0.22"/>'
         '<stop offset="1" stop-color="#38BDF8" stop-opacity="0"/></radialGradient>',
         '<circle cx="54" cy="54" r="40" fill="url(#g)"/>',
         '<g fill="none" stroke-linecap="round" stroke-linejoin="round">']
    for d, col, a in ticks():
        s.append(f'<path d="{d}" stroke="{col}" stroke-opacity="{a}" stroke-width="{TICK_W}"/>')
    s.append(f'<path d="{CHEV_BACK}" stroke="#38BDF8" stroke-opacity="0.55" stroke-width="7"/>')
    s.append(f'<path d="{CHEV_FRONT}" stroke="#F1F5F9" stroke-width="7"/>')
    s.append('</g></svg>\n')
    return '\n'.join(s)


def write(rel, text):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    print('wrote', rel)


glow = ('    <path android:pathData="M54,14 a40,40 0 1,0 0.01,0 Z" android:fillColor="#38BDF8" android:fillAlpha="0.08" />\n')
write('res/drawable/ic_launcher_fg.xml', vector(fg_body()))
write('res/drawable/ic_launcher_bg.xml', vector(f'    <path android:pathData="M0,0h108v108h-108z" android:fillColor="{BG}" />\n' + glow))
write('res/mipmap/ic_launcher.xml',
      '<?xml version="1.0" encoding="utf-8"?>\n'
      '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
      '    <background android:drawable="@drawable/ic_launcher_bg" />\n'
      '    <foreground android:drawable="@drawable/ic_launcher_fg" />\n'
      '    <monochrome android:drawable="@drawable/ic_tile_108" />\n'
      '</adaptive-icon>\n')
write('res/drawable/ic_launcher.xml', vector(
    f'    <path android:pathData="M24,0h60a24,24 0 0 1 24,24v60a24,24 0 0 1 -24,24h-60a24,24 0 0 1 -24,-24v-60a24,24 0 0 1 24,-24z" android:fillColor="{BG}" />\n'
    + glow + fg_body()))
write('res/drawable/ic_tile_108.xml', vector(fg_body('#FFFFFF')))
# Quick Settings tile: crop the 108 viewport to the ring (x/y 20..88) so it fills 24dp
tile = vector(fg_body('#FFFFFF'), size=24, vp=68).replace('<vector', '<vector', 1)
tile = tile.replace('android:viewportHeight="68">', 'android:viewportHeight="68">\n    <group android:translateX="-20" android:translateY="-20">', 1)
tile = tile.replace('</vector>', '    </group>\n</vector>')
write('res/drawable/ic_tile.xml', tile)
write('web/assets/icon.svg', svg())
write('docs/images/icon.svg', svg())
