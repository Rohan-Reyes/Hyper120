package com.custom.game120hz;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/** About, updates, support and setup help. */
public class AboutActivity extends Activity {
    private boolean checking = false;
    private String updateUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);
        String currentVer = UiHelper.appVersion(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(UiHelper.topBar(this, "About"));

        // Hero
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(16), dp(20), dp(16), dp(8));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        hero.addView(icon, new LinearLayout.LayoutParams(dp(92), dp(92)));
        TextView name = UiHelper.text(this, "Hyper120", 28, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        name.setPadding(0, dp(16), 0, 0);
        name.setGravity(Gravity.CENTER);
        hero.addView(name);
        TextView ver = UiHelper.chip(this, "Version " + currentVer, UiHelper.COL_TEAL);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vp.topMargin = dp(10);
        hero.addView(ver, vp);
        TextView desc = UiHelper.text(this,
            "Unlocks high refresh rates in games on Xiaomi, Redmi and POCO phones. It asks "
                + "PowerKeeper for the rate you pick, so adaptive refresh and battery life stay "
                + "untouched everywhere else. No root, no PC.",
            14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        desc.setGravity(Gravity.CENTER);
        desc.setLineSpacing(dp(4), 1f);
        desc.setPadding(dp(8), dp(16), dp(8), 0);
        hero.addView(desc);
        root.addView(hero);

        // Links
        root.addView(sectionLabel("App"));
        LinearLayout links = UiHelper.card(this);
        links.setPadding(dp(14), dp(4), dp(14), dp(4));
        final LinearLayout[] updateRow = new LinearLayout[1];
        updateRow[0] = UiHelper.row(this, R.drawable.ui_refresh, UiHelper.COL_PRIMARY,
            "Check for updates", "You're on v" + currentVer, null,
            v -> checkForUpdates(updateRow[0], currentVer));
        links.addView(updateRow[0]);
        links.addView(UiHelper.divider(this, 58));
        links.addView(UiHelper.row(this, R.drawable.ui_code, 0xFFA5B4FC,
            "Source code", "github.com/Rohan-Reyes/Hyper120", null,
            v -> UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL)));
        links.addView(UiHelper.divider(this, 58));
        links.addView(UiHelper.row(this, R.drawable.ui_flag, UiHelper.COL_ERROR,
            "Report a bug or device result", "Opens a GitHub issue form", null,
            v -> UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new/choose")));
        root.addView(links);

        // Setup
        root.addView(sectionLabel("Keep it running"));
        LinearLayout setup = UiHelper.card(this);
        setup.setPadding(dp(14), dp(4), dp(14), dp(4));
        setup.addView(UiHelper.row(this, R.drawable.ui_settings, UiHelper.COL_TEAL,
            "Open App info",
            "Turn on Autostart and set Battery saver to No restrictions so HyperOS doesn't stop Hyper120.",
            null, v -> {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
                startActivity(i);
            }));
        root.addView(setup);

        // Support
        root.addView(sectionLabel("Support"));
        LinearLayout coffee = UiHelper.card(this);
        coffee.setPadding(dp(14), dp(4), dp(14), dp(4));
        coffee.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.07f),
            UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.3f), 24));
        coffee.addView(UiHelper.row(this, R.drawable.ui_coffee, UiHelper.COL_COFFEE_BG,
            "Buy me a coffee", "Hyper120 is free. Tips keep updates coming.", null,
            v -> UiHelper.openUrl(this, UiHelper.BUY_ME_A_COFFEE_URL)));
        root.addView(coffee);

        TextView footer = UiHelper.text(this, "Made by Rohan Reyes · Open source",
            12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, 0);
        root.addView(footer, UiHelper.matchWrap(this, 0));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiHelper.COL_BG);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root);
        UiHelper.applyNotchSafePadding(this, scroll, 16, 12, 28);
        setContentView(scroll);
    }

    private View sectionLabel(String s) {
        TextView t = UiHelper.overline(this, s, UiHelper.COL_DIM);
        t.setPadding(dp(6), dp(24), 0, dp(0));
        return t;
    }

    private void checkForUpdates(LinearLayout row, String currentVer) {
        if (updateUrl != null) {
            UiHelper.openUrl(this, updateUrl);
            return;
        }
        if (checking) return;
        checking = true;
        TextView sub = UiHelper.subtitleOf(row);
        sub.setText("Checking GitHub releases…");
        sub.setTextColor(UiHelper.COL_MUTED);
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.github.com/repos/Rohan-Reyes/Hyper120/releases/latest");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "Hyper120/" + currentVer);
                if (conn.getResponseCode() != 200) throw new RuntimeException("HTTP " + conn.getResponseCode());
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                String tag = json.optString("tag_name", "").replaceFirst("^v", "");
                String releaseUrl = json.optString("html_url", UiHelper.GITHUB_REPO_URL + "/releases/latest");
                String apkUrl = releaseUrl;
                JSONArray assets = json.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.getJSONObject(i);
                        if (a.optString("name", "").endsWith(".apk")) {
                            apkUrl = a.optString("browser_download_url", releaseUrl);
                            break;
                        }
                    }
                }
                final String target = apkUrl;
                final boolean newer = compareVersions(tag, currentVer) > 0;
                main.post(() -> {
                    checking = false;
                    if (newer) {
                        updateUrl = target;
                        sub.setText("v" + tag + " is available — tap to download");
                        sub.setTextColor(UiHelper.COL_TEAL);
                    } else {
                        sub.setText("You're up to date (v" + currentVer + ")");
                        sub.setTextColor(UiHelper.COL_TEAL);
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    checking = false;
                    updateUrl = UiHelper.GITHUB_REPO_URL + "/releases/latest";
                    sub.setText("Couldn't reach GitHub — tap to open Releases");
                    sub.setTextColor(UiHelper.COL_WARN);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? parseDigits(pa[i]) : 0;
            int vb = i < pb.length ? parseDigits(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int parseDigits(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private int dp(float v) {
        return UiHelper.dp(this, v);
    }
}
