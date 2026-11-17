package com.custom.game120hz;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Sends Joyose-compatible refresh-rate override broadcasts to PowerKeeper and auto-detects games. */
public final class FpsApplier {
    private static final String TAG = "Hyper120";
    private static final String PREFS = "game_120hz_prefs";
    private static final String KEY_PACKAGES = "packages";
    private static final String KEY_IGNORED = "ignored_games";
    private static final String KEY_AUTO_ENABLED = "auto_enabled";
    private static final String KEY_AUTO_ADD_GAMES = "auto_add_games";
    private static final String KEY_TARGET_FPS = "target_fps";
    private static final String KEY_ANON_ID = "anon_id";
    private static final String KEY_LAST_PING = "last_ping_ms";
    private static final String KEY_GAME_FPS_PREFIX = "game_fps_";
    private static final String KEY_SERVICE_BEAT = "service_heartbeat_ms";
    private static final String KEY_ONBOARDED = "onboarded_v42";
    private static final String PING_URL = "https://hyper120.aten-systems.site/api/ping";
    private static final long PING_INTERVAL_MS = 12L * 60L * 60L * 1000L;
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

    public static int[] detectSupportedFps(Context ctx) {
        TreeSet<Integer> rates = new TreeSet<>();
        try {
            DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
            if (d != null) {
                for (Display.Mode m : d.getSupportedModes()) {
                    int r = Math.round(m.getRefreshRate());
                    if (r >= 60) {
                        rates.add(r);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (rates.isEmpty()) {
            return new int[]{60, 90, 120};
        }
        int[] out = new int[rates.size()];
        int i = 0;
        for (int r : rates) {
            out[i++] = r;
        }
        return out;
    }

    public static int getMaxSupportedFps(Context ctx) {
        int[] supported = detectSupportedFps(ctx);
        return supported[supported.length - 1];
    }

    /** Marketing name ("Xiaomi 14") when the ROM exposes it, else brand + model code. */
    public static String getDeviceName(Context ctx) {
        try {
            String n = android.provider.Settings.Global.getString(ctx.getContentResolver(), "device_name");
            if (n != null && !n.trim().isEmpty()) return n.trim();
        } catch (Exception ignored) {
        }
        String brand = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        return model.toLowerCase().startsWith(brand.toLowerCase()) ? model : (brand + " " + model).trim();
    }

    public static String getDeviceSummary(Context ctx) {
        String name = getDeviceName(ctx);
        return name + " (" + Build.DEVICE + ") \u2022 Android " + Build.VERSION.RELEASE
            + " \u2022 Max " + getMaxSupportedFps(ctx) + "Hz";
    }

    public static int getTargetFps(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int saved = sp.getInt(KEY_TARGET_FPS, -1);
        if (saved > 0) {
            return saved;
        }
        return getMaxSupportedFps(ctx);
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
            int own = getGameFps(ctx, trimmed);
            Intent intent = new Intent(ACTION_OVERRIDE);
            intent.putExtra("override_pkg_name", trimmed);
            intent.putExtra("override_freshrate", own > 0 ? own : fps);
            intent.setPackage(POWERKEEPER_PKG);
            ctx.sendBroadcast(intent);
            sent.add(trimmed);
        }
        Log.i(TAG, "applyTargetFps: fps=" + fps + " sent=" + sent.size() + " powerKeeperFound=" + pkFound + " pkgs=" + sent);
        return new ApplyResult(sent.size(), fps, pkFound, sent);
    }

    // ---------------------------------------------------------------- per-game rate

    /** Rate chosen for one game, or 0 to follow the global target. */
    public static int getGameFps(Context ctx, String pkg) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_GAME_FPS_PREFIX + pkg, 0);
    }

    public static void setGameFps(Context ctx, String pkg, int fps) {
        SharedPreferences.Editor e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (fps > 0) e.putInt(KEY_GAME_FPS_PREFIX + pkg, fps);
        else e.remove(KEY_GAME_FPS_PREFIX + pkg);
        e.apply();
    }

    // ---------------------------------------------------------------- health

    /** Written by Auto120Service on every loop so the UI can tell if HyperOS killed it. */
    public static void markServiceAlive(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_SERVICE_BEAT, System.currentTimeMillis()).apply();
    }

    public static long lastServiceBeat(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SERVICE_BEAT, 0L);
    }

    public static boolean isOnboarded(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false);
    }

    public static void setOnboarded(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, true).apply();
    }

    // ---------------------------------------------------------------- device / ROM info

    private static String prop(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Object v = sp.getMethod("get", String.class).invoke(null, key);
            return v == null ? "" : v.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /** "CN", "Global", "EEA", "India"... from the ROM's build properties. */
    public static String romRegion() {
        String mod = prop("ro.product.mod_device").toLowerCase(java.util.Locale.ROOT);
        if (mod.contains("_eea")) return "EEA";
        if (mod.contains("_in_")) return "India";
        if (mod.contains("_ru")) return "Russia";
        if (mod.contains("_id_")) return "Indonesia";
        if (mod.contains("_tw")) return "Taiwan";
        if (mod.contains("_tr")) return "Turkey";
        if (mod.contains("_global")) return "Global";
        String r = prop("ro.miui.build.region");
        if (r.isEmpty()) r = prop("ro.miui.region");
        if (r.equalsIgnoreCase("cn")) return "CN";
        if (r.equalsIgnoreCase("global")) return "Global";
        return r.isEmpty() ? "Unknown" : r.toUpperCase(java.util.Locale.ROOT);
    }

    /** e.g. "HyperOS OS3.0 (OS3.0.307.0.WNCCNXM)" or "MIUI V14 (...)". */
    public static String romVersion() {
        String os = prop("ro.mi.os.version.name");
        String inc = prop("ro.mi.os.version.incremental");
        if (inc.isEmpty()) inc = prop("ro.build.version.incremental");
        if (!os.isEmpty()) return "HyperOS " + os + " (" + inc + ")";
        String miui = prop("ro.miui.ui.version.name");
        if (!miui.isEmpty()) return "MIUI " + miui + " (" + inc + ")";
        return Build.DISPLAY;
    }

    /** Multi-line device block used in GitHub reports. */
    public static String deviceReport(Context ctx) {
        return "Device: " + getDeviceName(ctx) + " (" + Build.MODEL + ", " + Build.DEVICE + ")\n"
            + "ROM: " + romVersion() + " | Region: " + romRegion() + "\n"
            + "Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
            + "Panel max: " + getMaxSupportedFps(ctx) + "Hz | Target: " + getTargetFps(ctx) + "Hz\n"
            + "PowerKeeper: " + (isPowerKeeperInstalled(ctx) ? "found" : "missing")
            + " | Hyper120 v" + appVersion(ctx);
    }

    /** Sends an anonymous random-UUID ping at most once per 12h for the active user counter. */
    public static void pingActiveUser(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - sp.getLong(KEY_LAST_PING, 0L) < PING_INTERVAL_MS) {
            return;
        }
        String id = sp.getString(KEY_ANON_ID, null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            sp.edit().putString(KEY_ANON_ID, id).apply();
        }
        final String finalId = id;
        final String ver = appVersion(ctx);
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(PING_URL + "?id=" + finalId + "&v=" + ver);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    sp.edit().putLong(KEY_LAST_PING, System.currentTimeMillis()).apply();
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    public static String appVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
