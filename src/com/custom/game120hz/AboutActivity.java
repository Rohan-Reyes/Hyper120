package com.custom.game120hz;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
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

/** Dedicated Material About page for Hyper120. */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COL_BG);
        UiHelper.applyNotchSafePadding(this, root, 20, 18, 24);

        // Top Bar with Back Pill
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(20));

        Button backBtn = new Button(this);
        backBtn.setText("\u2190  Back");
        backBtn.setAllCaps(false);
        backBtn.setTextSize(14);
        backBtn.setTextColor(UiHelper.COL_TEXT);
        backBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 20));
        backBtn.setPadding(dp(16), 0, dp(16), 0);
        backBtn.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        TextView header = new TextView(this);
        header.setText("About");
        header.setTextSize(20);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(UiHelper.COL_TEXT);
        header.setPadding(dp(16), 0, 0, 0);
        topBar.addView(header);
        root.addView(topBar);

        // Hero Card
        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCard.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 24));
        heroCard.setPadding(dp(20), dp(24), dp(20), dp(24));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        iconParams.bottomMargin = dp(14);
        icon.setLayoutParams(iconParams);
        heroCard.addView(icon);

        TextView appName = new TextView(this);
        appName.setText("Hyper120");
        appName.setTextSize(24);
        appName.setTypeface(Typeface.DEFAULT_BOLD);
        appName.setTextColor(UiHelper.COL_TEXT);
        heroCard.addView(appName);

        String currentVer = getInstalledVersion();
        TextView version = new TextView(this);
        version.setText("v" + currentVer + "  \u2022  Rootless 120Hz Override");
        version.setTextSize(13);
        version.setTextColor(UiHelper.COL_TEAL);
        version.setPadding(0, dp(4), 0, dp(14));
        heroCard.addView(version);

        TextView desc = new TextView(this);
        desc.setText(
            "Hyper120 unlocks 120fps for selected games and apps on Xiaomi / HyperOS "
            + "by sending Joyose-compatible override broadcasts directly to PowerKeeper — "
            + "keeping adaptive refresh rate and normal-use battery life untouched.");
        desc.setTextSize(14);
        desc.setTextColor(UiHelper.COL_MUTED);
        desc.setGravity(Gravity.CENTER);
        heroCard.addView(desc);

        root.addView(heroCard);

        // Support & Links Card
        LinearLayout linksCard = new LinearLayout(this);
        linksCard.setOrientation(LinearLayout.VERTICAL);
        linksCard.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 24));
        linksCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams linksCardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linksCardParams.topMargin = dp(16);
        linksCard.setLayoutParams(linksCardParams);

        TextView linksTitle = new TextView(this);
        linksTitle.setText("Support & Source");
        linksTitle.setTextSize(16);
        linksTitle.setTypeface(Typeface.DEFAULT_BOLD);
        linksTitle.setTextColor(UiHelper.COL_TEXT);
        linksTitle.setPadding(0, 0, 0, dp(14));
        linksCard.addView(linksTitle);

        // Check for Updates Pill Button
        Button updatePill = new Button(this);
        updatePill.setText("Check for Updates");
        updatePill.setAllCaps(false);
        updatePill.setTextSize(15);
        updatePill.setTypeface(Typeface.DEFAULT_BOLD);
        updatePill.setTextColor(UiHelper.COL_TEXT);
        updatePill.setBackground(UiHelper.pillBg(this, UiHelper.COL_BG, UiHelper.COL_OUTLINE, 32));
        LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        updateParams.bottomMargin = dp(12);
        updatePill.setLayoutParams(updateParams);
        updatePill.setOnClickListener(v -> checkForUpdates(updatePill, currentVer));
        linksCard.addView(updatePill);

        // Buy Me a Coffee Pill Button
        Button coffeePill = new Button(this);
        coffeePill.setText("\u2615  buymeacoffee.com/rohan_reyes");
        coffeePill.setAllCaps(false);
        coffeePill.setTextSize(15);
        coffeePill.setTypeface(Typeface.DEFAULT_BOLD);
        coffeePill.setTextColor(UiHelper.COL_COFFEE_FG);
        coffeePill.setBackground(UiHelper.pillBg(this, UiHelper.COL_COFFEE_BG, 0, 32));
        LinearLayout.LayoutParams coffeeParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        coffeeParams.bottomMargin = dp(12);
        coffeePill.setLayoutParams(coffeeParams);
        coffeePill.setOnClickListener(v -> UiHelper.openUrl(this, UiHelper.BUY_ME_A_COFFEE_URL));
        linksCard.addView(coffeePill);

        // GitHub Repo Pill Button
        Button githubPill = new Button(this);
        githubPill.setText("GitHub: github.com/Rohan-Reyes/Hyper120");
        githubPill.setAllCaps(false);
        githubPill.setTextSize(14);
        githubPill.setTypeface(Typeface.DEFAULT_BOLD);
        githubPill.setTextColor(UiHelper.COL_ON_PRIMARY);
        githubPill.setBackground(UiHelper.pillBg(this, UiHelper.COL_PRIMARY, 0, 32));
        githubPill.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        githubPill.setOnClickListener(v -> UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL));
        linksCard.addView(githubPill);

        root.addView(linksCard);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiHelper.COL_BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    private String getInstalledVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "3.7";
        }
    }

    private void checkForUpdates(Button btn, String currentVer) {
        btn.setEnabled(false);
        btn.setText("Checking GitHub releases...");
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
                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("HTTP " + conn.getResponseCode());
                }
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
                final String targetUrl = apkUrl;
                final boolean newer = compareVersions(tag, currentVer) > 0;
                main.post(() -> {
                    btn.setEnabled(true);
                    if (newer) {
                        btn.setText("Update available: v" + tag + " (Tap to download)");
                        btn.setTextColor(UiHelper.COL_ON_PRIMARY);
                        btn.setBackground(UiHelper.pillBg(this, UiHelper.COL_TEAL, 0, 32));
                        btn.setOnClickListener(v -> UiHelper.openUrl(this, targetUrl));
                    } else {
                        btn.setText("\u2713 Up to date (v" + currentVer + ")");
                        btn.setTextColor(UiHelper.COL_TEAL);
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    btn.setEnabled(true);
                    btn.setText("Check failed \u2014 Tap to open Releases");
                    btn.setOnClickListener(v -> UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/releases/latest"));
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

    private int dp(int v) {
        return UiHelper.dp(this, v);
    }
}
