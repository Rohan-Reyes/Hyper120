package com.custom.game120hz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tells users when a newer Hyper120 is out.
 *
 * Source: https://hyper120.aten-systems.site/version.json (web/version.json in the repo — update it
 * AFTER the GitHub release is live, that's what switches the prompt on). Falls back to the GitHub
 * releases API. Checked at most every 6 h; the result is cached so the banner works offline.
 */
final class UpdateChecker {
    static final String SITE_URL = "https://hyper120.aten-systems.site/version.json";
    static final String GITHUB_URL = "https://api.github.com/repos/Rohan-Reyes/Hyper120/releases/latest";
    static final String DEFAULT_DOWNLOAD = "https://hyper120.aten-systems.site/download?src=app";
    private static final String PREFS = "game_120hz_prefs";
    private static final String K_LAST = "upd_last_check_ms";
    private static final String K_LATEST = "upd_latest";
    private static final String K_URL = "upd_url";
    private static final String K_NOTES = "upd_notes";
    private static final String K_PROMPTED = "upd_prompted";
    private static final String K_SNOOZE = "upd_snooze_until";
    private static final long INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static final long SNOOZE_MS = 3L * 24L * 60L * 60L * 1000L;

    private UpdateChecker() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Newer version than the installed one, or null. */
    static String available(Context c) {
        String latest = sp(c).getString(K_LATEST, null);
        return latest != null && compare(latest, FpsApplier.appVersion(c)) > 0 ? latest : null;
    }

    static String downloadUrl(Context c) {
        return sp(c).getString(K_URL, DEFAULT_DOWNLOAD);
    }

    /** Release notes lines in the user's language (may be empty). */
    static String[] notes(Context c) {
        String n = sp(c).getString(K_NOTES, "");
        return n.isEmpty() ? new String[0] : n.split("\n");
    }

    /** True the first time it's called for a given version (the one-time sheet). */
    static boolean shouldPrompt(Context c, String version) {
        return !version.equals(sp(c).getString(K_PROMPTED, null));
    }

    static void markPrompted(Context c, String version) {
        sp(c).edit().putString(K_PROMPTED, version).apply();
    }

    static boolean isSnoozed(Context c) {
        return System.currentTimeMillis() < sp(c).getLong(K_SNOOZE, 0L);
    }

    static void snooze(Context c) {
        sp(c).edit().putLong(K_SNOOZE, System.currentTimeMillis() + SNOOZE_MS).apply();
    }

    /** Checks if due (else uses the cache) and runs {@code done} on the UI thread either way. */
    static void checkAsync(Context ctx, Runnable done) {
        Context app = ctx.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        if (System.currentTimeMillis() - sp(app).getLong(K_LAST, 0L) < INTERVAL_MS) {
            main.post(done);
            return;
        }
        new Thread(() -> {
            boolean ok = fetchSite(app) || fetchGithub(app);
            if (ok) sp(app).edit().putLong(K_LAST, System.currentTimeMillis()).apply();
            main.post(done);
        }).start();
    }

    private static boolean fetchSite(Context c) {
        try {
            JSONObject j = new JSONObject(get(SITE_URL, c));
            String v = j.getString("version").replaceFirst("^v", "");
            String lang = Locale.getDefault().getLanguage();
            if ("id".equals(lang)) lang = "in";
            JSONObject notes = j.optJSONObject("notes");
            JSONArray arr = notes == null ? null : notes.optJSONArray(lang);
            if (arr == null && notes != null) arr = notes.optJSONArray("en");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; arr != null && i < arr.length() && i < 6; i++) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(arr.getString(i));
            }
            save(c, v, j.optString("url", DEFAULT_DOWNLOAD), sb.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean fetchGithub(Context c) {
        try {
            JSONObject j = new JSONObject(get(GITHUB_URL, c));
            String v = j.getString("tag_name").replaceFirst("^v", "");
            save(c, v, DEFAULT_DOWNLOAD, "");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void save(Context c, String v, String url, String notes) {
        if (!url.startsWith("https://")) url = DEFAULT_DOWNLOAD;
        sp(c).edit().putString(K_LATEST, v).putString(K_URL, url).putString(K_NOTES, notes).apply();
    }

    private static String get(String u, Context c) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
        try {
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Hyper120/" + FpsApplier.appVersion(c));
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) throw new RuntimeException("HTTP " + conn.getResponseCode());
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                return bo.toString("UTF-8");
            }
        } finally {
            conn.disconnect();
        }
    }

    static int compare(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int va = i < pa.length ? num(pa[i]) : 0;
            int vb = i < pb.length ? num(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
