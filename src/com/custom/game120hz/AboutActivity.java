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
        root.addView(UiHelper.topBar(this, getString(R.string.about_title)));

        // Hero
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(16), dp(20), dp(16), dp(8));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        hero.addView(icon, new LinearLayout.LayoutParams(dp(92), dp(92)));
        TextView name = UiHelper.text(this, getString(R.string.app_name), 28, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        name.setPadding(0, dp(16), 0, 0);
        name.setGravity(Gravity.CENTER);
        hero.addView(name);
        TextView ver = UiHelper.chip(this, getString(R.string.about_version, currentVer), UiHelper.COL_TEAL);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vp.topMargin = dp(10);
        hero.addView(ver, vp);
        TextView desc = UiHelper.text(this,
            getString(R.string.about_desc),
            14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        desc.setGravity(Gravity.CENTER);
        desc.setLineSpacing(dp(4), 1f);
        desc.setPadding(dp(8), dp(16), dp(8), 0);
        hero.addView(desc);
        root.addView(hero);

        // Links
        root.addView(sectionLabel(getString(R.string.about_section_app)));
        LinearLayout links = UiHelper.card(this);
        links.setPadding(dp(14), dp(4), dp(14), dp(4));
        final LinearLayout[] updateRow = new LinearLayout[1];
        updateRow[0] = UiHelper.row(this, R.drawable.ui_refresh, UiHelper.COL_PRIMARY,
            getString(R.string.about_update), getString(R.string.about_update_sub, currentVer), null,
            v -> checkForUpdates(updateRow[0], currentVer));
        links.addView(updateRow[0]);
        links.addView(UiHelper.divider(this, 58));
        links.addView(UiHelper.row(this, R.drawable.ui_info, UiHelper.COL_WARN,
            getString(R.string.about_faq), getString(R.string.about_faq_sub), null,
            v -> UiHelper.openUrl(this, "https://hyper120.aten-systems.site/#games")));
        links.addView(UiHelper.divider(this, 58));
        links.addView(UiHelper.row(this, R.drawable.ui_code, 0xFFA5B4FC,
            getString(R.string.about_source), "github.com/Rohan-Reyes/Hyper120", null,
            v -> UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL)));
        links.addView(UiHelper.divider(this, 58));
        links.addView(UiHelper.row(this, R.drawable.ui_flag, UiHelper.COL_ERROR,
            getString(R.string.about_report), getString(R.string.about_report_sub), null,
            v -> UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new/choose")));
        root.addView(links);

        // Setup
        root.addView(sectionLabel(getString(R.string.about_section_running)));
        LinearLayout setup = UiHelper.card(this);
        setup.setPadding(dp(14), dp(4), dp(14), dp(4));
        setup.addView(UiHelper.row(this, R.drawable.ui_settings, UiHelper.COL_TEAL,
            getString(R.string.about_appinfo),
            getString(R.string.about_appinfo_sub),
            null, v -> {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
                startActivity(i);
            }));
        setup.addView(UiHelper.divider(this, 58));
        setup.addView(UiHelper.row(this, R.drawable.ui_gauge, 0xFF818CF8,
            getString(R.string.about_widget), getString(R.string.about_widget_sub), null, v -> addWidget()));
        setup.addView(UiHelper.divider(this, 58));
        setup.addView(UiHelper.row(this, R.drawable.ui_info, UiHelper.COL_PRIMARY,
            getString(R.string.about_guide), getString(R.string.about_guide_sub), null,
            v -> startActivity(new Intent(this, OnboardingActivity.class))));
        root.addView(setup);

        // Support
        root.addView(sectionLabel(getString(R.string.about_section_support)));
        LinearLayout coffee = UiHelper.card(this);
        coffee.setPadding(dp(14), dp(4), dp(14), dp(4));
        coffee.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.07f),
            UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.3f), 24));
        coffee.addView(UiHelper.row(this, R.drawable.ui_coffee, UiHelper.COL_COFFEE_BG,
            getString(R.string.about_coffee), getString(R.string.about_coffee_sub), null,
            v -> UiHelper.openUrl(this, UiHelper.BUY_ME_A_COFFEE_URL)));
        root.addView(coffee);

        TextView footer = UiHelper.text(this, getString(R.string.about_footer),
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

    /** Pins the widget, or explains HyperOS's "Home screen shortcuts" permission first. */
    private void addWidget() {
        if (!Hyper120Widget.isMiuiShortcutAllowed(this)) {
            android.widget.LinearLayout c = new android.widget.LinearLayout(this);
            c.setOrientation(android.widget.LinearLayout.VERTICAL);
            final android.app.Dialog[] holder = new android.app.Dialog[1];
            TextView t = UiHelper.text(this, getString(R.string.widget_perm_title), 18, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
            c.addView(t);
            TextView b = UiHelper.text(this, getString(R.string.widget_perm_body), 14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
            b.setLineSpacing(dp(4), 1f);
            b.setPadding(0, dp(12), 0, dp(18));
            c.addView(b);
            c.addView(UiHelper.button(this, getString(R.string.widget_perm_open), R.drawable.ui_settings, true, v -> {
                holder[0].dismiss();
                Hyper120Widget.openMiuiPermissions(this);
            }));
            TextView manual = UiHelper.button(this, getString(R.string.widget_perm_manual), 0, false, v -> {
                holder[0].dismiss();
                android.widget.Toast.makeText(this, getString(R.string.widget_pin_manual),
                    android.widget.Toast.LENGTH_LONG).show();
            });
            ((android.widget.LinearLayout.LayoutParams) manual.getLayoutParams()).topMargin = dp(10);
            c.addView(manual);
            holder[0] = UiHelper.sheet(this, c);
            holder[0].show();
            return;
        }
        if (!Hyper120Widget.requestPin(this)) {
            android.widget.Toast.makeText(this, getString(R.string.widget_pin_manual),
                android.widget.Toast.LENGTH_LONG).show();
        }
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
        sub.setText(getString(R.string.upd_checking));
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
                        sub.setText(getString(R.string.upd_available, tag));
                        sub.setTextColor(UiHelper.COL_TEAL);
                    } else {
                        sub.setText(getString(R.string.upd_current, currentVer));
                        sub.setTextColor(UiHelper.COL_TEAL);
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    checking = false;
                    updateUrl = UiHelper.GITHUB_REPO_URL + "/releases/latest";
                    sub.setText(getString(R.string.upd_failed));
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
