package com.custom.game120hz;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * In-app FAQ. Source of truth is data/faq.json (tools/sync_games.py copies it to res/raw and web/).
 * The APK bundles a copy; the app downloads the latest from the website once a day, so answers
 * can change without an app update.
 *
 * Answer format: one line per paragraph, "1. " numbered step, "- " bullet, **bold**.
 */
final class FaqData {
    static final String REMOTE_URL = "https://hyper120.aten-systems.site/faq.json";
    private static final String CACHE_FILE = "faq.json";
    private static final String PREFS = "game_120hz_prefs";
    private static final String KEY_LAST_FETCH = "faq_last_fetch_ms";
    private static final long FETCH_INTERVAL_MS = 24L * 60L * 60L * 1000L;

    static final class Item {
        final String id;
        final String question;
        final String answer;

        Item(String id, String q, String a) {
            this.id = id;
            question = q;
            answer = a;
        }
    }

    private static volatile List<Item> items = Collections.emptyList();
    private static volatile int version = 0;
    private static boolean loaded = false;

    private FaqData() {}

    static List<Item> items() {
        return items;
    }

    static synchronized void init(Context ctx) {
        if (loaded) return;
        loaded = true;
        try (InputStream in = ctx.getResources().openRawResource(R.raw.faq)) {
            parseAndUse(readAll(in));
        } catch (Exception ignored) {
        }
        File cache = new File(ctx.getFilesDir(), CACHE_FILE);
        if (cache.exists()) {
            try (InputStream in = new FileInputStream(cache)) {
                parseAndUse(readAll(in));
            } catch (Exception ignored) {
            }
        }
    }

    /** Fetches the latest FAQ at most once a day; runs {@code onUpdated} on the UI thread if it changed. */
    static void refreshAsync(Context ctx, Runnable onUpdated) {
        Context app = ctx.getApplicationContext();
        long last = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_FETCH, 0L);
        if (System.currentTimeMillis() - last < FETCH_INTERVAL_MS) return;
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(REMOTE_URL).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Hyper120/" + FpsApplier.appVersion(app));
                if (conn.getResponseCode() != 200) return;
                String body;
                try (InputStream in = conn.getInputStream()) {
                    body = readAll(in);
                }
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_LAST_FETCH, System.currentTimeMillis()).apply();
                int before = version;
                if (parseAndUse(body)) {
                    try (FileOutputStream out = new FileOutputStream(new File(app.getFilesDir(), CACHE_FILE))) {
                        out.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                    if (version != before && onUpdated != null) {
                        new Handler(Looper.getMainLooper()).post(onUpdated);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /** Uses the FAQ if it parses, has items, and is not older than what we have. */
    private static synchronized boolean parseAndUse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        int v = root.optInt("version", 0);
        if (v < version) return false;
        String lang = Locale.getDefault().getLanguage();
        if ("id".equals(lang)) lang = "in";
        JSONArray arr = root.getJSONArray("items");
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject it = arr.getJSONObject(i);
            String q = pick(it.optJSONObject("q"), lang);
            String a = pick(it.optJSONObject("a"), lang);
            if (q != null && a != null) out.add(new Item(it.optString("id"), q, a));
        }
        if (out.isEmpty()) return false;
        items = Collections.unmodifiableList(out);
        version = v;
        return true;
    }

    private static String pick(JSONObject o, String lang) {
        if (o == null) return null;
        String s = o.optString(lang, "");
        if (s.isEmpty()) s = o.optString("en", "");
        return s.isEmpty() ? null : s;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toString("UTF-8");
    }
}
