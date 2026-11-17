package com.custom.game120hz;

import java.util.HashMap;
import java.util.Map;

/**
 * Known frame-rate limits of popular games on Android.
 *
 * Hyper120 lifts HyperOS's display cap, but a game still renders no faster than its own engine
 * allows. Many games also only show their 90/120 option on phones the developer has approved.
 * Keep this list in sync with the "Game list" section of README.md and web/index.html.
 */
final class GameCaps {
    static final int MAX_60 = 60;
    static final int MAX_90 = 90;
    static final int UP_TO_120 = 120;

    static final class Info {
        final int cap;
        final String note;

        Info(int cap, String note) {
            this.cap = cap;
            this.note = note;
        }
    }

    private static final Map<String, Info> DB = new HashMap<>();

    private static void put(int cap, String note, String... pkgs) {
        Info info = new Info(cap, note);
        for (String p : pkgs) DB.put(p, info);
    }

    static {
        // Can run above 60, usually behind an in-game option that only appears on approved phones
        put(UP_TO_120, "In Graphics, pick Ultra (120) or Super (90). The option only appears on phones Moonton has approved.",
            "com.mobile.legends");
        put(UP_TO_120, "In Graphics, set Frame Rate to Max / Ultra. 120 is only offered on approved phones.",
            "com.garena.game.codm", "com.activision.callofduty.shooter");
        put(UP_TO_120, "Set Graphics to Smooth, then Frame Rate to Ultra Extreme (120). Approved phones only.",
            "com.tencent.ig", "com.pubg.imobile", "com.vng.pubgmobile", "com.pubg.krmobile", "com.rekoo.pubgm");
        put(UP_TO_120, "Settings, Graphics, 120 FPS. Only on supported phones.",
            "com.levelinfinite.sgameGlobal");
        put(UP_TO_120, "Up to 90 or 120 FPS on supported phones, set in Graphics.",
            "com.proxima.dfm", "com.garena.game.df");
        put(UP_TO_120, "Runs at 120 in matches; menus stay at 60.",
            "com.supercell.brawlstars");
        put(UP_TO_120, "Settings, Advanced settings, turn on Native refresh rate.",
            "com.nianticlabs.pokemongo");
        put(UP_TO_120, "90/120 options only appear on some phones; many stay at 60.",
            "com.ea.gp.fifamobile");
        put(UP_TO_120, "Supports up to 120.",
            "com.naturalmotion.customstreetracer2");

        // 90 at most
        put(MAX_90, "High FPS mode gives 90 on some phones. There is no 120 option.",
            "com.dts.freefireth", "com.dts.freefiremax");
        put(MAX_90, "60 FPS on most Android phones, 90 on a few. No 120 on Android.",
            "com.epicgames.fortnite");

        // The game itself stops at 60 on Android
        put(MAX_60, "Roblox caps its mobile app at 60 FPS on every phone.",
            "com.roblox.client");
        put(MAX_60, "60 FPS on Android. The 120 option is iPhone/iPad only.",
            "com.miHoYo.GenshinImpact", "com.HoYoverse.hkrpgoversea");
        put(MAX_60, "Locked to 60 FPS on phones.",
            "com.HoYoverse.Nap");
        put(MAX_60, "The 120 FPS option was removed; phones run at 60.",
            "com.kurogame.wutheringwaves.global");
        put(MAX_60, "High frame rate mode is 60 FPS.",
            "jp.pokemon.pokemonunite");
        put(MAX_60, "PSP games run at their original 60 (or 30) FPS. A faster screen can't add frames.",
            "org.ppsspp.ppsspp", "org.ppsspp.ppssppgold");
    }

    private GameCaps() {}

    static Info get(String pkg) {
        return DB.get(pkg);
    }
}
