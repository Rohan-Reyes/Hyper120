package com.custom.game120hz;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Sends Joyose-compatible refresh-rate override broadcasts to PowerKeeper and auto-detects games. */
public final class FpsApplier {
    private static final String TAG = "Hyper120";
    private static final String PREFS = "game_120hz_prefs";
    private static final String KEY_PACKAGES = "packages";
    private static final String KEY_IGNORED = "ignored_games";
    private static final String KEY_AUTO_ENABLED = "auto_enabled";
    private static final String KEY_AUTO_ADD_GAMES = "auto_add_games";
    private static final String KEY_TARGET_FPS = "target_fps";
    private static final String ACTION_OVERRIDE = "com.xiaomi.joyose.OVERRIDE_GAME_FRESHRATE";
    private static final String POWERKEEPER_PKG = "com.miui.powerkeeper";

    public static final int[] SUPPORTED_FPS = {60, 90, 120, 144};

    public static final String[] DEFAULT_PACKAGES = {
        "com.mobile.legends",
        "com.roblox.client",
        "com.mojang.minecraftpe"
    };

    private FpsApplier() {}

    public static Set<String> loadPackages(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> saved = sp.getStringSet(KEY_PACKAGES, null);
        if (saved == null) {
            return new LinkedHashSet<>(Arrays.asList(DEFAULT_PACKAGES));
        }
        return new LinkedHashSet<>(saved);
    }

    public static void savePackages(Context ctx, Set<String> pkgs) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PACKAGES, new LinkedHashSet<>(pkgs))
            .apply();
    }

    public static Set<String> loadIgnoredPackages(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> ignored = sp.getStringSet(KEY_IGNORED, null);
        return ignored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ignored);
    }

    public static void setPackageIgnored(Context ctx, String pkg, boolean ignored) {
        Set<String> set = loadIgnoredPackages(ctx);
        if (ignored) {
            set.add(pkg);
        } else {
            set.remove(pkg);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_IGNORED, set)
            .apply();
    }

    public static boolean isAutoEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_ENABLED, true);
    }

    public static void setAutoEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_ENABLED, enabled)
            .apply();
    }

    public static boolean isAutoAddGamesEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_ADD_GAMES, true);
    }

    public static void setAutoAddGamesEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_ADD_GAMES, enabled)
            .apply();
    }

    public static int getTargetFps(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_TARGET_FPS, 120);
    }

    public static void setTargetFps(Context ctx, int fps) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TARGET_FPS, fps)
            .apply();
    }

    public static boolean isGameApp(ApplicationInfo ai) {
        return (ai.category == ApplicationInfo.CATEGORY_GAME)
            || ((ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0);
    }

    /** Auto-adds all installed games (unless manually unchecked by user) when enabled. */
    public static Set<String> syncInstalledGames(Context ctx) {
        Set<String> pkgs = loadPackages(ctx);
        if (!isAutoAddGamesEnabled(ctx)) {
            return pkgs;
        }
        Set<String> ignored = loadIgnoredPackages(ctx);
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        boolean changed = false;
        String selfPkg = ctx.getPackageName();
        for (ApplicationInfo ai : apps) {
            if (selfPkg.equals(ai.packageName)) continue;
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
            if (isGameApp(ai) && !ignored.contains(ai.packageName)) {
                if (pkgs.add(ai.packageName)) {
                    changed = true;
                }
            }
        }
        if (changed) {
            savePackages(ctx, pkgs);
        }
        return pkgs;
    }

    public static final class ApplyResult {
        public final int count;
        public final int fps;
        public final boolean powerKeeperFound;
        public final List<String> packages;

        ApplyResult(int count, int fps, boolean powerKeeperFound, List<String> packages) {
            this.count = count;
            this.fps = fps;
            this.powerKeeperFound = powerKeeperFound;
            this.packages = packages;
        }
    }

    public static boolean isPowerKeeperInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo(POWERKEEPER_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static ApplyResult apply120Hz(Context ctx, Set<String> pkgs) {
        return applyTargetFps(ctx, pkgs, getTargetFps(ctx));
    }

    public static ApplyResult applyTargetFps(Context ctx, Set<String> pkgs, int fps) {
        boolean pkFound = isPowerKeeperInstalled(ctx);
        List<String> sent = new ArrayList<>();
        for (String pkg : pkgs) {
            String trimmed = pkg.trim();
            if (trimmed.isEmpty()) continue;
            Intent intent = new Intent(ACTION_OVERRIDE);
            intent.putExtra("override_pkg_name", trimmed);
            intent.putExtra("override_freshrate", fps);
            intent.setPackage(POWERKEEPER_PKG);
            ctx.sendBroadcast(intent);
            sent.add(trimmed);
        }
        Log.i(TAG, "applyTargetFps: fps=" + fps + " sent=" + sent.size() + " powerKeeperFound=" + pkFound + " pkgs=" + sent);
        return new ApplyResult(sent.size(), fps, pkFound, sent);
    }
}
