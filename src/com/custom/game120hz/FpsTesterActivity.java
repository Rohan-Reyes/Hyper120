package com.custom.game120hz;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;

/** Built-in live FPS & display refresh-rate tester using Choreographer frame callbacks. */
public class FpsTesterActivity extends Activity implements Choreographer.FrameCallback {
    private TextView verdictBadge;
    private TextView fpsReadout;
    private TextView modeReadout;
    private TextView deviceReadout;
    private MotionBarView motionBar;
    private boolean running = false;
    private long windowStartNs = 0L;
    private int frameCount = 0;
    private int sampleCount = 0;
    private float measuredFps = 0f;
    private float displayHz = 0f;
    private int maxHz = 120;

    private static final class MotionBarView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float pos = 0f;
        private float dir = 1f;
        private long lastNs = 0L;

        MotionBarView(Context ctx) {
            super(ctx);
            trackPaint.setColor(0xFF0E141D);
            barPaint.setColor(UiHelper.COL_PRIMARY);
        }

        void step(long frameTimeNanos) {
            if (lastNs != 0L) {
                float dt = Math.min((frameTimeNanos - lastNs) / 1_000_000_000f, 0.05f);
                pos += dir * dt * 1.45f;
                if (pos >= 1f) {
                    pos = 1f;
                    dir = -1f;
                } else if (pos <= 0f) {
                    pos = 0f;
                    dir = 1f;
                }
            }
            lastNs = frameTimeNanos;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float r = h / 2f;
            canvas.drawRoundRect(0, 0, w, h, r, r, trackPaint);
            float barW = Math.max(h * 1.4f, w * 0.22f);
            float x = pos * Math.max(0f, w - barW);
            canvas.drawRoundRect(x, 0, x + barW, h, r, r, barPaint);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);
        maxHz = FpsApplier.getMaxSupportedFps(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COL_BG);
        UiHelper.applyNotchSafePadding(this, root, 20, 18, 24);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(18));

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
        header.setText("Live FPS Tester");
        header.setTextSize(20);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(UiHelper.COL_TEXT);
        header.setPadding(dp(16), 0, 0, 0);
        topBar.addView(header);
        root.addView(topBar);

        // Live FPS Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 22));
        card.setPadding(dp(20), dp(22), dp(20), dp(22));

        verdictBadge = new TextView(this);
        verdictBadge.setText("Testing your device...");
        verdictBadge.setTextSize(13);
        verdictBadge.setTypeface(Typeface.DEFAULT_BOLD);
        verdictBadge.setTextColor(UiHelper.COL_PRIMARY);
        verdictBadge.setBackground(UiHelper.pillBg(this, UiHelper.COL_BG, UiHelper.COL_OUTLINE, 16));
        verdictBadge.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout.LayoutParams vParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vParams.bottomMargin = dp(12);
        verdictBadge.setLayoutParams(vParams);
        card.addView(verdictBadge);

        fpsReadout = new TextView(this);
        fpsReadout.setText("--.- FPS");
        fpsReadout.setTextSize(40);
        fpsReadout.setTypeface(Typeface.DEFAULT_BOLD);
        fpsReadout.setTextColor(UiHelper.COL_TEAL);
        card.addView(fpsReadout);

        modeReadout = new TextView(this);
        modeReadout.setText("Measuring display mode...");
        modeReadout.setTextSize(14);
        modeReadout.setTextColor(UiHelper.COL_TEXT);
        modeReadout.setPadding(0, dp(6), 0, dp(16));
        card.addView(modeReadout);

        motionBar = new MotionBarView(this);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(22));
        barParams.bottomMargin = dp(16);
        motionBar.setLayoutParams(barParams);
        card.addView(motionBar);

        deviceReadout = new TextView(this);
        deviceReadout.setTextColor(UiHelper.COL_MUTED);
        deviceReadout.setTextSize(12);
        deviceReadout.setGravity(Gravity.CENTER);
        deviceReadout.setText(buildInfoText());
        card.addView(deviceReadout);

        root.addView(card);

        // Actions
        Button reapplyBtn = new Button(this);
        reapplyBtn.setText("Re-apply " + FpsApplier.getTargetFps(this) + "Hz Override");
        reapplyBtn.setAllCaps(false);
        reapplyBtn.setTextSize(15);
        reapplyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        reapplyBtn.setTextColor(UiHelper.COL_ON_PRIMARY);
        reapplyBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_PRIMARY, 0, 26));
        LinearLayout.LayoutParams reParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        reParams.topMargin = dp(16);
        reapplyBtn.setLayoutParams(reParams);
        reapplyBtn.setOnClickListener(v -> {
            FpsApplier.ApplyResult res = FpsApplier.apply120Hz(this, FpsApplier.loadPackages(this));
            Toast.makeText(this, "Sent " + res.fps + "Hz to " + res.count + " app(s)", Toast.LENGTH_SHORT).show();
        });
        root.addView(reapplyBtn);

        Button copyBtn = new Button(this);
        copyBtn.setText("Copy Device & FPS Report");
        copyBtn.setAllCaps(false);
        copyBtn.setTextSize(14);
        copyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        copyBtn.setTextColor(UiHelper.COL_TEXT);
        copyBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 26));
        LinearLayout.LayoutParams cpParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        cpParams.topMargin = dp(10);
        copyBtn.setLayoutParams(cpParams);
        copyBtn.setOnClickListener(v -> copyReport());
        root.addView(copyBtn);

        Button reportBtn = new Button(this);
        reportBtn.setText("Report Compatibility / Bug on GitHub");
        reportBtn.setAllCaps(false);
        reportBtn.setTextSize(14);
        reportBtn.setTypeface(Typeface.DEFAULT_BOLD);
        reportBtn.setTextColor(UiHelper.COL_PRIMARY);
        reportBtn.setBackground(UiHelper.pillBg(this, UiHelper.COL_BG, UiHelper.COL_OUTLINE, 26));
        LinearLayout.LayoutParams repParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        repParams.topMargin = dp(10);
        reportBtn.setLayoutParams(repParams);
        reportBtn.setOnClickListener(v -> openReportIssue());
        root.addView(reportBtn);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        windowStartNs = 0L;
        frameCount = 0;
        sampleCount = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onPause() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        super.onPause();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        motionBar.step(frameTimeNanos);
        if (windowStartNs == 0L) {
            windowStartNs = frameTimeNanos;
        }
        frameCount++;
        long elapsedNs = frameTimeNanos - windowStartNs;
        if (elapsedNs >= 500_000_000L) {
            measuredFps = (frameCount * 1_000_000_000f) / elapsedNs;
            windowStartNs = frameTimeNanos;
            frameCount = 0;
            sampleCount++;
            updateReadouts();
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void updateReadouts() {
        Display d = getWindowManager().getDefaultDisplay();
        displayHz = d != null ? d.getRefreshRate() : 0f;
        int target = FpsApplier.getTargetFps(this);
        int expected = Math.min(target, maxHz);
        boolean pkOk = FpsApplier.isPowerKeeperInstalled(this);
        boolean high = measuredFps >= expected - 6f;

        fpsReadout.setText(String.format("%.1f FPS", measuredFps));
        fpsReadout.setTextColor(high ? UiHelper.COL_TEAL : UiHelper.COL_COFFEE_BG);
        modeReadout.setText(String.format(
            "Display: %.1f Hz  \u2022  Target: %dHz  \u2022  Max: %dHz",
            displayHz, target, maxHz));

        if (!pkOk) {
            verdictBadge.setText("\u2717 Not supported (PowerKeeper missing)");
            verdictBadge.setTextColor(UiHelper.COL_COFFEE_BG);
            verdictBadge.setBackground(UiHelper.pillBg(this, 0xFF2B1D12, 0xFF78350F, 16));
        } else if (expected <= 60) {
            verdictBadge.setText("Running at 60Hz (Select 90/120Hz to test)");
            verdictBadge.setTextColor(UiHelper.COL_PRIMARY);
            verdictBadge.setBackground(UiHelper.pillBg(this, UiHelper.COL_BG, UiHelper.COL_OUTLINE, 16));
        } else if (high) {
            verdictBadge.setText("\u2713 Works on your device (" + Math.round(measuredFps) + " FPS)");
            verdictBadge.setTextColor(UiHelper.COL_ON_PRIMARY);
            verdictBadge.setBackground(UiHelper.pillBg(this, UiHelper.COL_TEAL, 0, 16));
        } else if (sampleCount >= 3) {
            verdictBadge.setText("\u2717 Clamped at ~" + Math.round(measuredFps) + " FPS (May not work on this ROM)");
            verdictBadge.setTextColor(0xFFFCA5A5);
            verdictBadge.setBackground(UiHelper.pillBg(this, 0xFF2B1518, 0xFF7F1D1D, 16));
        }
    }

    private String buildInfoText() {
        String pk = FpsApplier.isPowerKeeperInstalled(this) ? "Installed" : "Not found";
        return FpsApplier.getDeviceSummary(this) + "\nPowerKeeper: " + pk;
    }

    private String buildReportText() {
        String pk = FpsApplier.isPowerKeeperInstalled(this) ? "OK" : "Missing";
        String ver = "?";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        return "Hyper120 v" + ver + " Diagnostic Report\n"
            + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")\n"
            + "Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
            + "Max Hardware Hz: " + maxHz + "Hz\n"
            + "Target FPS: " + FpsApplier.getTargetFps(this) + "Hz\n"
            + String.format("Display Mode: %.1f Hz | Measured: %.1f FPS\n", displayHz, measuredFps)
            + "PowerKeeper: " + pk;
    }

    private void copyReport() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Hyper120 Report", buildReportText()));
            Toast.makeText(this, "Copied diagnostic report", Toast.LENGTH_SHORT).show();
        }
    }

    private void openReportIssue() {
        try {
            int expected = Math.min(FpsApplier.getTargetFps(this), maxHz);
            String verdict = !FpsApplier.isPowerKeeperInstalled(this)
                ? "PowerKeeper missing"
                : (measuredFps >= expected - 6f ? "Works (" + Math.round(measuredFps) + " FPS)" : "Clamped at " + Math.round(measuredFps) + " FPS");
            String title = "[Compatibility] " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ") - " + verdict;
            String body = "### Device & Tester Output\n```\n" + buildReportText()
                + "\n```\n\n### HyperOS / MIUI Version & Region\n(e.g. HyperOS 2.0 Global / CN)\n\n"
                + "### Games Tested & Notes\n- Autostart ON & Battery Saver = No restrictions?\n- Games tested:\n";
            String url = UiHelper.GITHUB_REPO_URL + "/issues/new?title="
                + URLEncoder.encode(title, "UTF-8")
                + "&body=" + URLEncoder.encode(body, "UTF-8");
            UiHelper.openUrl(this, url);
        } catch (Exception e) {
            UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new/choose");
        }
    }

    private int dp(int v) {
        return UiHelper.dp(this, v);
    }
}
