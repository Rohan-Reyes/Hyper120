package com.custom.game120hz;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

/** Hyper120 home: target refresh rate, background settings and the app picker. */
public class MainActivity extends Activity {
    private static final int FILTER_ALL = 0;
    private static final int FILTER_SELECTED = 1;
    private static final int FILTER_GAMES = 2;

    private static class AppEntry {
        final String label;
        final String packageName;
        final Drawable icon;
        final boolean isGame;
        View row;
        FrameLayout check;

        AppEntry(String label, String packageName, Drawable icon, boolean isGame) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
            this.isGame = isGame;
        }
    }

    private final List<AppEntry> allApps = new ArrayList<>();
    private Set<String> selectedPackages;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable resetBtnRunnable = this::resetApplyButton;
    private final Deque<String> logLines = new ArrayDeque<>();
    private final List<TextView> segments = new ArrayList<>();
    private final TextView[] filterChips = new TextView[3];

    private TextView statusChip;
    private TextView appCountText;
    private TextView hzNumber;
    private TextView applyBtn;
    private TextView lastApplied;
    private LinearLayout logCard;
    private TextView logText;
    private TextView logTileSub;
    private TextView selectedCountText;
    private LinearLayout listCard;
    private TextView emptyText;
    private Switch autoSwitch;
    private int filter = FILTER_ALL;
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);
        selectedPackages = FpsApplier.syncInstalledGames(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(buildHeader());
        content.addView(buildHero());
        content.addView(buildSettings());
        content.addView(buildTools());
        content.addView(buildLog());
        content.addView(buildAppsSection());

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiHelper.COL_BG);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(content);
        UiHelper.applyNotchSafePadding(this, scroll, 16, 12, 24);
        setContentView(scroll);

        updateSegments();
        updateStatusChip(FpsApplier.isPowerKeeperInstalled(this));
        applyNow(false, "App started");
        syncServiceState(FpsApplier.isAutoEnabled(this));
        FpsApplier.pingActiveUser(this);
        loadInstalledAppsAsync();
    }

    // ---------------------------------------------------------------- header

    private View buildHeader() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(2), 0, 0, dp(6));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(42));
        lp.rightMargin = dp(12);
        bar.addView(logo, lp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        col.addView(UiHelper.text(this, "Hyper120", 22, UiHelper.COL_TEXT, UiHelper.TF_BOLD));
        TextView sub = UiHelper.text(this, deviceLine(), 12.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        sub.setPadding(0, dp(5), 0, 0);
        col.addView(sub);
        bar.addView(col);

        View tester = UiHelper.iconButton(this, R.drawable.ui_gauge,
            v -> startActivity(new Intent(this, FpsTesterActivity.class)));
        ((LinearLayout.LayoutParams) tester.getLayoutParams()).rightMargin = dp(8);
        bar.addView(tester);
        bar.addView(UiHelper.iconButton(this, R.drawable.ui_more,
            v -> startActivity(new Intent(this, AboutActivity.class))));
        return bar;
    }

    private String deviceLine() {
        return FpsApplier.getDeviceName(this) + " · Android " + android.os.Build.VERSION.RELEASE
            + " · up to " + FpsApplier.getMaxSupportedFps(this) + "Hz";
    }

    // ---------------------------------------------------------------- hero

    private View buildHero() {
        LinearLayout hero = UiHelper.card(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF0E2233, UiHelper.COL_SURFACE, 0xFF141530});
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(1), UiHelper.alpha(UiHelper.COL_PRIMARY, 0.22f));
        hero.setBackground(bg);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        statusChip = UiHelper.chip(this, "", UiHelper.COL_TEAL);
        top.addView(statusChip);
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        appCountText = UiHelper.text(this, "", 13, UiHelper.COL_MUTED, UiHelper.TF_MEDIUM);
        top.addView(appCountText);
        hero.addView(top);

        LinearLayout numRow = new LinearLayout(this);
        numRow.setOrientation(LinearLayout.HORIZONTAL);
        numRow.setGravity(Gravity.BOTTOM);
        numRow.setPadding(0, dp(18), 0, 0);
        hzNumber = UiHelper.text(this, "", 76, UiHelper.COL_TEXT, UiHelper.TF_DISPLAY);
        hzNumber.setLetterSpacing(-0.03f);
        float h = hzNumber.getTextSize();
        hzNumber.getPaint().setShader(new LinearGradient(0, 0, 0, h,
            new int[]{0xFFFFFFFF, 0xFFBAE6FD, UiHelper.COL_PRIMARY}, null, Shader.TileMode.CLAMP));
        numRow.addView(hzNumber);
        TextView hz = UiHelper.text(this, "Hz", 24, UiHelper.COL_MUTED, UiHelper.TF_BOLD);
        hz.setPadding(dp(6), 0, 0, dp(10));
        numRow.addView(hz);
        hero.addView(numRow);

        TextView caption = UiHelper.text(this, "Target refresh rate for your selected games",
            13.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        caption.setPadding(0, dp(6), 0, dp(18));
        hero.addView(caption);

        // Segmented control
        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setBackground(UiHelper.pillBg(this, UiHelper.alpha(0xFF000000, 0.28f), UiHelper.COL_OUTLINE, 16));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        segments.clear();
        for (int fps : FpsApplier.detectSupportedFps(this)) {
            TextView s = UiHelper.text(this, String.valueOf(fps), 15, UiHelper.COL_MUTED, UiHelper.TF_BOLD);
            s.setGravity(Gravity.CENTER);
            s.setTag(fps);
            s.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                FpsApplier.setTargetFps(this, fps);
                updateSegments();
                if (FpsApplier.isAutoEnabled(this)) syncServiceState(true);
                applyNow(true, "Target " + fps + "Hz");
                Hyper120TileService.requestTileSync(this);
            });
            seg.addView(s, new LinearLayout.LayoutParams(0, dp(42), 1f));
            segments.add(s);
        }
        hero.addView(seg);

        applyBtn = UiHelper.button(this, "", R.drawable.ui_zap, true, v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            applyNow(true, "Manual tap");
        });
        ((LinearLayout.LayoutParams) applyBtn.getLayoutParams()).topMargin = dp(12);
        hero.addView(applyBtn);

        lastApplied = UiHelper.text(this, "", 12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        lastApplied.setGravity(Gravity.CENTER);
        lastApplied.setPadding(0, dp(12), 0, 0);
        hero.addView(lastApplied, UiHelper.matchWrap(this, 0));
        return hero;
    }

    private void updateSegments() {
        int active = FpsApplier.getTargetFps(this);
        hzNumber.setText(String.valueOf(active));
        for (TextView s : segments) {
            boolean on = (Integer) s.getTag() == active;
            s.setTextColor(on ? UiHelper.COL_ON_PRIMARY : UiHelper.COL_MUTED);
            s.setBackground(on
                ? UiHelper.gradientBg(this, 12, UiHelper.COL_PRIMARY, 0xFF818CF8)
                : UiHelper.ripple(this, null, 12));
        }
        resetApplyButton();
    }

    private void updateStatusChip(boolean pkFound) {
        if (!pkFound) {
            UiHelper.setChip(this, statusChip, "PowerKeeper missing", UiHelper.COL_WARN);
        } else if (FpsApplier.isAutoEnabled(this)) {
            UiHelper.setChip(this, statusChip, "Active", UiHelper.COL_TEAL);
        } else {
            UiHelper.setChip(this, statusChip, "Manual mode", UiHelper.COL_PRIMARY);
        }
    }

    // ---------------------------------------------------------------- settings

    private View buildSettings() {
        LinearLayout card = UiHelper.card(this);
        card.setPadding(dp(14), dp(4), dp(16), dp(4));

        autoSwitch = UiHelper.styledSwitch(this);
        autoSwitch.setChecked(FpsApplier.isAutoEnabled(this));
        LinearLayout autoRow = UiHelper.row(this, R.drawable.ui_zap, UiHelper.COL_TEAL,
            "Background override", "Re-sends every 15 s while the screen is on", autoSwitch, null);
        autoRow.setOnClickListener(v -> autoSwitch.toggle());
        autoRow.setBackground(UiHelper.ripple(this, null, 14));
        autoSwitch.setOnCheckedChangeListener((b, on) -> {
            FpsApplier.setAutoEnabled(this, on);
            syncServiceState(on);
            updateStatusChip(FpsApplier.isPowerKeeperInstalled(this));
            Hyper120TileService.requestTileSync(this);
            appendLog("Background override " + (on ? "on" : "off"));
        });
        card.addView(autoRow);
        card.addView(UiHelper.divider(this, 58));

        Switch addSwitch = UiHelper.styledSwitch(this);
        addSwitch.setChecked(FpsApplier.isAutoAddGamesEnabled(this));
        LinearLayout addRow = UiHelper.row(this, R.drawable.ui_gamepad, 0xFF818CF8,
            "Auto-add games", "Newly installed games are added for you", addSwitch, null);
        addRow.setOnClickListener(v -> addSwitch.toggle());
        addRow.setBackground(UiHelper.ripple(this, null, 14));
        addSwitch.setOnCheckedChangeListener((b, on) -> {
            FpsApplier.setAutoAddGamesEnabled(this, on);
            if (on) {
                selectedPackages = FpsApplier.syncInstalledGames(this);
                applyNow(false, "Auto-add on");
                loadInstalledAppsAsync();
            } else {
                appendLog("Auto-add games off");
            }
        });
        card.addView(addRow);
        return card;
    }

    // ---------------------------------------------------------------- tools + log

    private View buildTools() {
        LinearLayout rowL = new LinearLayout(this);
        rowL.setOrientation(LinearLayout.HORIZONTAL);
        rowL.setLayoutParams(UiHelper.matchWrap(this, 12));

        View tester = toolTile(R.drawable.ui_gauge, UiHelper.COL_PRIMARY, "FPS tester",
            "Check it works", v -> startActivity(new Intent(this, FpsTesterActivity.class)));
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        a.rightMargin = dp(6);
        rowL.addView(tester, a);

        View log = toolTile(R.drawable.ui_terminal, UiHelper.COL_MUTED, "Activity log",
            "Show broadcasts", v -> toggleLog());
        logTileSub = UiHelper.subtitleOf(log);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b.leftMargin = dp(6);
        rowL.addView(log, b);
        return rowL;
    }

    private View toolTile(int icon, int color, String title, String sub, View.OnClickListener l) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(16), dp(16), dp(16), dp(16));
        t.setBackground(UiHelper.ripple(this, UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 22), 22));
        FrameLayout badge = UiHelper.iconBadge(this, icon, color);
        ((LinearLayout.LayoutParams) badge.getLayoutParams()).rightMargin = 0;
        ((LinearLayout.LayoutParams) badge.getLayoutParams()).bottomMargin = dp(14);
        t.addView(badge);
        t.addView(UiHelper.text(this, title, 15, UiHelper.COL_TEXT, UiHelper.TF_MEDIUM));
        TextView s = UiHelper.text(this, sub, 12.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        s.setTag("subtitle");
        s.setPadding(0, dp(5), 0, 0);
        t.addView(s);
        t.setOnClickListener(l);
        return t;
    }

    private View buildLog() {
        logCard = UiHelper.card(this);
        logCard.setBackground(UiHelper.pillBg(this, 0xFF070A10, UiHelper.COL_OUTLINE, 20));
        logCard.setPadding(dp(16), dp(14), dp(16), dp(16));
        logCard.addView(UiHelper.overline(this, "PowerKeeper broadcasts", UiHelper.COL_DIM));
        logText = UiHelper.text(this, "", 11.5f, 0xFF9FB3C8, Typeface.MONOSPACE);
        logText.setLineSpacing(dp(4), 1f);
        logText.setPadding(0, dp(10), 0, 0);
        logCard.addView(logText);
        logCard.setVisibility(View.GONE);
        return logCard;
    }

    private void toggleLog() {
        boolean show = logCard.getVisibility() != View.VISIBLE;
        logCard.setVisibility(show ? View.VISIBLE : View.GONE);
        logTileSub.setText(show ? "Hide broadcasts" : "Show broadcasts");
        if (show) {
            logCard.setAlpha(0f);
            logCard.setTranslationY(-dp(8));
            logCard.animate().alpha(1f).translationY(0).setDuration(200).start();
        }
    }

    // ---------------------------------------------------------------- apps

    private View buildAppsSection() {
        LinearLayout sec = new LinearLayout(this);
        sec.setOrientation(LinearLayout.VERTICAL);
        sec.setLayoutParams(UiHelper.matchWrap(this, 26));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.BOTTOM);
        head.setPadding(dp(4), 0, dp(4), 0);
        TextView title = UiHelper.text(this, "Apps", 20, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        selectedCountText = UiHelper.text(this, "", 13, UiHelper.COL_PRIMARY, UiHelper.TF_MEDIUM);
        head.addView(selectedCountText);
        sec.addView(head);

        // Search field
        LinearLayout search = new LinearLayout(this);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 18));
        search.setPadding(dp(16), 0, dp(12), 0);
        search.setLayoutParams(UiHelper.matchWrap(this, 12));
        search.addView(UiHelper.icon(this, R.drawable.ui_search, UiHelper.COL_DIM, 18));
        EditText box = new EditText(this);
        box.setHint("Search apps or package names");
        box.setHintTextColor(UiHelper.COL_DIM);
        box.setTextColor(UiHelper.COL_TEXT);
        box.setTextSize(14.5f);
        box.setSingleLine(true);
        box.setBackground(null);
        box.setPadding(dp(12), dp(14), 0, dp(14));
        box.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                refreshList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        search.addView(box, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sec.addView(search);

        // Filter chips
        LinearLayout chips = new LinearLayout(this);
        chips.setLayoutParams(UiHelper.matchWrap(this, 12));
        String[] names = {"All", "Selected", "Games"};
        for (int i = 0; i < 3; i++) {
            final int f = i;
            TextView c = UiHelper.text(this, names[i], 13, UiHelper.COL_MUTED, UiHelper.TF_MEDIUM);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(14), 0, dp(14), 0);
            c.setOnClickListener(v -> {
                filter = f;
                updateFilterChips();
                refreshList();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            lp.rightMargin = dp(8);
            chips.addView(c, lp);
            filterChips[i] = c;
        }
        sec.addView(chips);

        // Explains why some games stay at 60 even when the override works
        LinearLayout note = new LinearLayout(this);
        note.setGravity(Gravity.TOP);
        note.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_WARN, 0.06f),
            UiHelper.alpha(UiHelper.COL_WARN, 0.22f), 18));
        note.setPadding(dp(14), dp(12), dp(14), dp(12));
        note.setLayoutParams(UiHelper.matchWrap(this, 12));
        ImageView ni = UiHelper.icon(this, R.drawable.ui_info, UiHelper.COL_WARN, 18);
        ((LinearLayout.LayoutParams) ni.getLayoutParams()).rightMargin = dp(10);
        note.addView(ni);
        TextView nt = UiHelper.text(this,
            "Hyper120 unlocks the screen, but each game has its own limit. Games tagged 60 MAX stop at 60 FPS "
                + "whatever you do. Turn on the high frame-rate option inside the game too. Long-press a game for tips.",
            12.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        nt.setLineSpacing(dp(3), 1f);
        note.addView(nt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sec.addView(note);

        listCard = UiHelper.card(this);
        listCard.setPadding(dp(14), dp(4), dp(14), dp(4));
        emptyText = UiHelper.text(this, "Loading installed apps…", 14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(0, dp(28), 0, dp(28));
        listCard.addView(emptyText, UiHelper.matchWrap(this, 0));
        sec.addView(listCard);

        TextView hint = UiHelper.text(this,
            "Tip: in App info, turn on Autostart and set Battery saver to No restrictions so HyperOS keeps Hyper120 running.",
            12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        hint.setLineSpacing(dp(3), 1f);
        hint.setPadding(dp(6), dp(16), dp(6), 0);
        sec.addView(hint);

        updateFilterChips();
        return sec;
    }

    private void updateFilterChips() {
        int games = 0;
        for (AppEntry e : allApps) if (e.isGame) games++;
        int[] counts = {allApps.size(), selectedPackages.size(), games};
        String[] names = {"All", "Selected", "Games"};
        for (int i = 0; i < 3; i++) {
            TextView c = filterChips[i];
            boolean on = i == filter;
            c.setText(allApps.isEmpty() ? names[i] : names[i] + "  " + counts[i]);
            c.setTextColor(on ? UiHelper.COL_PRIMARY : UiHelper.COL_MUTED);
            c.setBackground(UiHelper.ripple(this, UiHelper.pillBg(this,
                on ? UiHelper.alpha(UiHelper.COL_PRIMARY, 0.14f) : UiHelper.COL_SURFACE,
                on ? UiHelper.alpha(UiHelper.COL_PRIMARY, 0.45f) : UiHelper.COL_OUTLINE, 99), 99));
        }
        selectedCountText.setText(selectedPackages.size() + " selected");
    }

    private void loadInstalledAppsAsync() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<AppEntry> list = new ArrayList<>();
            for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
                if (getPackageName().equals(ai.packageName)) continue;
                list.add(new AppEntry(pm.getApplicationLabel(ai).toString(), ai.packageName,
                    pm.getApplicationIcon(ai), FpsApplier.isGameApp(ai)));
            }
            Collections.sort(list, (a, b) -> {
                boolean as = selectedPackages.contains(a.packageName);
                boolean bs = selectedPackages.contains(b.packageName);
                if (as != bs) return as ? -1 : 1;
                if (a.isGame != b.isGame) return a.isGame ? -1 : 1;
                return a.label.compareToIgnoreCase(b.label);
            });
            ui.post(() -> {
                allApps.clear();
                allApps.addAll(list);
                updateFilterChips();
                refreshList();
            });
        }).start();
    }

    private void refreshList() {
        listCard.removeAllViews();
        int shown = 0;
        for (AppEntry e : allApps) {
            if (filter == FILTER_SELECTED && !selectedPackages.contains(e.packageName)) continue;
            if (filter == FILTER_GAMES && !e.isGame) continue;
            if (!query.isEmpty()
                && !e.label.toLowerCase(Locale.ROOT).contains(query)
                && !e.packageName.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (e.row == null) buildRow(e);
            if (shown > 0) listCard.addView(UiHelper.divider(this, 56));
            if (e.row.getParent() != null) ((ViewGroup) e.row.getParent()).removeView(e.row);
            listCard.addView(e.row);
            shown++;
        }
        if (shown == 0) {
            emptyText.setText(allApps.isEmpty() ? "Loading installed apps…" : "No apps match");
            listCard.addView(emptyText);
        }
    }

    private void buildRow(AppEntry e) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), dp(2), dp(12));
        row.setBackground(UiHelper.ripple(this, null, 14));

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(e.icon);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(42), dp(42));
        ip.rightMargin = dp(14);
        row.addView(iv, ip);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView name = UiHelper.text(this, e.label, 15.5f, UiHelper.COL_TEXT, UiHelper.TF_MEDIUM);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(name);

        SpannableStringBuilder sb = new SpannableStringBuilder();
        GameCaps.Info caps = GameCaps.get(e.packageName);
        String tag = null;
        int tagColor = UiHelper.COL_TEAL;
        if (caps != null) {
            if (caps.cap == GameCaps.MAX_60) {
                tag = "60 MAX";
                tagColor = UiHelper.COL_WARN;
            } else if (caps.cap == GameCaps.MAX_90) {
                tag = "90 MAX";
                tagColor = UiHelper.COL_PRIMARY;
            } else {
                tag = "UP TO 120";
            }
        } else if (e.isGame) {
            tag = "GAME";
        }
        if (tag != null) {
            sb.append(tag);
            sb.setSpan(new ForegroundColorSpan(tagColor), 0, tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append("  ·  ");
        }
        sb.append(e.packageName);
        TextView pkg = UiHelper.text(this, sb, 12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        pkg.setSingleLine(true);
        pkg.setEllipsize(TextUtils.TruncateAt.END);
        pkg.setPadding(0, dp(5), 0, 0);
        col.addView(pkg);
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout check = new FrameLayout(this);
        ImageView tick = UiHelper.icon(this, R.drawable.ui_check, UiHelper.COL_ON_PRIMARY, 16);
        check.addView(tick, new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(26), dp(26));
        cp.leftMargin = dp(12);
        row.addView(check, cp);

        e.row = row;
        e.check = check;
        styleCheck(e, false);
        row.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            String msg = caps != null ? e.label + ": " + caps.note
                : e.label + ": no data for this game yet. If it has a frame-rate setting, pick the highest; "
                    + "if it still feels like 60, the game itself is probably capped.";
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
            return true;
        });
        row.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            boolean nowOn = !selectedPackages.contains(e.packageName);
            if (nowOn) selectedPackages.add(e.packageName);
            else selectedPackages.remove(e.packageName);
            FpsApplier.setPackageIgnored(this, e.packageName, !nowOn);
            FpsApplier.savePackages(this, selectedPackages);
            appendLog((nowOn ? "Added " : "Removed ") + e.packageName);
            styleCheck(e, true);
            updateFilterChips();
            applyNow(true, "Selection updated");
        });
    }

    private void styleCheck(AppEntry e, boolean animate) {
        boolean on = selectedPackages.contains(e.packageName);
        GradientDrawable d;
        if (on) {
            d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{UiHelper.COL_PRIMARY, 0xFF818CF8});
        } else {
            d = new GradientDrawable();
            d.setColor(0);
            d.setStroke(dp(2), 0xFF323B4E);
        }
        d.setShape(GradientDrawable.OVAL);
        e.check.setBackground(d);
        e.check.getChildAt(0).setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        if (animate) {
            e.check.setScaleX(0.7f);
            e.check.setScaleY(0.7f);
            e.check.animate().scaleX(1f).scaleY(1f).setDuration(260)
                .setInterpolator(new OvershootInterpolator(2.5f)).start();
        }
    }

    // ---------------------------------------------------------------- apply

    private void syncServiceState(boolean enabled) {
        Intent svc = new Intent(this, Auto120Service.class);
        if (enabled) startForegroundService(svc);
        else stopService(svc);
    }

    private void applyNow(boolean animateBtn, String reason) {
        FpsApplier.ApplyResult res = FpsApplier.apply120Hz(this, selectedPackages);
        updateStatusChip(res.powerKeeperFound);
        appCountText.setText(res.count + (res.count == 1 ? " app" : " apps"));
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        lastApplied.setText(res.powerKeeperFound
            ? "Last sent " + res.fps + "Hz to " + res.count + " apps at " + ts
            : "PowerKeeper not found — this ROM may not support the override");

        String preview = "";
        if (!res.packages.isEmpty()) {
            int show = Math.min(2, res.packages.size());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < show; i++) {
                if (i > 0) sb.append(", ");
                sb.append(res.packages.get(i));
            }
            if (res.packages.size() > show) sb.append(" +").append(res.packages.size() - show);
            preview = " [" + sb + "]";
        }
        appendLog(reason + ": " + res.fps + "Hz → " + res.count + " apps"
            + (res.powerKeeperFound ? "" : " (PowerKeeper missing)") + preview);

        if (animateBtn) animateApplyButton(res);
    }

    private void animateApplyButton(FpsApplier.ApplyResult res) {
        ui.removeCallbacks(resetBtnRunnable);
        applyBtn.animate().cancel();
        applyBtn.setScaleX(0.96f);
        applyBtn.setScaleY(0.96f);
        applyBtn.animate().scaleX(1f).scaleY(1f).setDuration(240)
            .setInterpolator(new DecelerateInterpolator()).start();
        if (res.powerKeeperFound) {
            applyBtn.setText("Sent " + res.fps + "Hz to " + res.count + " apps");
            setButtonIcon(R.drawable.ui_check);
            applyBtn.setBackground(UiHelper.ripple(this,
                UiHelper.gradientBg(this, 18, UiHelper.COL_TEAL, 0xFF5EEAD4), 18));
        } else {
            applyBtn.setText("PowerKeeper not found");
            applyBtn.setBackground(UiHelper.ripple(this,
                UiHelper.gradientBg(this, 18, UiHelper.COL_WARN, 0xFFFCD34D), 18));
        }
        ui.postDelayed(resetBtnRunnable, 1500);
    }

    private void resetApplyButton() {
        applyBtn.setText("Apply " + FpsApplier.getTargetFps(this) + "Hz now");
        setButtonIcon(R.drawable.ui_zap);
        UiHelper.styleButton(this, applyBtn, true);
    }

    private void setButtonIcon(int res) {
        Drawable d = getDrawable(res).mutate();
        d.setTint(UiHelper.COL_ON_PRIMARY);
        d.setBounds(0, 0, dp(18), dp(18));
        applyBtn.setCompoundDrawables(d, null, null, null);
    }

    private void appendLog(String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        logLines.addLast(ts + "  " + msg);
        while (logLines.size() > 6) logLines.removeFirst();
        logText.setText(TextUtils.join("\n", logLines));
    }

    private int dp(float v) {
        return UiHelper.dp(this, v);
    }
}
