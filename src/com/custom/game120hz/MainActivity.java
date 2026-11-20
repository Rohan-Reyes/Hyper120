package com.custom.game120hz;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.widget.Toast;
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
import java.util.HashSet;
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
        ImageView settings;
        TextView sub;

        AppEntry(String label, String packageName, Drawable icon, boolean isGame) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
            this.isGame = isGame;
        }
    }

    private final List<AppEntry> allApps = new ArrayList<>();
    private boolean appsLoading;
    private boolean appsLoadFailed;
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
    private LinearLayout healthSlot;
    private TextView heroCaption;
    private int checkResultTarget;
    private LinearLayout faqSlot;
    private ScrollView mainScroll;
    private LinearLayout checkBox;
    private OverrideCheck check;
    private boolean serviceWasKilled = false;
    private boolean batteryDismissed = false;
    private int filter = FILTER_ALL;
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);
        selectedPackages = FpsApplier.syncInstalledGames(this);
        GameCaps.init(this);
        FaqData.init(this);
        // A stale heartbeat means HyperOS killed the background service while we were away.
        long beat = FpsApplier.lastServiceBeat(this);
        serviceWasKilled = FpsApplier.isAutoEnabled(this) && beat > 0
            && System.currentTimeMillis() - beat > 60_000L;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(buildHeader());
        healthSlot = new LinearLayout(this);
        healthSlot.setOrientation(LinearLayout.VERTICAL);
        content.addView(healthSlot);
        content.addView(buildHero());
        content.addView(buildSettings());
        content.addView(buildTools());
        content.addView(buildLog());
        content.addView(buildAppsSection());

        ScrollView scroll = new ScrollView(this);
        mainScroll = scroll;
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
        GameCaps.refreshAsync(this, this::rebindTags);
        FaqData.refreshAsync(this, () -> fillFaq(false));
    }

    /** Shows the setup guide until it is finished; reuses the open guide instead of stacking copies. */
    @Override
    protected void onPause() {
        super.onPause();
        if (check != null) {   // don't leave the window pinned to a rate or the button half-filled
            check.cancel();
            check = null;
            resetApplyButton();
        }
    }

    private void ensureOnboarded() {
        if (!FpsApplier.isOnboarded(this)) {
            startActivity(new Intent(this, OnboardingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHealth();
        if (hzNumber != null) updateSegments();
        ensureOnboarded();
        checkForUpdate();
    }

    /** Re-tags rows after a newer game list was downloaded. */
    private void rebindTags() {
        for (AppEntry e : allApps) if (e.sub != null) bindSub(e);
        appendLog("Game list updated (v" + GameCaps.version() + ")");
    }

    // ---------------------------------------------------------------- health

    private void updateHealth() {
        healthSlot.removeAllViews();
        if (FpsApplier.isSystemRefreshTooLow(this)) {
            LinearLayout card = (LinearLayout) healthCard(R.string.health_refresh_title, R.string.health_refresh_body, null);
            LinearLayout actions = (LinearLayout) card.getChildAt(card.getChildCount() - 1);
            actions.removeAllViews();
            actions.addView(actionChip(getString(R.string.health_open_display), UiHelper.COL_WARN,
                v -> UiHelper.openDisplaySettings(this)));
            TextView title = (TextView) ((LinearLayout) card.getChildAt(0)).getChildAt(1);
            title.setText(getString(R.string.health_refresh_title, FpsApplier.getSystemRefreshSetting(this)));
            healthSlot.addView(card);
        } else if (UpdateChecker.available(this) != null && !UpdateChecker.isSnoozed(this)) {
            healthSlot.addView(updateBanner(UpdateChecker.available(this)));
        } else if (serviceWasKilled) {
            healthSlot.addView(healthCard(R.string.health_killed_title, R.string.health_killed_body, () -> {
                serviceWasKilled = false;
                updateHealth();
            }));
        } else if (!batteryDismissed && FpsApplier.isAutoEnabled(this) && !UiHelper.isBatteryUnrestricted(this)) {
            healthSlot.addView(healthCard(R.string.health_battery_title, R.string.health_battery_body, () -> {
                batteryDismissed = true;
                updateHealth();
            }));
        }
    }

    // ---------------------------------------------------------------- updates

    private void checkForUpdate() {
        UpdateChecker.checkAsync(this, () -> {
            if (isFinishing() || isDestroyed()) return;
            updateHealth();
            String v = UpdateChecker.available(this);
            // one-time sheet per version; never on top of the setup guide
            if (v != null && FpsApplier.isOnboarded(this) && UpdateChecker.shouldPrompt(this, v)) {
                UpdateChecker.markPrompted(this, v);
                // runs once the window is attached (the result can arrive before that on launch)
                getWindow().getDecorView().post(() -> {
                    if (!isFinishing() && !isDestroyed()) showUpdateSheet(v);
                });
            }
        });
    }

    private void openUpdate() {
        UiHelper.openUrl(this, UpdateChecker.downloadUrl(this));
        Toast.makeText(this, getString(R.string.upd_install_hint), Toast.LENGTH_LONG).show();
    }

    private View updateBanner(String v) {
        LinearLayout card = UiHelper.card(this);
        card.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_PRIMARY, 0.1f),
            UiHelper.alpha(UiHelper.COL_PRIMARY, 0.45f), 22));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView ic = UiHelper.icon(this, R.drawable.ui_refresh, UiHelper.COL_PRIMARY, 20);
        ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = dp(10);
        head.addView(ic);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(UiHelper.text(this, getString(R.string.upd_banner_title, v), 15, UiHelper.COL_TEXT, UiHelper.TF_BOLD));
        TextView sub = UiHelper.text(this, getString(R.string.upd_kept), 12.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        sub.setPadding(0, dp(4), 0, 0);
        col.addView(sub);
        head.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(head);
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(12), 0, 0);
        TextView now = actionChip(getString(R.string.upd_now), UiHelper.COL_PRIMARY, v2 -> openUpdate());
        now.setTextColor(UiHelper.COL_ON_PRIMARY);
        now.setBackground(UiHelper.ripple(this, UiHelper.gradientBg(this, 99, UiHelper.COL_PRIMARY, 0xFF818CF8), 99));
        actions.addView(now);
        actions.addView(actionChip(getString(R.string.upd_whats_new), UiHelper.COL_PRIMARY, v2 -> showUpdateSheet(v)));
        actions.addView(actionChip(getString(R.string.upd_later), UiHelper.COL_MUTED, v2 -> {
            UpdateChecker.snooze(this);
            updateHealth();
        }));
        card.addView(actions);
        return card;
    }

    private void showUpdateSheet(String v) {
        final android.app.Dialog[] holder = new android.app.Dialog[1];
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);

        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(UiHelper.gradientBg(this, 18, UiHelper.COL_PRIMARY, 0xFF818CF8));
        badge.addView(UiHelper.icon(this, R.drawable.ui_refresh, UiHelper.COL_ON_PRIMARY, 26),
            new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        c.addView(badge, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView t = UiHelper.text(this, getString(R.string.upd_title), 22, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        t.setPadding(0, dp(16), 0, dp(4));
        c.addView(t);
        c.addView(UiHelper.text(this, getString(R.string.upd_versions, v, FpsApplier.appVersion(this)),
            14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR));

        String[] notes = UpdateChecker.notes(this);
        if (notes.length > 0) {
            TextView wn = UiHelper.overline(this, getString(R.string.upd_whats_new), UiHelper.COL_DIM);
            wn.setPadding(0, dp(18), 0, dp(4));
            c.addView(wn);
            for (String n : notes) {
                LinearLayout row = new LinearLayout(this);
                row.setPadding(0, dp(6), 0, 0);
                TextView dot = UiHelper.text(this, "\u2022", 15, UiHelper.COL_PRIMARY, UiHelper.TF_BOLD);
                row.addView(dot, new LinearLayout.LayoutParams(dp(18), ViewGroup.LayoutParams.WRAP_CONTENT));
                TextView nt = UiHelper.text(this, n, 14, UiHelper.COL_TEXT, UiHelper.TF_REGULAR);
                nt.setLineSpacing(dp(2), 1f);
                row.addView(nt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                c.addView(row);
            }
        }

        TextView kept = UiHelper.text(this, getString(R.string.upd_kept), 13, UiHelper.COL_TEAL, UiHelper.TF_MEDIUM);
        kept.setPadding(0, dp(18), 0, 0);
        c.addView(kept);

        TextView now = UiHelper.button(this, getString(R.string.upd_now), R.drawable.ui_refresh, true, x -> {
            holder[0].dismiss();
            openUpdate();
        });
        ((LinearLayout.LayoutParams) now.getLayoutParams()).topMargin = dp(20);
        c.addView(now);
        TextView later = UiHelper.button(this, getString(R.string.upd_later), 0, false, x -> holder[0].dismiss());
        ((LinearLayout.LayoutParams) later.getLayoutParams()).topMargin = dp(6);
        c.addView(later);
        holder[0] = UiHelper.sheet(this, c);
        holder[0].show();
    }

    private View healthCard(int titleRes, int bodyRes, Runnable onDismiss) {
        LinearLayout card = UiHelper.card(this);
        card.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_WARN, 0.08f),
            UiHelper.alpha(UiHelper.COL_WARN, 0.35f), 22));
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView ic = UiHelper.icon(this, R.drawable.ui_info, UiHelper.COL_WARN, 20);
        ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = dp(10);
        head.addView(ic);
        head.addView(UiHelper.text(this, getString(titleRes), 15, UiHelper.COL_TEXT, UiHelper.TF_BOLD),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(head);
        TextView body = UiHelper.text(this, getString(bodyRes), 13, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        body.setLineSpacing(dp(3), 1f);
        body.setPadding(dp(30), dp(8), 0, dp(12));
        card.addView(body);
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(30), 0, 0, 0);
        actions.addView(actionChip(getString(R.string.health_fix_battery), UiHelper.COL_WARN,
            v -> UiHelper.requestBatteryExemption(this)));
        actions.addView(actionChip(getString(R.string.health_autostart), UiHelper.COL_WARN,
            v -> UiHelper.openAutostart(this)));
        actions.addView(actionChip(getString(R.string.health_dismiss), UiHelper.COL_MUTED, v -> onDismiss.run()));
        card.addView(actions);
        return card;
    }

    private TextView actionChip(String label, int color, View.OnClickListener l) {
        TextView c = UiHelper.text(this, label, 13, color, UiHelper.TF_BOLD);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(12), 0, dp(12), 0);
        c.setBackground(UiHelper.ripple(this, UiHelper.pillBg(this, 0, UiHelper.alpha(color, 0.4f), 99), 99));
        c.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.rightMargin = dp(8);
        c.setLayoutParams(lp);
        return c;
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
        col.addView(UiHelper.text(this, getString(R.string.app_name), 22, UiHelper.COL_TEXT, UiHelper.TF_BOLD));
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
        return getString(R.string.main_device_line, FpsApplier.getDeviceName(this),
            android.os.Build.VERSION.RELEASE, FpsApplier.getMaxSupportedFps(this));
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

        heroCaption = UiHelper.text(this, getString(R.string.main_caption),
            13.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        heroCaption.setPadding(0, dp(6), 0, dp(18));
        heroCaption.setLineSpacing(dp(3), 1f);
        hero.addView(heroCaption);

        // Segmented control
        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setBackground(UiHelper.pillBg(this, UiHelper.alpha(0xFF000000, 0.28f), UiHelper.COL_OUTLINE, 16));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        segments.clear();
        for (int fps : FpsApplier.selectableRates(this)) {
            TextView s = UiHelper.text(this, String.valueOf(fps), 15, UiHelper.COL_MUTED, UiHelper.TF_BOLD);
            s.setGravity(Gravity.CENTER);
            s.setTag(fps);
            s.setOnClickListener(v -> {
                if (FpsApplier.isRateLocked(this, fps)) {
                    v.performHapticFeedback(HapticFeedbackConstants.REJECT);
                    android.widget.Toast.makeText(this, getString(R.string.rate_locked,
                        FpsApplier.getSystemRefreshSetting(this), fps), android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                if (check != null) {
                    check.cancel();
                    check = null;
                }
                if (fps > 120) {
                    android.widget.Toast.makeText(this, getString(R.string.main_caption_high, fps),
                        android.widget.Toast.LENGTH_LONG).show();
                }
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
            applyNow(false, "Manual tap");
            startCheck();
        });
        ((LinearLayout.LayoutParams) applyBtn.getLayoutParams()).topMargin = dp(12);
        hero.addView(applyBtn);

        checkBox = new LinearLayout(this);
        checkBox.setOrientation(LinearLayout.VERTICAL);
        checkBox.setVisibility(View.GONE);
        hero.addView(checkBox, UiHelper.matchWrap(this, 12));

        lastApplied = UiHelper.text(this, "", 12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        lastApplied.setGravity(Gravity.CENTER);
        lastApplied.setPadding(0, dp(12), 0, 0);
        hero.addView(lastApplied, UiHelper.matchWrap(this, 0));
        return hero;
    }

    private void updateSegments() {
        int active = FpsApplier.getEffectiveTargetFps(this);
        hzNumber.setText(String.valueOf(active));
        if (checkResultTarget != active) {
            checkResultTarget = 0;
            checkBox.setVisibility(View.GONE);
        }
        for (TextView s : segments) {
            int rate = (Integer) s.getTag();
            boolean on = rate == active;
            boolean locked = FpsApplier.isRateLocked(this, rate);
            s.setText(locked ? "\uD83D\uDD12 " + rate : String.valueOf(rate));
            s.setTextColor(on ? UiHelper.COL_ON_PRIMARY : locked ? UiHelper.COL_DIM : UiHelper.COL_MUTED);
            s.setBackground(on
                ? UiHelper.gradientBg(this, 12, UiHelper.COL_PRIMARY, 0xFF818CF8)
                : UiHelper.ripple(this, null, 12));
        }
        heroCaption.setText(active > 120 ? getString(R.string.main_caption_high, active)
            : getString(R.string.main_caption));
        heroCaption.setTextColor(active > 120 ? UiHelper.COL_WARN : UiHelper.COL_MUTED);
        resetApplyButton();
    }

    private void updateStatusChip(boolean pkFound) {
        if (!pkFound) {
            UiHelper.setChip(this, statusChip, getString(R.string.chip_pk_missing), UiHelper.COL_WARN);
        } else if (FpsApplier.isAutoEnabled(this)) {
            UiHelper.setChip(this, statusChip, getString(R.string.chip_active), UiHelper.COL_TEAL);
        } else {
            UiHelper.setChip(this, statusChip, getString(R.string.chip_manual), UiHelper.COL_PRIMARY);
        }
    }

    // ---------------------------------------------------------------- settings

    private View buildSettings() {
        LinearLayout card = UiHelper.card(this);
        card.setPadding(dp(14), dp(4), dp(16), dp(4));

        autoSwitch = UiHelper.styledSwitch(this);
        autoSwitch.setChecked(FpsApplier.isAutoEnabled(this));
        LinearLayout autoRow = UiHelper.row(this, R.drawable.ui_zap, UiHelper.COL_TEAL,
            getString(R.string.set_bg_title), getString(R.string.set_bg_sub), autoSwitch, null);
        autoRow.setOnClickListener(v -> autoSwitch.toggle());
        autoRow.setBackground(UiHelper.ripple(this, null, 14));
        autoSwitch.setOnCheckedChangeListener((b, on) -> {
            FpsApplier.setAutoEnabled(this, on);
            syncServiceState(on);
            Hyper120Widget.updateAll(this);
            updateStatusChip(FpsApplier.isPowerKeeperInstalled(this));
            Hyper120TileService.requestTileSync(this);
            appendLog("Background override " + (on ? "on" : "off"));
        });
        card.addView(autoRow);
        card.addView(UiHelper.divider(this, 58));

        Switch addSwitch = UiHelper.styledSwitch(this);
        addSwitch.setChecked(FpsApplier.isAutoAddGamesEnabled(this));
        LinearLayout addRow = UiHelper.row(this, R.drawable.ui_gamepad, 0xFF818CF8,
            getString(R.string.set_add_title), getString(R.string.set_add_sub), addSwitch, null);
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

        View tester = toolTile(R.drawable.ui_gauge, UiHelper.COL_PRIMARY, getString(R.string.tool_tester),
            getString(R.string.tool_tester_sub), v -> startActivity(new Intent(this, FpsTesterActivity.class)));
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        a.rightMargin = dp(6);
        rowL.addView(tester, a);

        View log = toolTile(R.drawable.ui_terminal, UiHelper.COL_MUTED, getString(R.string.tool_log),
            getString(R.string.tool_log_show), v -> toggleLog());
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
        logCard.addView(UiHelper.overline(this, getString(R.string.log_title), UiHelper.COL_DIM));
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
        logTileSub.setText(getString(show ? R.string.tool_log_hide : R.string.tool_log_show));
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
        TextView title = UiHelper.text(this, getString(R.string.apps_title), 20, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
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
        box.setHint(getString(R.string.search_hint));
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
        String[] names = {getString(R.string.filter_all), getString(R.string.filter_selected), getString(R.string.filter_games)};
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

        // Short reminder that each game keeps its own limit (details are in the FAQ)
        LinearLayout note = new LinearLayout(this);
        note.setGravity(Gravity.CENTER_VERTICAL);
        note.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_WARN, 0.07f),
            UiHelper.alpha(UiHelper.COL_WARN, 0.3f), 14));
        note.setPadding(dp(12), dp(10), dp(12), dp(10));
        note.setLayoutParams(UiHelper.matchWrap(this, 12));
        ImageView ni = UiHelper.icon(this, R.drawable.ui_info, UiHelper.COL_WARN, 16);
        ((LinearLayout.LayoutParams) ni.getLayoutParams()).rightMargin = dp(10);
        note.addView(ni);
        TextView nt = UiHelper.text(this, getString(R.string.limit_note_short), 12.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        nt.setLineSpacing(dp(2), 1f);
        note.addView(nt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sec.addView(note);
        sec.addView(buildFaqCard());

        listCard = UiHelper.card(this);
        listCard.setPadding(dp(14), dp(4), dp(14), dp(4));
        emptyText = UiHelper.text(this, getString(R.string.loading_apps), 14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(0, dp(28), 0, dp(28));
        listCard.addView(emptyText, UiHelper.matchWrap(this, 0));
        sec.addView(listCard);

        TextView hint = UiHelper.text(this,
            getString(R.string.tip_keep_running),
            12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        hint.setLineSpacing(dp(3), 1f);
        hint.setPadding(dp(6), dp(16), dp(6), 0);
        sec.addView(hint);

        updateFilterChips();
        return sec;
    }

    /** Collapsible FAQ above the app list. Content comes from FaqData (bundled + updated from the website). */
    private View buildFaqCard() {
        faqSlot = new LinearLayout(this);
        faqSlot.setOrientation(LinearLayout.VERTICAL);
        faqSlot.setLayoutParams(UiHelper.matchWrap(this, 12));
        fillFaq(false);
        return faqSlot;
    }

    private void fillFaq(boolean open) {
        if (faqSlot == null) return;
        faqSlot.removeAllViews();
        java.util.List<FaqData.Item> qa = FaqData.items();
        if (qa.isEmpty()) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_PRIMARY, 0.05f),
            UiHelper.alpha(UiHelper.COL_PRIMARY, 0.25f), 18));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(14), dp(13), dp(14), dp(13));
        head.setBackground(UiHelper.ripple(this, null, 18));
        ImageView hi = UiHelper.icon(this, R.drawable.ui_info, UiHelper.COL_PRIMARY, 18);
        ((LinearLayout.LayoutParams) hi.getLayoutParams()).rightMargin = dp(10);
        head.addView(hi);
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.addView(UiHelper.text(this, getString(R.string.faq_title), 14.5f, UiHelper.COL_TEXT, UiHelper.TF_MEDIUM));
        TextView hs = UiHelper.text(this, getString(R.string.faq_sub, qa.size()), 12, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        hs.setPadding(0, dp(3), 0, 0);
        ht.addView(hs);
        head.addView(ht, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView chev = UiHelper.icon(this, R.drawable.ui_chevron, UiHelper.COL_MUTED, 18);
        chev.setRotation(open ? 270 : 90);
        head.addView(chev);
        card.addView(head);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(14), 0, dp(14), dp(8));
        list.setVisibility(open ? View.VISIBLE : View.GONE);
        for (FaqData.Item item : qa) {
            list.addView(UiHelper.divider(this, 0));
            list.addView(UiHelper.faqItem(this, item));
        }
        list.addView(UiHelper.divider(this, 0));
        TextView more = UiHelper.text(this, getString(R.string.faq_more), 13, UiHelper.COL_PRIMARY, UiHelper.TF_MEDIUM);
        more.setPadding(0, dp(12), 0, dp(6));
        more.setOnClickListener(v -> UiHelper.openUrl(this, "https://hyper120.aten-systems.site/#faq"));
        list.addView(more);
        card.addView(list);

        head.setOnClickListener(v -> {
            boolean show = list.getVisibility() != View.VISIBLE;
            list.setVisibility(show ? View.VISIBLE : View.GONE);
            chev.animate().rotation(show ? 270 : 90).setDuration(180).start();
        });
        faqSlot.addView(card);
    }

    private void updateFilterChips() {
        int games = 0;
        int selected = 0;
        for (AppEntry e : allApps) {
            if (inGamesFilter(e)) games++;
            if (selectedPackages.contains(e.packageName)) selected++;
        }
        if (allApps.isEmpty()) selected = selectedPackages.size();
        int[] counts = {allApps.size(), selected, games};
        String[] names = {getString(R.string.filter_all), getString(R.string.filter_selected), getString(R.string.filter_games)};
        for (int i = 0; i < 3; i++) {
            TextView c = filterChips[i];
            boolean on = i == filter;
            c.setText(allApps.isEmpty() ? names[i] : names[i] + "  " + counts[i]);
            c.setTextColor(on ? UiHelper.COL_PRIMARY : UiHelper.COL_MUTED);
            c.setBackground(UiHelper.ripple(this, UiHelper.pillBg(this,
                on ? UiHelper.alpha(UiHelper.COL_PRIMARY, 0.14f) : UiHelper.COL_SURFACE,
                on ? UiHelper.alpha(UiHelper.COL_PRIMARY, 0.45f) : UiHelper.COL_OUTLINE, 99), 99));
        }
        selectedCountText.setText(getString(R.string.apps_selected, selected));
    }

    /**
     * Renders an app icon into a bitmap at the size we actually show (48dp). Apps ship icons up to
     * 512px+, and keeping ~150 of those decoded cost ~180 MB, which made HyperOS likely to kill
     * the process (and the background service with it).
     */
    private Drawable smallIcon(Drawable d) {
        int px = dp(48);
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(b);
        d.setBounds(0, 0, px, px);
        d.draw(c);
        return new android.graphics.drawable.BitmapDrawable(getResources(), b);
    }

    @Override
    protected void onDestroy() {
        // Let the icons go as soon as the screen closes; the background service stays light.
        allApps.clear();
        if (listCard != null) listCard.removeAllViews();
        super.onDestroy();
    }

    private void loadInstalledAppsAsync() {
        if (appsLoading) return;
        appsLoading = true;
        appsLoadFailed = false;
        refreshList();
        new Thread(() -> {
            List<AppEntry> list = new ArrayList<>();
            boolean failed = false;
            try {
                PackageManager pm = getPackageManager();
                Set<String> seen = new HashSet<>();
                Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                for (ResolveInfo entry : pm.queryIntentActivities(launcher, 0)) {
                    if (entry.activityInfo == null) continue;
                    ApplicationInfo ai = entry.activityInfo.applicationInfo;
                    if (ai == null || getPackageName().equals(ai.packageName) || !seen.add(ai.packageName)) continue;
                    list.add(new AppEntry(pm.getApplicationLabel(ai).toString(), ai.packageName,
                        smallIcon(pm.getApplicationIcon(ai)),
                        FpsApplier.isGameApp(ai) || GameCaps.get(ai.packageName) != null));
                }
            } catch (RuntimeException ex) {
                android.util.Log.w("Hyper120", "Could not load installed apps", ex);
                failed = true;
            }
            boolean loadFailed = failed;
            ui.post(() -> {
                if (isDestroyed()) return;
                appsLoading = false;
                appsLoadFailed = loadFailed;
                if (!loadFailed) {
                    list.sort((a, b) -> {
                        boolean aSelected = selectedPackages.contains(a.packageName);
                        boolean bSelected = selectedPackages.contains(b.packageName);
                        if (aSelected != bSelected) return aSelected ? -1 : 1;
                        if (a.isGame != b.isGame) return a.isGame ? -1 : 1;
                        return a.label.compareToIgnoreCase(b.label);
                    });
                    allApps.clear();
                    allApps.addAll(list);
                }
                updateFilterChips();
                refreshList();
            });
        }).start();
    }

    /** Games tab: detected games, games from our list, and anything the user selected (so Selected ⊆ Games). */
    private boolean inGamesFilter(AppEntry e) {
        return e.isGame || selectedPackages.contains(e.packageName);
    }

    private void refreshList() {
        listCard.removeAllViews();
        int shown = 0;
        for (AppEntry e : allApps) {
            if (filter == FILTER_SELECTED && !selectedPackages.contains(e.packageName)) continue;
            if (filter == FILTER_GAMES && !inGamesFilter(e)) continue;
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
            int message = appsLoading ? R.string.loading_apps
                : allApps.isEmpty() ? R.string.no_installed_apps : R.string.no_match;
            emptyText.setText(getString(appsLoadFailed ? R.string.apps_load_failed : message));
            listCard.addView(emptyText);
        }
        if (appsLoadFailed || (!appsLoading && allApps.isEmpty())) {
            listCard.addView(actionChip(getString(R.string.check_retry), UiHelper.COL_PRIMARY,
                v -> loadInstalledAppsAsync()));
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

        TextView pkg = UiHelper.text(this, "", 12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        pkg.setSingleLine(true);
        pkg.setEllipsize(TextUtils.TruncateAt.END);
        pkg.setPadding(0, dp(5), 0, 0);
        col.addView(pkg);
        e.sub = pkg;
        bindSub(e);
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView settings = UiHelper.icon(this, R.drawable.ui_settings, UiHelper.COL_MUTED, 20);
        settings.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        settings.setPadding(dp(14), dp(14), dp(14), dp(14));
        settings.setBackground(UiHelper.ripple(this, null, 12));
        settings.setContentDescription(getString(R.string.game_settings_label, e.label));
        settings.setFocusable(true);
        settings.setOnClickListener(v -> showGameSheet(e));
        row.addView(settings);

        FrameLayout check = new FrameLayout(this);
        ImageView tick = UiHelper.icon(this, R.drawable.ui_check, UiHelper.COL_ON_PRIMARY, 16);
        check.addView(tick, new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(26), dp(26));
        cp.leftMargin = dp(12);
        row.addView(check, cp);

        e.row = row;
        e.check = check;
        e.settings = settings;
        styleCheck(e, false);
        row.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            showGameSheet(e);
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

    /** Second line of a row: limit tag, per-game rate, package name. */
    private void bindSub(AppEntry e) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        GameCaps.Info caps = GameCaps.get(e.packageName);
        String tag = null;
        int tagColor = UiHelper.COL_TEAL;
        if (caps != null) {
            if (caps.cap == GameCaps.MAX_60) {
                tag = getString(R.string.tag_60);
                tagColor = UiHelper.COL_WARN;
            } else if (caps.cap == GameCaps.MAX_90) {
                tag = getString(R.string.tag_90);
                tagColor = UiHelper.COL_PRIMARY;
            } else {
                tag = getString(R.string.tag_120);
            }
        } else if (e.isGame) {
            tag = getString(R.string.tag_game);
        }
        if (tag != null) {
            sb.append(tag);
            sb.setSpan(new ForegroundColorSpan(tagColor), 0, tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append("  \u00B7  ");
        }
        int own = FpsApplier.getGameFps(this, e.packageName);
        if (own > 0) {
            int st = sb.length();
            sb.append(own + "Hz");
            sb.setSpan(new ForegroundColorSpan(UiHelper.COL_TEXT), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append("  \u00B7  ");
        }
        sb.append(e.packageName);
        e.sub.setText(sb);
    }

    // ---------------------------------------------------------------- game sheet

    private void showGameSheet(AppEntry e) {
        GameCaps.Info caps = GameCaps.get(e.packageName);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        final android.app.Dialog[] holder = new android.app.Dialog[1];

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(e.icon);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(48), dp(48));
        ip.rightMargin = dp(14);
        head.addView(iv, ip);
        LinearLayout hc = new LinearLayout(this);
        hc.setOrientation(LinearLayout.VERTICAL);
        hc.addView(UiHelper.text(this, e.label, 18, UiHelper.COL_TEXT, UiHelper.TF_BOLD));
        TextView tagLine = UiHelper.text(this, "", 12.5f, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        tagLine.setPadding(0, dp(4), 0, 0);
        tagLine.setText(e.sub.getText());
        tagLine.setSingleLine(true);
        tagLine.setEllipsize(TextUtils.TruncateAt.END);
        hc.addView(tagLine);
        head.addView(hc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        c.addView(head);

        TextView tip = UiHelper.text(this, caps != null ? caps.note : getString(R.string.dlg_tip_unknown),
            14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        tip.setLineSpacing(dp(4), 1f);
        tip.setPadding(0, dp(16), 0, 0);
        c.addView(tip);

        TextView rateLabel = UiHelper.overline(this, getString(R.string.dlg_rate_title), UiHelper.COL_DIM);
        rateLabel.setPadding(dp(2), dp(22), 0, dp(10));
        c.addView(rateLabel);
        LinearLayout seg = new LinearLayout(this);
        seg.setBackground(UiHelper.pillBg(this, UiHelper.alpha(0xFF000000, 0.28f), UiHelper.COL_OUTLINE, 16));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        List<TextView> opts = new ArrayList<>();
        int[] rates = FpsApplier.selectableRates(this);
        int[] values = new int[rates.length + 1];
        values[0] = 0;
        System.arraycopy(rates, 0, values, 1, rates.length);
        for (int val : values) {
            String label = val == 0 ? getString(R.string.dlg_rate_default, FpsApplier.getTargetFps(this)) : String.valueOf(val);
            TextView o = UiHelper.text(this, label, 13.5f, UiHelper.COL_MUTED, UiHelper.TF_BOLD);
            o.setGravity(Gravity.CENTER);
            o.setSingleLine(true);
            o.setTag(val);
            o.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                FpsApplier.setGameFps(this, e.packageName, val);
                styleOptions(opts, val);
                bindSub(e);
                tagLine.setText(e.sub.getText());
                android.widget.Toast.makeText(this, val == 0
                    ? getString(R.string.game_rate_default, e.label)
                    : getString(R.string.game_rate_saved, e.label, val), android.widget.Toast.LENGTH_SHORT).show();
                appendLog(e.packageName + " rate " + (val == 0 ? "default" : val + "Hz"));
                if (selectedPackages.contains(e.packageName)) applyNow(false, "Game rate");
            });
            seg.addView(o, new LinearLayout.LayoutParams(0, dp(40), val == 0 ? 1.9f : 1f));
            opts.add(o);
        }
        styleOptions(opts, FpsApplier.getGameFps(this, e.packageName));
        c.addView(seg, UiHelper.matchWrap(this, 0));

        TextView report = UiHelper.button(this, getString(R.string.dlg_report), R.drawable.ui_flag, false, v -> {
            holder[0].dismiss();
            showReportSheet(e);
        });
        ((LinearLayout.LayoutParams) report.getLayoutParams()).topMargin = dp(20);
        c.addView(report);
        TextView close = UiHelper.button(this, getString(R.string.dlg_close), 0, false, v -> holder[0].dismiss());
        close.setBackground(UiHelper.ripple(this, null, 18));
        close.setTextColor(UiHelper.COL_MUTED);
        ((LinearLayout.LayoutParams) close.getLayoutParams()).topMargin = dp(4);
        c.addView(close);

        holder[0] = UiHelper.sheet(this, c);
        holder[0].show();
    }

    private void styleOptions(List<TextView> opts, int selected) {
        for (TextView o : opts) {
            boolean on = (Integer) o.getTag() == selected;
            o.setTextColor(on ? UiHelper.COL_ON_PRIMARY : UiHelper.COL_MUTED);
            o.setBackground(on ? UiHelper.gradientBg(this, 12, UiHelper.COL_PRIMARY, 0xFF818CF8)
                : UiHelper.ripple(this, null, 12));
        }
    }

    private void showReportSheet(AppEntry e) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        final android.app.Dialog[] holder = new android.app.Dialog[1];
        TextView q = UiHelper.text(this, getString(R.string.report_q, e.label), 18, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        q.setPadding(dp(2), 0, 0, dp(16));
        c.addView(q);
        int[] labels = {R.string.report_yes, R.string.report_no, R.string.report_unsure};
        String[] results = {"Works above 60", "Stuck at 60", "Not sure"};
        int[] icons = {R.drawable.ui_check, R.drawable.ui_flag, R.drawable.ui_info};
        for (int i = 0; i < 3; i++) {
            final String result = results[i];
            TextView b = UiHelper.button(this, getString(labels[i]), icons[i], i == 0, v -> {
                holder[0].dismiss();
                openGameReport(e, result);
            });
            if (i > 0) ((LinearLayout.LayoutParams) b.getLayoutParams()).topMargin = dp(10);
            c.addView(b);
        }
        holder[0] = UiHelper.sheet(this, c);
        holder[0].show();
    }

    /** Opens a GitHub issue pre-filled with the game, the result and this phone's ROM details. */
    private void openGameReport(AppEntry e, String result) {
        String gameVer = "?";
        try {
            gameVer = getPackageManager().getPackageInfo(e.packageName, 0).versionName;
        } catch (Exception ignored) {
        }
        GameCaps.Info caps = GameCaps.get(e.packageName);
        String title = "[Game] " + e.label + " - " + result + " - " + FpsApplier.getDeviceName(this)
            + " (" + FpsApplier.romRegion() + ")";
        String body = "### Game\n" + e.label + " (`" + e.packageName + "` v" + gameVer + ")\n"
            + "Listed as: " + (caps == null ? "not in list" : caps.cap + " max") + "\n"
            + "Configured rate to send: " + FpsApplier.getEffectiveGameFps(this, e.packageName) + "Hz"
            + (selectedPackages.contains(e.packageName) ? "" : " (game not ticked!)") + "\n\n"
            + "### Result\n" + result + "\n\n"
            + "### Device\n```\n" + FpsApplier.deviceReport(this) + "\n```\n\n"
            + "### Notes\n- FPS options shown in the game:\n- Selected FPS option, graphics preset and game mode:"
            + "\n- Observed gameplay FPS and measurement tool:\n- Test result in Hyper120, if available:\n"
            + FpsApplier.backgroundReport(this);
        try {
            UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new?title="
                + java.net.URLEncoder.encode(title, "UTF-8") + "&body=" + java.net.URLEncoder.encode(body, "UTF-8"));
        } catch (Exception ex) {
            UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new/choose");
        }
    }

    private void styleCheck(AppEntry e, boolean animate) {
        boolean on = selectedPackages.contains(e.packageName);
        e.settings.setVisibility(e.isGame || on ? View.VISIBLE : View.GONE);
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
        appCountText.setText(getResources().getQuantityString(R.plurals.apps_count, res.count, res.count));
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        lastApplied.setText(res.powerKeeperFound
            ? getString(R.string.last_sent, res.fps, res.count, ts)
            : getString(R.string.pk_not_found_long));

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
        Hyper120Widget.updateAll(this);
    }

    private void animateApplyButton(FpsApplier.ApplyResult res) {
        ui.removeCallbacks(resetBtnRunnable);
        applyBtn.animate().cancel();
        applyBtn.setScaleX(0.96f);
        applyBtn.setScaleY(0.96f);
        applyBtn.animate().scaleX(1f).scaleY(1f).setDuration(240)
            .setInterpolator(new DecelerateInterpolator()).start();
        if (res.powerKeeperFound) {
            applyBtn.setText(getString(R.string.btn_sent, res.fps, res.count));
            setButtonIcon(R.drawable.ui_check);
            applyBtn.setBackground(UiHelper.ripple(this,
                UiHelper.gradientBg(this, 18, UiHelper.COL_TEAL, 0xFF5EEAD4), 18));
        } else {
            applyBtn.setText(getString(R.string.btn_pk_not_found));
            applyBtn.setBackground(UiHelper.ripple(this,
                UiHelper.gradientBg(this, 18, UiHelper.COL_WARN, 0xFFFCD34D), 18));
        }
        ui.postDelayed(resetBtnRunnable, 1500);
    }

    // ---------------------------------------------------------------- apply check

    /**
     * After Apply: sends the same rate to Hyper120 itself, asks for that display mode on this window,
     * keeps the button animating (HyperOS drops the rate on a still screen) and times every frame.
     * The median over ~1.6 s shows whether the phone really switched.
     */
    private final class OverrideCheck implements android.view.Choreographer.FrameCallback {
        final int fps;
        final long[] dts = new long[600];
        int n = 0;
        long last = 0, start = 0;
        boolean done = false;
        android.animation.ValueAnimator anim;

        OverrideCheck(int fps) {
            this.fps = fps;
        }

        void begin() {
            FpsApplier.applyTargetFps(MainActivity.this, java.util.Collections.singleton(getPackageName()), fps);
            setWindowRate(fps);
            ui.removeCallbacks(resetBtnRunnable);
            applyBtn.setText(getString(R.string.check_running, fps));
            setButtonIcon(R.drawable.ui_gauge);
            android.graphics.drawable.ClipDrawable fill = new android.graphics.drawable.ClipDrawable(
                UiHelper.gradientBg(MainActivity.this, 18, UiHelper.COL_PRIMARY, 0xFF818CF8), Gravity.START,
                android.graphics.drawable.ClipDrawable.HORIZONTAL);
            applyBtn.setBackground(new android.graphics.drawable.LayerDrawable(new Drawable[]{
                UiHelper.gradientBg(MainActivity.this, 18, UiHelper.alpha(UiHelper.COL_PRIMARY, 0.45f), UiHelper.alpha(0xFF818CF8, 0.45f)), fill}));
            anim = android.animation.ValueAnimator.ofInt(0, 10000);
            anim.setDuration(2400);
            anim.addUpdateListener(a -> fill.setLevel((Integer) a.getAnimatedValue()));
            anim.start();
            android.view.Choreographer.getInstance().postFrameCallback(this);
        }

        void cancel() {
            done = true;
            if (anim != null) anim.cancel();
            android.view.Choreographer.getInstance().removeFrameCallback(this);
            setWindowRate(0);
        }

        @Override
        public void doFrame(long t) {
            if (done) return;
            if (start == 0) start = t;
            if (t - start > 700_000_000L && last != 0) {      // skip the mode switch
                long dt = t - last;
                if (dt > 0 && dt < 250_000_000L && n < dts.length) dts[n++] = dt;
            }
            last = t;
            if (t - start >= 2_300_000_000L) {
                android.view.Display display = getWindowManager().getDefaultDisplay();
                int displayHz = Math.round(display.getMode().getRefreshRate());
                cancel();
                float measured = 0;
                if (n >= 10) {
                    long[] s = java.util.Arrays.copyOf(dts, n);
                    java.util.Arrays.sort(s);
                    measured = 1_000_000_000f / s[n / 2];
                }
                onCheckDone(fps, displayHz, measured);
                return;
            }
            android.view.Choreographer.getInstance().postFrameCallback(this);
        }
    }

    private void setWindowRate(int fps) {
        try {
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.preferredDisplayModeId = 0;
            lp.preferredRefreshRate = 0;
            if (fps > 0) {
                android.view.Display d = getWindowManager().getDefaultDisplay();
                android.view.Display.Mode cur = d.getMode();
                for (android.view.Display.Mode m : d.getSupportedModes()) {
                    if (m.getPhysicalWidth() == cur.getPhysicalWidth() && m.getPhysicalHeight() == cur.getPhysicalHeight()
                        && Math.round(m.getRefreshRate()) == fps) {
                        lp.preferredDisplayModeId = m.getModeId();
                    }
                }
                lp.preferredRefreshRate = fps;
            }
            getWindow().setAttributes(lp);
        } catch (Exception ignored) {
        }
    }

    private void startCheck() {
        if (check != null) {
            check.cancel();
            check = null;
        }
        checkResultTarget = 0;
        updateSegments();
        int fps = FpsApplier.getEffectiveTargetFps(this);
        if (fps <= 60 || !FpsApplier.isPowerKeeperInstalled(this)) return;
        check = new OverrideCheck(fps);
        check.begin();
    }

    private void onCheckDone(int fps, int displayHz, float measured) {
        check = null;
        boolean verified = displayHz >= fps - 1 && measured >= fps * 0.95f;
        appendLog(String.format(Locale.US, "Check %dHz: display %dHz, Hyper120 test %.1f FPS",
            fps, displayHz, measured));
        resetApplyButton();

        int accent = verified ? UiHelper.COL_TEAL : UiHelper.COL_WARN;
        checkBox.removeAllViews();
        checkBox.setPadding(dp(14), dp(14), dp(14), dp(6));
        checkBox.setBackground(UiHelper.pillBg(this, UiHelper.alpha(accent, 0.07f),
            UiHelper.alpha(accent, 0.30f), 16));

        TextView heading = UiHelper.text(this, verified ? getString(R.string.check_verified)
            : getString(R.string.check_fail_title_unknown, fps), 15, accent, UiHelper.TF_BOLD);
        Drawable icon = getDrawable(verified ? R.drawable.ui_check : R.drawable.ui_info).mutate();
        icon.setTint(accent);
        icon.setBounds(0, 0, dp(16), dp(16));
        heading.setCompoundDrawablesRelative(icon, null, null, null);
        heading.setCompoundDrawablePadding(dp(8));
        checkBox.addView(heading, UiHelper.matchWrap(this, 0));

        TextView details = UiHelper.text(this, getString(R.string.check_result_modes, fps, displayHz)
            + "\n" + getString(R.string.check_measured, Math.round(measured)),
            13, UiHelper.COL_TEXT, UiHelper.TF_MEDIUM);
        details.setLineSpacing(dp(4), 1f);
        checkBox.addView(details, UiHelper.matchWrap(this, 8));

        boolean refreshLow = FpsApplier.isRateLocked(this, fps);
        int bodyRes = verified ? R.string.check_result_scope
            : refreshLow ? R.string.check_fail_refresh : R.string.check_fail_body;
        TextView body = UiHelper.text(this, getString(bodyRes), 13, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        body.setLineSpacing(dp(3), 1f);
        checkBox.addView(body, UiHelper.matchWrap(this, 8));

        if (verified) {
            addCheckAction(R.string.check_game_help, v -> openFaq());
        } else if (refreshLow) {
            addCheckAction(R.string.health_open_display, v -> UiHelper.openDisplaySettings(this));
        } else {
            addCheckAction(R.string.check_report, v -> reportCheck(fps, displayHz, measured));
            addCheckAction(R.string.check_faq, v -> openFaq());
        }
        checkResultTarget = fps;
        checkBox.setVisibility(View.VISIBLE);
    }

    private void addCheckAction(int label, View.OnClickListener onClick) {
        TextView action = UiHelper.text(this, getString(label), 13, UiHelper.COL_PRIMARY, UiHelper.TF_BOLD);
        action.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        action.setMinHeight(dp(48));
        action.setPadding(0, dp(8), 0, dp(8));
        action.setBackground(UiHelper.ripple(this, null, 8));
        action.setOnClickListener(onClick);
        checkBox.addView(action, UiHelper.matchWrap(this, 0));
    }

    private void reportCheck(int fps, int displayHz, float measured) {
        try {
            String title = "[Compatibility] " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                + " (" + android.os.Build.DEVICE + ") - Apply check: " + Math.round(measured) + " FPS at " + fps + "Hz";
            String body = "### Device & check result\n```\n" + FpsApplier.deviceReport(this) + "\n"
                + String.format(Locale.US, "Apply check: requested %dHz | display mode %dHz | Hyper120 test %.1f FPS (median)",
                    fps, displayHz, measured)
                + "\n```\n\n### Games tested & notes\n- Autostart ON & Battery saver = No restrictions?\n- Games tested:\n"
                + FpsApplier.backgroundReport(this);
            UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new?title="
                + java.net.URLEncoder.encode(title, "UTF-8") + "&body=" + java.net.URLEncoder.encode(body, "UTF-8"));
        } catch (Exception e) {
            UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new/choose");
        }
    }

    /** Opens the FAQ card and scrolls to it. */
    private void openFaq() {
        fillFaq(true);
        if (mainScroll == null || faqSlot == null) return;
        mainScroll.post(() -> {
            int[] slot = new int[2], sv = new int[2];
            faqSlot.getLocationOnScreen(slot);
            mainScroll.getLocationOnScreen(sv);
            int y = mainScroll.getScrollY() + slot[1] - sv[1] - dp(16);
            mainScroll.smoothScrollTo(0, Math.max(0, y));
        });
    }

    private void resetApplyButton() {
        applyBtn.setText(getString(R.string.btn_apply, FpsApplier.getEffectiveTargetFps(this)));
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
