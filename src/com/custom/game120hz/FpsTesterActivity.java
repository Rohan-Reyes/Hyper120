package com.custom.game120hz;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;

/** Live FPS tester: measures real frame delivery with Choreographer and gives a verdict. */
public class FpsTesterActivity extends Activity implements Choreographer.FrameCallback {
    private GaugeView gauge;
    private MotionBarView motionBar;
    private TextView verdictChip;
    private TextView verdictText;
    private TextView statDisplay;
    private TextView statTarget;
    private TextView applyBtn;
    private boolean running = false;
    private long windowStartNs = 0L;
    private int frameCount = 0;
    private int sampleCount = 0;
    private float measuredFps = 0f;
    private float displayHz = 0f;
    private int maxHz = 120;
    private int testFps = 120;
    private final java.util.List<TextView> testSegs = new java.util.ArrayList<>();

    /** 240° arc gauge with labelled refresh-rate ticks. */
    private static final class GaugeView extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private final int[] marks;
        private final float max;
        private final float density;
        private float value = 0f;
        private float shown = 0f;

        GaugeView(Context ctx, int[] marks, int maxHz) {
            super(ctx);
            this.marks = marks;
            this.max = Math.max(maxHz, 60) * 1.1f;
            density = ctx.getResources().getDisplayMetrics().density;
            float sw = 14 * density;
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(sw);
            track.setStrokeCap(Paint.Cap.ROUND);
            track.setColor(0xFF1A2130);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeWidth(sw);
            arc.setStrokeCap(Paint.Cap.ROUND);
            tick.setStrokeWidth(2 * density);
            tick.setStrokeCap(Paint.Cap.ROUND);
            label.setTextSize(11 * density);
            label.setTextAlign(Paint.Align.CENTER);
            label.setTypeface(UiHelper.TF_BOLD);
            big.setTextSize(58 * density);
            big.setTextAlign(Paint.Align.CENTER);
            big.setTypeface(UiHelper.TF_DISPLAY);
            big.setColor(UiHelper.COL_TEXT);
            small.setTextSize(13 * density);
            small.setTextAlign(Paint.Align.CENTER);
            small.setTypeface(UiHelper.TF_BOLD);
            small.setLetterSpacing(0.2f);
            small.setColor(UiHelper.COL_MUTED);
        }

        void setValue(float v) {
            value = v;
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            setMeasuredDimension(width, (int) (width * 0.62f));
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            float sw = arc.getStrokeWidth();
            float r = Math.min(w * 0.34f, h * 0.42f);
            float cx = w / 2f, cy = h * 0.60f;
            oval.set(cx - r, cy - r, cx + r, cy + r);
            SweepGradient sg = new SweepGradient(cx, cy,
                new int[]{UiHelper.COL_INDIGO, UiHelper.COL_PRIMARY, UiHelper.COL_TEAL, UiHelper.COL_INDIGO},
                new float[]{0f, 0.35f, 0.66f, 1f});
            Matrix m = new Matrix();
            m.setRotate(140f - 10f, cx, cy);
            sg.setLocalMatrix(m);
            arc.setShader(sg);
        }

        @Override
        protected void onDraw(Canvas c) {
            shown += (value - shown) * 0.18f;
            float start = 150f, sweep = 240f;
            c.drawArc(oval, start, sweep, false, track);
            float frac = Math.max(0.005f, Math.min(1f, shown / max));
            c.drawArc(oval, start, sweep * frac, false, arc);

            float cx = oval.centerX(), cy = oval.centerY(), r = oval.width() / 2f;
            for (int hz : marks) {
                double a = Math.toRadians(start + sweep * Math.min(1f, hz / max));
                float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
                float r1 = r + 14 * density, r2 = r + 20 * density, rl = r + 34 * density;
                boolean reached = shown >= hz - 4;
                tick.setColor(reached ? UiHelper.COL_TEXT : UiHelper.COL_DIM);
                label.setColor(reached ? UiHelper.COL_TEXT : UiHelper.COL_DIM);
                c.drawLine(cx + r1 * cos, cy + r1 * sin, cx + r2 * cos, cy + r2 * sin, tick);
                c.drawText(String.valueOf(hz), cx + rl * cos, cy + rl * sin + 4 * density, label);
            }
            c.drawText(shown < 1 ? "--" : String.valueOf(Math.round(shown)), cx, cy + 14 * density, big);
            c.drawText("FPS", cx, cy + 40 * density, small);
            if (Math.abs(value - shown) > 0.2f) postInvalidateOnAnimation();
        }
    }

    /** A puck bouncing across a track; judder is easy to see at low refresh rates. */
    private static final class MotionBarView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint puck = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ghost = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] trail = new float[5];
        private float pos = 0f;
        private float dir = 1f;
        private long lastNs = 0L;

        MotionBarView(Context ctx) {
            super(ctx);
            trackPaint.setColor(0xFF0B1018);
            puck.setColor(UiHelper.COL_PRIMARY);
            ghost.setColor(UiHelper.COL_PRIMARY);
        }

        void step(long frameTimeNanos) {
            if (lastNs != 0L) {
                float dt = Math.min((frameTimeNanos - lastNs) / 1_000_000_000f, 0.05f);
                System.arraycopy(trail, 0, trail, 1, trail.length - 1);
                trail[0] = pos;
                pos += dir * dt * 1.6f;
                if (pos >= 1f) { pos = 1f; dir = -1f; }
                else if (pos <= 0f) { pos = 0f; dir = 1f; }
            }
            lastNs = frameTimeNanos;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), r = h / 2f;
            canvas.drawRoundRect(0, 0, w, h, r, r, trackPaint);
            float pad = 4 * getResources().getDisplayMetrics().density;
            float d = h - pad * 2, travel = w - pad * 2 - d;
            for (int i = trail.length - 1; i >= 0; i--) {
                ghost.setAlpha(Math.round(70f * (1f - (i + 1f) / (trail.length + 1f))));
                float x = pad + trail[i] * travel;
                canvas.drawRoundRect(x, pad, x + d, pad + d, d / 2, d / 2, ghost);
            }
            float x = pad + pos * travel;
            canvas.drawRoundRect(x, pad, x + d, pad + d, d / 2, d / 2, puck);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);
        maxHz = FpsApplier.getMaxSupportedFps(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(UiHelper.topBar(this, "FPS tester"));

        // Gauge card
        LinearLayout card = UiHelper.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(16), dp(18), dp(16), dp(20));
        testFps = Math.min(FpsApplier.getTargetFps(this), maxHz);
        verdictChip = UiHelper.chip(this, "Measuring…", UiHelper.COL_PRIMARY);
        card.addView(verdictChip, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        gauge = new GaugeView(this, FpsApplier.detectSupportedFps(this), maxHz);
        LinearLayout.LayoutParams gp = UiHelper.matchWrap(this, 4);
        card.addView(gauge, gp);
        verdictText = UiHelper.text(this, "Keep this screen open for a few seconds.",
            13.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        verdictText.setGravity(Gravity.CENTER);
        verdictText.setLineSpacing(dp(3), 1f);
        verdictText.setPadding(dp(8), 0, dp(8), 0);
        card.addView(verdictText);

        TextView testLabel = UiHelper.overline(this, "Test at", UiHelper.COL_DIM);
        testLabel.setPadding(dp(4), dp(20), 0, dp(10));
        card.addView(testLabel, UiHelper.matchWrap(this, 0));
        LinearLayout seg = new LinearLayout(this);
        seg.setBackground(UiHelper.pillBg(this, UiHelper.alpha(0xFF000000, 0.28f), UiHelper.COL_OUTLINE, 16));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int fps : FpsApplier.detectSupportedFps(this)) {
            TextView s = UiHelper.text(this, fps + " Hz", 14, UiHelper.COL_MUTED, UiHelper.TF_BOLD);
            s.setGravity(Gravity.CENTER);
            s.setTag(fps);
            s.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                startTest(fps);
            });
            seg.addView(s, new LinearLayout.LayoutParams(0, dp(40), 1f));
            testSegs.add(s);
        }
        card.addView(seg, UiHelper.matchWrap(this, 0));
        root.addView(card);

        // Motion preview
        LinearLayout motion = UiHelper.card(this);
        motion.addView(UiHelper.overline(this, "Motion preview", UiHelper.COL_DIM));
        motionBar = new MotionBarView(this);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30));
        mp.topMargin = dp(12);
        motion.addView(motionBar, mp);
        root.addView(motion);

        // Stats grid
        LinearLayout grid1 = new LinearLayout(this);
        grid1.setLayoutParams(UiHelper.matchWrap(this, 12));
        statDisplay = stat(grid1, "Display mode", "--", true);
        statTarget = stat(grid1, "Testing at", testFps + " Hz", false);
        root.addView(grid1);
        LinearLayout grid2 = new LinearLayout(this);
        grid2.setLayoutParams(UiHelper.matchWrap(this, 12));
        stat(grid2, "Panel max", maxHz + " Hz", true);
        boolean pk = FpsApplier.isPowerKeeperInstalled(this);
        TextView pkv = stat(grid2, "PowerKeeper", pk ? "Found" : "Missing", false);
        pkv.setTextColor(pk ? UiHelper.COL_TEAL : UiHelper.COL_WARN);
        root.addView(grid2);

        // Actions
        applyBtn = UiHelper.button(this, "Re-apply " + FpsApplier.getTargetFps(this) + "Hz",
            R.drawable.ui_refresh, true, v -> {
                FpsApplier.ApplyResult res = FpsApplier.apply120Hz(this, FpsApplier.loadPackages(this));
                startTest(testFps);
                Toast.makeText(this, "Sent " + res.fps + "Hz to " + res.count + " apps", Toast.LENGTH_SHORT).show();
            });
        ((LinearLayout.LayoutParams) applyBtn.getLayoutParams()).topMargin = dp(18);
        root.addView(applyBtn);

        LinearLayout actions = new LinearLayout(this);
        actions.setLayoutParams(UiHelper.matchWrap(this, 10));
        TextView copy = UiHelper.button(this, "Copy report", R.drawable.ui_copy, false, v -> copyReport());
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(52), 1f);
        a.rightMargin = dp(5);
        actions.addView(copy, a);
        TextView report = UiHelper.button(this, "Report", R.drawable.ui_flag, false, v -> openReportIssue());
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(52), 1f);
        b.leftMargin = dp(5);
        actions.addView(report, b);
        root.addView(actions);

        TextView note = UiHelper.text(this,
            "This screen measures frames the system actually delivers to Hyper120. Games are limited by "
                + "their own FPS setting too, so set the in-game frame rate to High/Ultra or 120.",
            12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        note.setLineSpacing(dp(3), 1f);
        note.setPadding(dp(6), dp(16), dp(6), 0);
        root.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiHelper.COL_BG);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root);
        UiHelper.applyNotchSafePadding(this, scroll, 16, 12, 24);
        setContentView(scroll);
    }

    private TextView stat(LinearLayout parent, String label, String value, boolean first) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setBackground(UiHelper.pillBg(this, UiHelper.COL_SURFACE, UiHelper.COL_OUTLINE, 20));
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.addView(UiHelper.overline(this, label, UiHelper.COL_DIM));
        TextView v = UiHelper.text(this, value, 20, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        v.setPadding(0, dp(8), 0, 0);
        t.addView(v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (first) lp.rightMargin = dp(6); else lp.leftMargin = dp(6);
        parent.addView(t, lp);
        return v;
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        windowStartNs = 0L;
        frameCount = 0;
        sampleCount = 0;
        Choreographer.getInstance().postFrameCallback(this);
        startTest(testFps);
    }

    /**
     * Points the override at Hyper120 itself before measuring. PowerKeeper only lifts the cap for
     * packages in mPrivGames, so without this the tester could read 60 FPS on a device that works.
     * Also asks for the matching display mode on this window.
     */
    private void startTest(int fps) {
        testFps = fps;
        FpsApplier.applyTargetFps(this, java.util.Collections.singleton(getPackageName()), fps);
        try {
            Display d = getWindowManager().getDefaultDisplay();
            Display.Mode cur = d.getMode();
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            for (Display.Mode m : d.getSupportedModes()) {
                if (m.getPhysicalWidth() == cur.getPhysicalWidth()
                    && m.getPhysicalHeight() == cur.getPhysicalHeight()
                    && Math.round(m.getRefreshRate()) == fps) {
                    lp.preferredDisplayModeId = m.getModeId();
                }
            }
            lp.preferredRefreshRate = fps;
            getWindow().setAttributes(lp);
        } catch (Exception ignored) {
        }
        for (TextView s : testSegs) {
            boolean on = (Integer) s.getTag() == fps;
            s.setTextColor(on ? UiHelper.COL_ON_PRIMARY : UiHelper.COL_MUTED);
            s.setBackground(on ? UiHelper.gradientBg(this, 12, UiHelper.COL_PRIMARY, 0xFF818CF8)
                : UiHelper.ripple(this, null, 12));
        }
        if (statTarget != null) statTarget.setText(fps + " Hz");
        windowStartNs = 0L;
        frameCount = 0;
        sampleCount = -1; // drop the first window while the display mode switches
        UiHelper.setChip(this, verdictChip, "Measuring at " + fps + " Hz…", UiHelper.COL_PRIMARY);
        verdictText.setText("Sent the " + fps + "Hz override to Hyper120 itself. Keep this screen open for a few seconds.");
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
        if (windowStartNs == 0L) windowStartNs = frameTimeNanos;
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
        int expected = Math.min(testFps, maxHz);
        if (sampleCount <= 0) {
            gauge.setValue(measuredFps);
            return;
        }
        boolean pkOk = FpsApplier.isPowerKeeperInstalled(this);
        boolean high = measuredFps >= expected - 6f;

        gauge.setValue(measuredFps);
        statDisplay.setText(String.format("%.0f Hz", displayHz));

        if (!pkOk) {
            UiHelper.setChip(this, verdictChip, "Not supported", UiHelper.COL_WARN);
            verdictText.setText("PowerKeeper isn't installed, so this ROM can't receive the override.");
        } else if (expected <= 60) {
            UiHelper.setChip(this, verdictChip, "Testing 60 Hz", UiHelper.COL_PRIMARY);
            verdictText.setText("Pick a higher rate below to test whether the cap is lifted.");
        } else if (high) {
            UiHelper.setChip(this, verdictChip, expected + " Hz works", UiHelper.COL_TEAL);
            verdictText.setText("Frames are arriving at " + Math.round(measuredFps)
                + " FPS, so PowerKeeper accepted the override. Your games can use " + expected + "Hz.");
        } else if (sampleCount >= 3) {
            UiHelper.setChip(this, verdictChip, "Clamped at " + Math.round(measuredFps), UiHelper.COL_ERROR);
            verdictText.setText("The display stayed near " + Math.round(displayHz)
                + "Hz even with the " + expected + "Hz override. Tap Re-apply, or report your device.");
        }
    }

    private String buildReportText() {
        String pk = FpsApplier.isPowerKeeperInstalled(this) ? "OK" : "Missing";
        return "Hyper120 v" + UiHelper.appVersion(this) + " Diagnostic Report\n"
            + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")\n"
            + "Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
            + "Max Hardware Hz: " + maxHz + "Hz\n"
            + "Target FPS: " + FpsApplier.getTargetFps(this) + "Hz | Tested at: " + testFps + "Hz\n"
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
            int expected = Math.min(testFps, maxHz);
            String verdict = !FpsApplier.isPowerKeeperInstalled(this)
                ? "PowerKeeper missing"
                : (measuredFps >= expected - 6f ? "Works (" + Math.round(measuredFps) + " FPS)"
                    : "Clamped at " + Math.round(measuredFps) + " FPS");
            String title = "[Compatibility] " + Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ") - " + verdict;
            String body = "### Device & Tester Output\n```\n" + buildReportText()
                + "\n```\n\n### HyperOS / MIUI Version & Region\n(e.g. HyperOS 2.0 Global / CN)\n\n"
                + "### Games Tested & Notes\n- Autostart ON & Battery Saver = No restrictions?\n- Games tested:\n";
            UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new?title="
                + URLEncoder.encode(title, "UTF-8") + "&body=" + URLEncoder.encode(body, "UTF-8"));
        } catch (Exception e) {
            UiHelper.openUrl(this, UiHelper.GITHUB_REPO_URL + "/issues/new/choose");
        }
    }

    private int dp(float v) {
        return UiHelper.dp(this, v);
    }
}
