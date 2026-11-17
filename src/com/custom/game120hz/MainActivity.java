package com.custom.game120hz;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Material-styled UI for Hyper120 with notch-safe spacing, app picker, and 3-dot menu. */
public class MainActivity extends Activity {
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
    private Button applyBtn;
    private TextView statusText;
    private TextView logToggleBtn;
    private TextView logText;
    private boolean logVisible = false;
    private final List<TextView> fpsPills = new ArrayList<>();
    private final Deque<String> logLines = new ArrayDeque<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetBtnRunnable = this::resetApplyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);

        selectedPackages = FpsApplier.syncInstalledGames(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COL_BG);
        UiHelper.applyNotchSafePadding(this, root, 16, 18, 12);

        // Top Material App Bar (pushed comfortably below drop/punch-hole notch)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(4), dp(8), dp(4), dp(14));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("Hyper120");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(UiHelper.COL_TEXT);
        titleCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(FpsApplier.getDeviceSummary(this));
        subtitle.setTextSize(11.5f);
        subtitle.setTextColor(UiHelper.COL_MUTED);
        titleCol.addView(subtitle);
        topBar.addView(titleCol);

        TextView menuBtn = new TextView(this);
        menuBtn.setText("\u22EE");
        menuBtn.setTextSize(24);
        menuBtn.setTypeface(Typeface.DEFAULT_BOLD);
        menuBtn.setTextColor(UiHelper.COL_TEXT);
        menuBtn.setGravity(Gravity.CENTER);
        menuBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 22));
        menuBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        menuBtn.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        topBar.addView(menuBtn);

        root.addView(topBar);

        // Material Control Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 20));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setElevation(dp(4));

        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView toggleLabel = new TextView(this);
        toggleLabel.setText("Auto-override in background");
        toggleLabel.setTextColor(UiHelper.COL_TEXT);
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
            Hyper120TileService.requestTileSync(this);
        });
        toggleRow.addView(autoSwitch);
        card.addView(toggleRow);

        LinearLayout autoAddRow = new LinearLayout(this);
        autoAddRow.setOrientation(LinearLayout.HORIZONTAL);
        autoAddRow.setGravity(Gravity.CENTER_VERTICAL);
        autoAddRow.setPadding(0, dp(8), 0, 0);

        TextView autoAddLabel = new TextView(this);
        autoAddLabel.setText("Auto-add installed games");
        autoAddLabel.setTextColor(UiHelper.COL_TEXT);
        autoAddLabel.setTextSize(15);
        autoAddLabel.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        autoAddRow.addView(autoAddLabel);

        Switch autoAddSwitch = new Switch(this);
        autoAddSwitch.setChecked(FpsApplier.isAutoAddGamesEnabled(this));
        autoAddSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            FpsApplier.setAutoAddGamesEnabled(this, isChecked);
            if (isChecked) {
                selectedPackages = FpsApplier.syncInstalledGames(this);
                loadInstalledApps();
                filterApps("");
                applyNow(false, "Auto-add ON");
            } else {
                appendLog("Auto-add games disabled");
            }
        });
        autoAddRow.addView(autoAddSwitch);
        card.addView(autoAddRow);

        // Target FPS selector row
        LinearLayout fpsRow = new LinearLayout(this);
        fpsRow.setOrientation(LinearLayout.HORIZONTAL);
        fpsRow.setGravity(Gravity.CENTER_VERTICAL);
        fpsRow.setPadding(0, dp(10), 0, 0);

        TextView fpsLabel = new TextView(this);
        fpsLabel.setText("Target FPS");
        fpsLabel.setTextColor(UiHelper.COL_TEXT);
        fpsLabel.setTextSize(15);
        fpsLabel.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        fpsRow.addView(fpsLabel);

        fpsPills.clear();
        for (int fps : FpsApplier.detectSupportedFps(this)) {
            TextView pill = new TextView(this);
            pill.setText(fps + "Hz");
            pill.setTag(fps);
            pill.setTextSize(12);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(dp(11), dp(6), dp(11), dp(6));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pp.leftMargin = dp(6);
            pill.setLayoutParams(pp);
            pill.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                FpsApplier.setTargetFps(this, fps);
                updateFpsPills();
                resetApplyButton();
                if (FpsApplier.isAutoEnabled(this)) {
                    syncServiceState(true);
                }
                applyNow(true, "Target " + fps + "Hz");
                Hyper120TileService.requestTileSync(this);
            });
            fpsPills.add(pill);
            fpsRow.addView(pill);
        }
        card.addView(fpsRow);
        updateFpsPills();

        applyBtn = new Button(this);
        applyBtn.setText("Apply " + FpsApplier.getTargetFps(this) + "Hz Now");
        applyBtn.setAllCaps(false);
        applyBtn.setTextSize(15);
        applyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        applyBtn.setTextColor(UiHelper.COL_ON_PRIMARY);
        applyBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_PRIMARY, 0, 26));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        btnParams.topMargin = dp(12);
        applyBtn.setLayoutParams(btnParams);
        applyBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            applyNow(true, "Manual tap");
        });
        card.addView(applyBtn);

        statusText = new TextView(this);
        statusText.setTextColor(UiHelper.COL_TEAL);
        statusText.setTextSize(12);
        statusText.setPadding(dp(2), dp(10), dp(2), 0);
        card.addView(statusText);

        LinearLayout toolsRow = new LinearLayout(this);
        toolsRow.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow.setPadding(0, dp(8), 0, 0);

        TextView testFpsBtn = new TextView(this);
        testFpsBtn.setText("Test FPS");
        testFpsBtn.setTextColor(UiHelper.COL_TEAL);
        testFpsBtn.setTextSize(12);
        testFpsBtn.setTypeface(Typeface.DEFAULT_BOLD);
        testFpsBtn.setGravity(Gravity.CENTER);
        testFpsBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_BG, UiHelper.COL_OUTLINE, 16));
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(
            0, dp(34), 1f);
        testParams.rightMargin = dp(6);
        testFpsBtn.setLayoutParams(testParams);
        testFpsBtn.setOnClickListener(v -> startActivity(new Intent(this, FpsTesterActivity.class)));
        toolsRow.addView(testFpsBtn);

        logToggleBtn = new TextView(this);
        logToggleBtn.setText("Show Terminal");
        logToggleBtn.setTextColor(UiHelper.COL_MUTED);
        logToggleBtn.setTextSize(12);
        logToggleBtn.setTypeface(Typeface.DEFAULT_BOLD);
        logToggleBtn.setGravity(Gravity.CENTER);
        logToggleBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_BG, UiHelper.COL_OUTLINE, 16));
        LinearLayout.LayoutParams logBtnParams = new LinearLayout.LayoutParams(
            0, dp(34), 1f);
        logBtnParams.leftMargin = dp(6);
        logToggleBtn.setLayoutParams(logBtnParams);
        logToggleBtn.setOnClickListener(v -> {
            logVisible = !logVisible;
            logToggleBtn.setText(logVisible ? "Hide Terminal" : "Show Terminal");
            logToggleBtn.setTextColor(logVisible ? UiHelper.COL_PRIMARY : UiHelper.COL_MUTED);
            if (logVisible) {
                logText.setVisibility(View.VISIBLE);
                logText.setAlpha(0f);
                logText.animate().alpha(1f).setDuration(180).start();
            } else {
                logText.setVisibility(View.GONE);
            }
        });
        toolsRow.addView(logToggleBtn);
        card.addView(toolsRow);

        logText = new TextView(this);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextColor(UiHelper.COL_MUTED);
        logText.setTextSize(10.5f);
        logText.setLineSpacing(dp(2), 1f);
        logText.setBackground(UiHelper.pillBg(this, UiHelper.COL_BG, UiHelper.COL_OUTLINE, 12));
        logText.setPadding(dp(10), dp(8), dp(10), dp(8));
        logText.setVisibility(View.GONE);
        LinearLayout.LayoutParams logParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        logParams.topMargin = dp(8);
        logText.setLayoutParams(logParams);
        card.addView(logText);

        root.addView(card);

        // Material Search Pill
        EditText searchBox = new EditText(this);
        searchBox.setHint("Search apps or packages...");
        searchBox.setHintTextColor(UiHelper.COL_MUTED);
        searchBox.setTextColor(UiHelper.COL_TEXT);
        searchBox.setTextSize(14);
        searchBox.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 26));
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
                FpsApplier.setPackageIgnored(this, entry.packageName, true);
                appendLog("Removed " + entry.packageName);
            } else {
                selectedPackages.add(entry.packageName);
                FpsApplier.setPackageIgnored(this, entry.packageName, false);
                appendLog("Added " + entry.packageName);
            }
            FpsApplier.savePackages(this, selectedPackages);
            adapter.notifyDataSetChanged();
            applyNow(true, "Selection updated");
        });
        root.addView(listView);

        setContentView(root);

        loadInstalledApps();
        filterApps("");
        applyNow(false, "App started");
        syncServiceState(autoOn);
        FpsApplier.pingActiveUser(this);
    }

    private void syncServiceState(boolean enabled) {
        Intent svc = new Intent(this, Auto120Service.class);
        if (enabled) {
            startForegroundService(svc);
        } else {
            stopService(svc);
        }
    }

    private void applyNow(boolean animateBtn, String reason) {
        FpsApplier.ApplyResult res = FpsApplier.apply120Hz(this, selectedPackages);
        String pkTag = res.powerKeeperFound ? "PowerKeeper OK" : "PowerKeeper missing!";
        statusText.setText(res.fps + "Hz active for " + res.count + " app(s) \u00B7 " + pkTag);
        statusText.setAlpha(0.35f);
        statusText.animate().alpha(1f).setDuration(260).start();

        String preview = "";
        if (!res.packages.isEmpty()) {
            int show = Math.min(2, res.packages.size());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < show; i++) {
                if (i > 0) sb.append(", ");
                sb.append(res.packages.get(i));
            }
            if (res.packages.size() > show) {
                sb.append(" +").append(res.packages.size() - show);
            }
            preview = " [" + sb + "]";
        }
        appendLog(reason + ": sent " + res.fps + "Hz to " + res.count + " app(s) (" + pkTag + ")" + preview);

        if (animateBtn) {
            animateApplyButton(res.count, res.fps, res.powerKeeperFound);
        }
    }

    private void animateApplyButton(int count, int fps, boolean pkFound) {
        uiHandler.removeCallbacks(resetBtnRunnable);
        applyBtn.animate().cancel();
        applyBtn.setScaleX(0.94f);
        applyBtn.setScaleY(0.94f);
        applyBtn.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220)
            .setInterpolator(new DecelerateInterpolator())
            .start();

        applyBtn.setText(pkFound ? ("\u2713 Applied " + fps + "Hz (" + count + ")") : "PowerKeeper not found");
        applyBtn.setBackground(UiHelper.pillBg(this, pkFound ? UiHelper.COL_TEAL : UiHelper.COL_PRIMARY, 0, 26));
        uiHandler.postDelayed(resetBtnRunnable, 1400);
    }

    private void resetApplyButton() {
        applyBtn.setText("Apply " + FpsApplier.getTargetFps(this) + "Hz Now");
        applyBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_PRIMARY, 0, 26));
    }

    private void updateFpsPills() {
        int activeFps = FpsApplier.getTargetFps(this);
        for (TextView pill : fpsPills) {
            int fps = (Integer) pill.getTag();
            boolean selected = (fps == activeFps);
            pill.setTextColor(selected ? UiHelper.COL_ON_PRIMARY : UiHelper.COL_MUTED);
            pill.setBackground(UiHelper.pillBg(
                this,
                selected ? UiHelper.COL_PRIMARY : UiHelper.COL_BG,
                selected ? 0 : UiHelper.COL_OUTLINE,
                16));
        }
    }

    private void appendLog(String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        logLines.addLast("[" + ts + "] " + msg);
        while (logLines.size() > 4) {
            logLines.removeFirst();
        }
        StringBuilder sb = new StringBuilder();
        for (String line : logLines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        logText.setText(sb.toString());
        if (logVisible) {
            logText.setAlpha(0.55f);
            logText.animate().alpha(1f).setDuration(220).start();
        }
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
            boolean isGame = FpsApplier.isGameApp(ai);
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
        return UiHelper.dp(this, v);
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
                nameView.setTextColor(UiHelper.COL_TEXT);
                nameView.setTextSize(15);
                nameView.setTypeface(Typeface.DEFAULT_BOLD);
                textCol.addView(nameView);

                pkgView = new TextView(MainActivity.this);
                pkgView.setTextColor(UiHelper.COL_MUTED);
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
            row.setBackground(UiHelper.pillBg(
                MainActivity.this,
                selected ? UiHelper.COL_SURFACE_SEL : UiHelper.COL_SURFACE,
                selected ? UiHelper.COL_PRIMARY : UiHelper.COL_OUTLINE,
                16));
            iconView.setImageDrawable(entry.icon);
            nameView.setText(entry.isGame ? entry.label + "  \u2022  GAME" : entry.label);
            pkgView.setText(entry.packageName);
            checkBox.setChecked(selected);
            return row;
        }
    }
}
