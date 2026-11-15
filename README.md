# Hyper120

A lightweight, rootless Android utility for Xiaomi / HyperOS devices that unlocks **120Hz refresh rate** in games throttled to 60fps by `com.miui.powerkeeper` — with **zero battery trade-off during normal use**.

## How It Works
On HyperOS/MIUI, `com.miui.powerkeeper` (`DisplayFrameSetting`) places games like Mobile Legends, Roblox, and Minecraft into `mDefaultGameApp(60)` and clamps the display panel to 60Hz whenever they enter the foreground.

`Hyper120` sends the `com.xiaomi.joyose.OVERRIDE_GAME_FRESHRATE` broadcast (`override_pkg_name`, `override_freshrate=120`) directly to `com.miui.powerkeeper`, populating `mPrivGames` for your selected apps while leaving `PowerKeeper` and adaptive refresh rate (`is_smart_fps=1`) enabled for everything else.

## Features
- Material-styled dark UI with app picker & search
- Automatic background override service (`Auto120Service`) + boot receiver (`BOOT_COMPLETED`)
- Zero root required

## Support
- Buy Me a Coffee: https://buymeacoffee.com/rohan_reyes
