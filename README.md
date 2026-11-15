# Hyper120

**The easiest, most consistent rootless 120Hz fix for Xiaomi / HyperOS.**

Unlike other workarounds that force you to disable `com.miui.powerkeeper` (hurting daily battery life), root your phone, or keep a PC connected, **Hyper120** unlocks **120fps** for your chosen games while keeping PowerKeeper and adaptive refresh rate (`is_smart_fps=1`) **100% enabled** for normal use.

---

## Why Hyper120 Is the Easiest & Most Consistent Fix

- **No Root Required:** Installs like a normal Android app.
- **No PC Needed After Install:** Runs on-device, auto-starts on boot (`BOOT_COMPLETED`), and keeps your 120Hz overrides active in the background.
- **Zero Normal-Use Battery Trade-off:** PowerKeeper stays enabled and adaptive refresh rate still drops to 60Hz/idle in regular apps.
- **Consistent Against Resets:** Periodically refreshes PowerKeeper's `mPrivGames` map while your screen is on so games stay locked at 120Hz.
- **Simple App Picker:** Search and check any installed game or app — no manual package typing or ADB commands required.

---

## Screenshots

| Main App Picker | About & Support |
| :---: | :---: |
| <img src="assets/ui-main.png" width="320" alt="Hyper120 Main UI" /> | <img src="assets/ui-about.png" width="320" alt="Hyper120 About Page" /> |

### In-Game (Mobile Legends: Bang Bang)

<img src="assets/ingame-mlbb.png" width="720" alt="Mobile Legends running with Hyper120 active" />

---

## Proof of 120Hz Working (Xiaomi 14 / `houji`, Android 16)

Before **Hyper120**, `com.miui.powerkeeper` (`DisplayFrameSetting`) placed `com.mobile.legends` into `mDefaultGameApp(60)` and clamped the display to `modeId 2` (`60.000004` Hz):

```text
mDisplayModeSpecs={baseModeId=2 ... render: (0.0 60.0)}
mActiveRenderFrameRate=60.000004
```

With **Hyper120** active (and `PowerKeeper` + `is_smart_fps=1` **still enabled**), `dumpsys` confirms PowerKeeper's `mPrivGames` is populated and the display switches to **`120.00001` Hz** (`baseModeId=1`) while Mobile Legends is in the foreground:

```text
mDisplayModeSpecs={baseModeId=1 allowGroupSwitching=false primary=physical: (0.0 Infinity) render: (0.0 120.0) appRequest=physical: (0.0 Infinity) render: (0.0 120.0) idleScreenRefreshRateConfig=null}
mActiveRenderFrameRate=120.00001
fpsconfig mPrivGames={com.roblox.client=120, com.tungsten.fcl=120, com.block.juggle=120, org.ppsspp.ppssppgold=120, com.fancyforce.happywheels=120, com.mobile.legends=120, com.gabama.monopostolite=120, com.xq.archeroii=120, com.mojang.minecraftpe=120, com.plarium.mechlegion=120, com.garena.game.codm=120, com.StefMorojna.SpaceflightSimulator=120}
```

---

## How It Works

On HyperOS / MIUI, `com.miui.powerkeeper` (`DisplayFrameSetting`) checks `mPrivGames` and `mDefaultGameApp(60)` whenever an app enters the foreground. Games in `mDefaultGameApp(60)` get clamped to 60Hz via `DisplayFeatureManager.setScreenEffect(...)`.

**Hyper120** sends `com.xiaomi.joyose.OVERRIDE_GAME_FRESHRATE` (`override_pkg_name`, `override_freshrate=120`) directly to `com.miui.powerkeeper`, registering your selected apps in `mPrivGames` at `120` without disabling PowerKeeper.

---

## Support

- **Buy Me a Coffee:** https://buymeacoffee.com/rohan_reyes
- **GitHub:** https://github.com/Rohan-Reyes/Hyper120
