#!/usr/bin/env python3
"""Generate the in-app line icons (24dp, 2px round strokes, white; tinted at runtime)."""
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')

ICONS = {
    'back': ['M19,12H5', 'M12,19l-7,-7 7,-7'],
    'chevron': ['M9,18l6,-6 -6,-6'],
    'search': ['M18,11a7,7 0 1,1 -14,0a7,7 0 1,1 14,0', 'M21,21l-4.6,-4.6'],
    'check': ['M20,6L9,17l-5,-5'],
    'terminal': ['M4,17l6,-6 -6,-6', 'M12,19h8'],
    'gauge': ['M12,14l4,-4', 'M3.34,19a10,10 0 1,1 17.32,0'],
    'refresh': ['M21,12a9,9 0 0,1 -15.74,6.26L3,16', 'M3,21v-5h5',
                'M3,12a9,9 0 0,1 15.74,-6.26L21,8', 'M21,3v5h-5'],
    'coffee': ['M17,8h1a4,4 0 1,1 0,8h-1', 'M3,8h14v9a4,4 0 0,1 -4,4H7a4,4 0 0,1 -4,-4Z',
               'M6,2v2', 'M10,2v2', 'M14,2v2'],
    'code': ['M16,18l6,-6 -6,-6', 'M8,6l-6,6 6,6'],
    'flag': ['M4,15s1,-1 4,-1 5,2 8,2 4,-1 4,-1V3s-1,1 -4,1 -5,-2 -8,-2 -4,1 -4,1z', 'M4,22v-7'],
    'copy': ['M10,8h10a2,2 0 0,1 2,2v10a2,2 0 0,1 -2,2H10a2,2 0 0,1 -2,-2V10a2,2 0 0,1 2,-2z',
             'M4,16a2,2 0 0,1 -2,-2V4a2,2 0 0,1 2,-2h10a2,2 0 0,1 2,2'],
    'zap': ['M13,2L3,14h9l-1,8 10,-12h-9l1,-8z'],
    'gamepad': ['M6,12h4', 'M8,10v4', 'M15,13h0.01', 'M18,11h0.01',
                'M6,6h12a4,4 0 0,1 4,4v4a4,4 0 0,1 -4,4H6a4,4 0 0,1 -4,-4v-4a4,4 0 0,1 4,-4z'],
    'info': ['M22,12a10,10 0 1,1 -20,0a10,10 0 1,1 20,0', 'M12,16v-4', 'M12,8h0.01'],
    'settings': ['M4,21v-7', 'M4,10V3', 'M12,21v-9', 'M12,8V3', 'M20,21v-5', 'M20,12V3',
                 'M1,14h6', 'M9,8h6', 'M17,16h6'],
}
DOTS = 'M12,5m-1.6,0a1.6,1.6 0 1,0 3.2,0a1.6,1.6 0 1,0 -3.2,0 M12,12m-1.6,0a1.6,1.6 0 1,0 3.2,0a1.6,1.6 0 1,0 -3.2,0 M12,19m-1.6,0a1.6,1.6 0 1,0 3.2,0a1.6,1.6 0 1,0 -3.2,0'

HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp" android:height="24dp"\n'
        '    android:viewportWidth="24" android:viewportHeight="24">\n')


def write(name, body):
    path = os.path.join(ROOT, 'res', 'drawable', f'ui_{name}.xml')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(HEAD + body + '</vector>\n')
    print('wrote', os.path.relpath(path, ROOT))


for name, paths in ICONS.items():
    write(name, ''.join(
        f'    <path android:pathData="{d}" android:strokeColor="#FFFFFF" android:strokeWidth="2"\n'
        '        android:strokeLineCap="round" android:strokeLineJoin="round" />\n' for d in paths))
write('more', f'    <path android:pathData="{DOTS}" android:fillColor="#FFFFFF" />\n')
