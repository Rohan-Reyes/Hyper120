package com.custom.game120hz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Material-styled UI for Hyper120 with app picker, 3-dot menu, and auto-override service. */
public class MainActivity extends Activity {
    private static final String BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/rohan_reyes";
    private static final String GITHUB_REPO_URL = "https://github.com/Rohan-Reyes/Hyper120";

    private static final int COL_BG = Color.parseColor("#0E1117");
    private static final int COL_SURFACE = Color.parseColor("#181D27");
    private static final int COL_SURFACE_SEL = Color.parseColor("#172636");
    private static final int COL_OUTLINE = Color.parseColor("#2A3244");
    private static final int COL_PRIMARY = Color.parseColor("#38BDF8");
    private static final int COL_ON_PRIMARY = Color.parseColor("#06202E");
    private static final int COL_TEXT = Color.parseColor("#F1F5F9");
    private static final int COL_MUTED = Color.parseColor("#94A3B8");
    private static final int COL_TEAL = Color.parseColor("#2DD4BF");

    private static class AppEntry {
        final String label;
        final String packageName;
        final Drawable icon;
        final boolean isGame;

        AppEntry(String label, String packageName, Drawable icon, boolean isGame) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
            this.isGame = isGame;
        }
    }

    private final List<AppEntry> allApps = new ArrayList<>();
    private final List<AppEntry> filteredApps = new ArrayList<>();
    private Set<String> selectedPackages;
    private AppAdapter adapter;
    private TextView statusText;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;

        Window w = getWindow();
        w.setStatusBarColor(COL_BG);
        w.setNavigationBarColor(COL_BG);

        selectedPackages = FpsApplier.loadPackages(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COL_BG);
        root.setPadding(dp(16), dp(20), dp(16), dp(12));

        // Top Material App Bar with 3-dot overflow menu
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(4), dp(4), dp(4), dp(12));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("Hyper120");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COL_TEXT);
        titleCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Xiaomi 120Hz PowerKeeper Override");
        subtitle.setTextSize(12);
        subtitle.setTextColor(COL_MUTED);
        titleCol.addView(subtitle);
        topBar.addView(titleCol);

        TextView menuBtn = new TextView(this);
        menuBtn.setText("\u22EE");
        menuBtn.setTextSize(24);
        menuBtn.setTypeface(Typeface.DEFAULT_BOLD);
        menuBtn.setTextColor(COL_TEXT);
        menuBtn.setGravity(Gravity.CENTER);
        menuBtn.setBackground(pillBg(COL_SURFACE, COL_OUTLINE, 20));
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        menuBtn.setLayoutParams(menuParams);
        menuBtn.setOnClickListener(this::showOverflowMenu);
        topBar.addView(menuBtn);

        root.addView(topBar);

        // Material Control Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(pillBg(COL_SURFACE, COL_OUTLINE, 20));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setElevation(dp(4));

        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView toggleLabel = new TextView(this);
        toggleLabel.setText("Auto-override in background");
        toggleLabel.setTextColor(COL_TEXT);
        toggleLabel.setTextSize(15);
        toggleLabel.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        toggleRow.addView(toggleLabel);

        Switch autoSwitch = new Switch(this);
        boolean autoOn = FpsApplier.isAutoEnabled(this);
        autoSwitch.setChecked(autoOn);
        autoSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            FpsApplier.setAutoEnabled(this, isChecked);
            syncServiceState(isChecked);
        });
        toggleRow.addView(autoSwitch);
        card.addView(toggleRow);

        Button applyBtn = new Button(this);
        applyBtn.setText("Apply 120Hz Now");
        applyBtn.setAllCaps(false);
        applyBtn.setTextSize(15);
        applyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        applyBtn.setTextColor(COL_ON_PRIMARY);
        applyBtn.setBackground(pillBg(COL_PRIMARY, 0, 24));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        btnParams.topMargin = dp(12);
        applyBtn.setLayoutParams(btnParams);
        applyBtn.setOnClickListener(v -> applyNow());
        card.addView(applyBtn);

        statusText = new TextView(this);
        statusText.setTextColor(COL_TEAL);
        statusText.setTextSize(12);
        statusText.setPadding(0, dp(10), 0, 0);
        card.addView(statusText);

        root.addView(card);

        // Material Search Pill
        EditText searchBox = new EditText(this);
        searchBox.setHint("Search apps or packages...");
        searchBox.setHintTextColor(COL_MUTED);
        searchBox.setTextColor(COL_TEXT);
        searchBox.setTextSize(14);
        searchBox.setBackground(pillBg(COL_SURFACE, COL_OUTLINE, 26));
        searchBox.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.topMargin = dp(12);
        searchBox.setLayoutParams(searchParams);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(searchBox);

        // App Picker List
        ListView listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(dp(8));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        listParams.topMargin = dp(10);
        listView.setLayoutParams(listParams);
        adapter = new AppAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, pos, id) -> {
            AppEntry entry = filteredApps.get(pos);
            if (selectedPackages.contains(entry.packageName)) {
                selectedPackages.remove(entry.packageName);
            } else {
                selectedPackages.add(entry.packageName);
            }
            FpsApplier.savePackages(this, selectedPackages);
            adapter.notifyDataSetChanged();
            applyNow();
        });
        root.addView(listView);

        setContentView(root);

        loadInstalledApps();
        filterApps("");
        applyNow();
        syncServiceState(autoOn);
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, 1, 1, "Support: buymeacoffee.com/rohan_reyes");
        popup.getMenu().add(Menu.NONE, 2, 2, "GitHub: github.com/Rohan-Reyes/Hyper120");
        popup.getMenu().add(Menu.NONE, 3, 3, "About Hyper120");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    openUrl(BUY_ME_A_COFFEE_URL);
                    return true;
                case 2:
                    openUrl(GITHUB_REPO_URL);
                    return true;
                case 3:
                    showAboutDialog();
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Hyper120 v3.0")
            .setMessage(
                "Unlocks 120Hz in Xiaomi PowerKeeper for selected games & apps without root.\n\n"
                + "Support:\n" + BUY_ME_A_COFFEE_URL + "\n\n"
                + "GitHub:\n" + GITHUB_REPO_URL)
            .setPositiveButton("GitHub", (d, w) -> openUrl(GITHUB_REPO_URL))
            .setNeutralButton("Buy Me a Coffee", (d, w) -> openUrl(BUY_ME_A_COFFEE_URL))
            .setNegativeButton("Close", null)
            .show();
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private GradientDrawable pillBg(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) {
            gd.setStroke(dp(1), strokeColor);
        }
        return gd;
    }

    private void syncServiceState(boolean enabled) {
        Intent svc = new Intent(this, Auto120Service.class);
        if (enabled) {
            startForegroundService(svc);
        } else {
            stopService(svc);
        }
    }

    private void applyNow() {
        FpsApplier.apply120Hz(this, selectedPackages);
        statusText.setText("120Hz active for " + selectedPackages.size() + " selected app(s).");
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        allApps.clear();
        for (ApplicationInfo ai : apps) {
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
            if (getPackageName().equals(ai.packageName)) continue;
            String label = pm.getApplicationLabel(ai).toString();
            Drawable icon = pm.getApplicationIcon(ai);
            boolean isGame = (ai.category == ApplicationInfo.CATEGORY_GAME)
                || ((ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0);
            allApps.add(new AppEntry(label, ai.packageName, icon, isGame));
        }
        Collections.sort(allApps, (a, b) -> {
            boolean aSel = selectedPackages.contains(a.packageName);
            boolean bSel = selectedPackages.contains(b.packageName);
            if (aSel != bSel) return aSel ? -1 : 1;
            if (a.isGame != b.isGame) return a.isGame ? -1 : 1;
            return a.label.compareToIgnoreCase(b.label);
        });
    }

    private void filterApps(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        filteredApps.clear();
        for (AppEntry entry : allApps) {
            if (q.isEmpty()
                || entry.label.toLowerCase(Locale.ROOT).contains(q)
                || entry.packageName.toLowerCase(Locale.ROOT).contains(q)) {
                filteredApps.add(entry);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private int dp(int v) {
        return (int) (v * density);
    }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredApps.size(); }
        @Override public Object getItem(int position) { return filteredApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            ImageView iconView;
            TextView nameView;
            TextView pkgView;
            CheckBox checkBox;

            if (convertView == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(10), dp(12), dp(10));

                iconView = new ImageView(MainActivity.this);
                int iconSize = dp(42);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(iconSize, iconSize);
                ip.rightMargin = dp(12);
                iconView.setLayoutParams(ip);
                row.addView(iconView);

                LinearLayout textCol = new LinearLayout(MainActivity.this);
                textCol.setOrientation(LinearLayout.VERTICAL);
                textCol.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                nameView = new TextView(MainActivity.this);
                nameView.setTextColor(COL_TEXT);
                nameView.setTextSize(15);
                nameView.setTypeface(Typeface.DEFAULT_BOLD);
                textCol.addView(nameView);

                pkgView = new TextView(MainActivity.this);
                pkgView.setTextColor(COL_MUTED);
                pkgView.setTextSize(12);
                textCol.addView(pkgView);

                row.addView(textCol);

                checkBox = new CheckBox(MainActivity.this);
                checkBox.setFocusable(false);
                checkBox.setClickable(false);
                row.addView(checkBox);

                row.setTag(new View[] { iconView, nameView, pkgView, checkBox });
            } else {
                row = (LinearLayout) convertView;
                View[] views = (View[]) row.getTag();
                iconView = (ImageView) views[0];
                nameView = (TextView) views[1];
                pkgView = (TextView) views[2];
                checkBox = (CheckBox) views[3];
            }

            AppEntry entry = filteredApps.get(position);
            boolean selected = selectedPackages.contains(entry.packageName);
            row.setBackground(pillBg(
                selected ? COL_SURFACE_SEL : COL_SURFACE,
                selected ? COL_PRIMARY : COL_OUTLINE,
                16));
            iconView.setImageDrawable(entry.icon);
            nameView.setText(entry.isGame ? entry.label + "  \u2022  GAME" : entry.label);
            pkgView.setText(entry.packageName);
            checkBox.setChecked(selected);
            return row;
        }
    }
}
