package com.custom.game120hz;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * First-launch guide, visual-first:
 *  0. what it does (animated 60 vs 120 demo)
 *  1. before you play (screen + in-game requirements, tag legend, "I understand" gate)
 *  2. keep it running (checklist tiles)
 */
public class OnboardingActivity extends Activity {
    private int page = 0;
    /** Set when the user taps "I understand" on the requirements page; Next stays disabled until then. */
    private boolean understood = false;
    /** Existing users see "you just updated, settings kept" instead of the welcome page. */
    private boolean updater;
    private LinearLayout body;
    private LinearLayout dots;
    private TextView nextBtn;
    private TextView skipBtn;
    private View refreshTile;
    private View batteryTile;

    /** Two lanes of dots: positions sampled at 60Hz vs 120Hz, drawn as frame trails. */
    private static final class PacingView extends View {
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float dp;

        PacingView(Context c) {
            super(c);
            dp = c.getResources().getDisplayMetrics().density;
            track.setColor(UiHelper.COL_OUTLINE);
            track.setStrokeWidth(2 * dp);
            track.setPathEffect(new DashPathEffect(new float[]{3 * dp, 7 * dp}, 0));
            label.setTextSize(15 * dp);
            label.setTypeface(UiHelper.TF_BOLD);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = 10 * dp, pad = 74 * dp;
            double t = System.nanoTime() / 1e9;
            int[][] lanes = {{60, 0xFF94A3B8}, {120, UiHelper.COL_PRIMARY}};
            for (int i = 0; i < 2; i++) {
                int hz = lanes[i][0], col = lanes[i][1];
                float y = h * (i == 0 ? 0.3f : 0.72f);
                label.setColor(col);
                c.drawText(hz + " Hz", 0, y + 5 * dp, label);
                c.drawLine(pad, y, w - r, y, track);
                long frame = (long) Math.floor(t * hz);
                int trail = hz == 120 ? 12 : 6;
                for (int k = trail; k >= 0; k--) {
                    double ft = (frame - k) / (double) hz;
                    double p = 0.5 - 0.5 * Math.cos(((ft / 1.4) % 1.0) * Math.PI * 2);
                    float x = (float) (pad + r + p * (w - pad - 2 * r));
                    dot.setColor(k == 0 ? col : UiHelper.alpha(col, 0.3f * (1f - k / (trail + 1f))));
                    c.drawCircle(x, y, k == 0 ? r : r * 0.8f, dot);
                }
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);
        updater = FpsApplier.isUpdateFromOlder(this);
        FaqData.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COL_BG);
        UiHelper.applyNotchSafePadding(this, root, 22, 12, 22);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        top.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(38)));
        TextView name = UiHelper.text(this, getString(R.string.app_name), 17, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        name.setPadding(dp(10), 0, 0, 0);
        top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        skipBtn = UiHelper.text(this, getString(R.string.ob_skip), 15, UiHelper.COL_MUTED, UiHelper.TF_MEDIUM);
        skipBtn.setPadding(dp(14), dp(10), dp(4), dp(10));
        skipBtn.setOnClickListener(v -> {
            if (!understood) {
                page = 1;
                render();
            } else if (!screenReady()) {
                page = 2;
                render();
            } else {
                finishGuide();
            }
        });
        top.addView(skipBtn);
        root.addView(top);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(22), 0, dp(20));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, dp(4), 0, dp(14));
        root.addView(dots, UiHelper.matchWrap(this, 0));

        nextBtn = UiHelper.button(this, "", 0, true, v -> {
            if (page == 1 && !understood) return;
            if (page < 2) {
                page++;
                render();
            } else {
                finishGuide();
            }
        });
        root.addView(nextBtn);

        setContentView(root);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (page == 2) render();  // refresh checklist ticks after returning from Settings
    }

    @Override
    public void onBackPressed() {
        if (page > 0) {
            page--;
            render();
        } else {
            moveTaskToBack(true);
        }
    }

    /** Used to decide whether to show display setup advice. */
    private boolean screenReady() {
        if (FpsApplier.getMaxSupportedFps(this) < 90) return false;
        int setting = FpsApplier.getSystemRefreshSetting(this);
        return setting < 0 || setting >= 89;
    }

    private void finishGuide() {
        FpsApplier.setOnboarded(this);
        finish();
    }

    // ---------------------------------------------------------------- pages

    private int renderedPage = -1;

    private void render() {
        body.removeAllViews();
        if (page == 0 && updater) pageWhatsNew();
        else if (page == 0) pageIntro();
        else if (page == 1) pageRequirements();
        else pageKeepRunning();

        if (page == 0) addSetupFaq("safe", "compat", "adaptive");
        else if (page == 1) addSetupFaq("stuck60", "high", "heat");
        else addSetupFaq("setup90", "stopped", "gameturbo");

        if (renderedPage != page) {
            renderedPage = page;
            ScrollView setupScroll = (ScrollView) body.getParent();
            setupScroll.post(() -> setupScroll.scrollTo(0, 0));
        }

        body.setAlpha(0f);
        body.setTranslationX(dp(16));
        body.animate().alpha(1f).translationX(0).setDuration(220).start();

        dots.removeAllViews();
        for (int i = 0; i < 3; i++) {
            View d = new View(this);
            boolean on = i == page;
            d.setBackground(UiHelper.pillBg(this, on ? UiHelper.COL_PRIMARY : UiHelper.COL_OUTLINE, 0, 99));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(on ? 22 : 8), dp(8));
            lp.leftMargin = dp(4);
            lp.rightMargin = dp(4);
            dots.addView(d, lp);
        }
        skipBtn.setVisibility(page == 0 || (page == 1 && understood) ? View.VISIBLE : View.INVISIBLE);
        styleNext();
    }

    private void pageIntro() {
        LinearLayout demo = UiHelper.card(this);
        demo.setLayoutParams(UiHelper.matchWrap(this, 0));
        demo.setPadding(dp(18), dp(18), dp(18), dp(14));
        demo.setBackground(UiHelper.gradientBg(this, 26, 0xFF0E2233, UiHelper.COL_SURFACE));
        demo.addView(new PacingView(this), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
        TextView cap = UiHelper.text(this, getString(R.string.ob1_demo), 12, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
        cap.setGravity(Gravity.CENTER);
        demo.addView(cap, UiHelper.matchWrap(this, 6));
        body.addView(demo);

        title(R.string.ob1_title, 30);
        subtitle(getString(R.string.ob1_sub));

        LinearLayout chips = new LinearLayout(this);
        chips.setLayoutParams(UiHelper.matchWrap(this, 22));
        int[] labels = {R.string.ob1_chip_root, R.string.ob1_chip_pc, R.string.ob1_chip_battery};
        for (int i = 0; i < labels.length; i++) {
            TextView c = UiHelper.chip(this, getString(labels[i]), UiHelper.COL_TEAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.leftMargin = dp(8);
            chips.addView(c, lp);
        }
        body.addView(chips);
    }

    private void pageWhatsNew() {
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(UiHelper.gradientBg(this, 24, UiHelper.COL_TEAL, 0xFF5EEAD4));
        badge.addView(UiHelper.icon(this, R.drawable.ui_check, UiHelper.COL_ON_PRIMARY, 34),
            new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        body.addView(badge, new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView t = UiHelper.text(this, getString(R.string.wn_title, FpsApplier.appVersion(this)), 30,
            UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        t.setLetterSpacing(-0.02f);
        t.setPadding(0, dp(22), 0, dp(10));
        body.addView(t);

        // Reassurance first: nothing was reset
        LinearLayout kept = new LinearLayout(this);
        kept.setGravity(Gravity.CENTER_VERTICAL);
        kept.setPadding(dp(14), dp(12), dp(14), dp(12));
        kept.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_TEAL, 0.1f),
            UiHelper.alpha(UiHelper.COL_TEAL, 0.4f), 16));
        ImageView ki = UiHelper.icon(this, R.drawable.ui_check, UiHelper.COL_TEAL, 18);
        ((LinearLayout.LayoutParams) ki.getLayoutParams()).rightMargin = dp(10);
        kept.addView(ki);
        TextView kt = UiHelper.text(this, getString(R.string.wn_kept), 14.5f, UiHelper.COL_TEAL, UiHelper.TF_MEDIUM);
        kt.setLineSpacing(dp(2), 1f);
        kept.addView(kt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        kept.setLayoutParams(UiHelper.matchWrap(this, 6));
        body.addView(kept);

        TextView lbl = UiHelper.overline(this, getString(R.string.wn_new), UiHelper.COL_DIM);
        lbl.setPadding(dp(4), dp(24), 0, dp(4));
        body.addView(lbl);
        LinearLayout list = UiHelper.card(this);
        list.setPadding(dp(12), dp(2), dp(12), dp(2));
        int[][] items = {
            {R.drawable.ui_gauge, R.string.wn_tester},
            {R.drawable.ui_info, R.string.wn_faq},
            {R.drawable.ui_zap, R.string.wn_rates},
            {R.drawable.ui_settings, R.string.wn_check},
        };
        for (int i = 0; i < items.length; i++) {
            if (i > 0) list.addView(UiHelper.divider(this, 56));
            list.addView(UiHelper.row(this, items[i][0], UiHelper.COL_PRIMARY, getString(items[i][1]), null, null, null));
        }
        body.addView(list);

        TextView next = UiHelper.text(this, getString(R.string.wn_next), 13.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        next.setPadding(dp(4), dp(18), 0, 0);
        next.setLineSpacing(dp(3), 1f);
        body.addView(next);
    }

    private void pageRequirements() {
        title(R.string.ob2_before, 30);
        subtitle(getString(R.string.ob2_sub));

        // Two requirement tiles
        int max = FpsApplier.getMaxSupportedFps(this);
        boolean screenOk = max >= 90;
        LinearLayout req = new LinearLayout(this);
        req.setLayoutParams(UiHelper.matchWrap(this, 20));
        req.addView(reqTile(R.drawable.ui_gauge, screenOk ? UiHelper.COL_TEAL : UiHelper.COL_WARN,
                getString(R.string.ob2_req_screen),
                screenOk ? getString(R.string.ob2_req_screen_ok, max) : getString(R.string.ob2_req_screen_low, max)),
            weight(0, 5));
        req.addView(reqTile(R.drawable.ui_gamepad, UiHelper.COL_PRIMARY,
            getString(R.string.ob2_req_game), getString(R.string.ob2_req_game_hint)), weight(5, 0));
        body.addView(req);

        // Tag legend: three big pills
        TextView legend = UiHelper.overline(this, getString(R.string.ob2_tags_title), UiHelper.COL_DIM);
        legend.setPadding(dp(4), dp(22), 0, dp(10));
        body.addView(legend);
        LinearLayout tags = new LinearLayout(this);
        tags.addView(tagPill(R.string.tag_120, UiHelper.COL_TEAL, R.string.ob2_cap120), weight(0, 4));
        tags.addView(tagPill(R.string.tag_90, UiHelper.COL_PRIMARY, R.string.ob2_cap90), weight(4, 4));
        tags.addView(tagPill(R.string.tag_60, UiHelper.COL_WARN, R.string.ob2_cap60), weight(4, 0));
        body.addView(tags);

        TextView ack = UiHelper.button(this, "", R.drawable.ui_check, false, null);
        ((LinearLayout.LayoutParams) ack.getLayoutParams()).topMargin = dp(22);
        ack.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            understood = true;
            styleAck(ack);
            styleNext();
            skipBtn.setVisibility(View.VISIBLE);
        });
        styleAck(ack);
        body.addView(ack);
    }

    private void pageKeepRunning() {
        title(R.string.ob3_title, 30);
        subtitle(getString(R.string.ob3_sub));

        int rate = FpsApplier.getSystemRefreshSetting(this);
        refreshTile = stepTile(1, R.drawable.ui_gauge, getString(R.string.ob3_s_refresh),
            rate >= 90, rate >= 90 ? getString(R.string.hz_value, rate) : null, v -> UiHelper.openDisplaySettings(this));
        ((LinearLayout.LayoutParams) refreshTile.getLayoutParams()).topMargin = dp(20);
        body.addView(refreshTile);
        if (!screenReady()) {
            int message = FpsApplier.getMaxSupportedFps(this) < 90
                ? R.string.ob3_panel_limit : R.string.ob3_low_setting;
            TextView advice = UiHelper.text(this, getString(message), 13, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
            advice.setLineSpacing(dp(3), 1f);
            body.addView(advice, UiHelper.matchWrap(this, 8));
        }
        batteryTile = stepTile(2, R.drawable.ui_zap, getString(R.string.ob3_s_battery),
            UiHelper.isBatteryUnrestricted(this), null, v -> UiHelper.requestBatteryExemption(this));
        body.addView(batteryTile);
        body.addView(stepTile(3, R.drawable.ui_settings, getString(R.string.ob3_s_autostart),
            false, null, v -> UiHelper.openAutostart(this)));

        LinearLayout coffee = UiHelper.card(this);
        coffee.setLayoutParams(UiHelper.matchWrap(this, 22));
        coffee.setPadding(dp(12), dp(2), dp(12), dp(2));
        coffee.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.07f),
            UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.3f), 20));
        coffee.addView(UiHelper.row(this, R.drawable.ui_coffee, UiHelper.COL_COFFEE_BG,
            getString(R.string.about_coffee), null, null,
            v -> UiHelper.openUrl(this, UiHelper.BUY_ME_A_COFFEE_URL)));
        body.addView(coffee);
    }

    private void addSetupFaq(String... ids) {
        LinearLayout faq = UiHelper.card(this);
        faq.setLayoutParams(UiHelper.matchWrap(this, 18));
        faq.setPadding(dp(16), dp(14), dp(16), dp(4));
        faq.addView(UiHelper.overline(this, getString(R.string.faq_title), UiHelper.COL_PRIMARY));
        for (String id : ids) {
            for (FaqData.Item item : FaqData.items()) {
                if (!id.equals(item.id)) continue;
                faq.addView(UiHelper.divider(this, 0));
                faq.addView(UiHelper.faqItem(this, item));
                break;
            }
        }
        body.addView(faq);
    }

    // ---------------------------------------------------------------- building blocks

    private void title(int res, float sp) {
        TextView t = UiHelper.text(this, getString(res), sp, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        t.setLetterSpacing(-0.02f);
        t.setPadding(0, page == 0 ? dp(26) : 0, 0, dp(10));
        body.addView(t);
    }

    private void subtitle(String s) {
        TextView b = UiHelper.text(this, s, 16, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        b.setLineSpacing(dp(4), 1f);
        body.addView(b);
    }

    private LinearLayout.LayoutParams weight(int leftDp, int rightDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = dp(leftDp);
        lp.rightMargin = dp(rightDp);
        return lp;
    }

    private View reqTile(int icon, int color, String label, String detail) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(16), dp(18), dp(16), dp(16));
        t.setBackground(UiHelper.pillBg(this, UiHelper.alpha(color, 0.08f), UiHelper.alpha(color, 0.35f), 22));
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(UiHelper.pillBg(this, UiHelper.alpha(color, 0.16f), 0, 16));
        badge.addView(UiHelper.icon(this, icon, color, 28), new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        t.addView(badge, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView l = UiHelper.text(this, label, 16, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        l.setPadding(0, dp(14), 0, dp(6));
        t.addView(l);
        TextView d = UiHelper.text(this, detail, 13, color, UiHelper.TF_MEDIUM);
        d.setLineSpacing(dp(2), 1f);
        t.addView(d);
        return t;
    }

    private View tagPill(int tagRes, int color, int capRes) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setGravity(Gravity.CENTER_HORIZONTAL);
        p.setPadding(dp(6), dp(14), dp(6), dp(14));
        p.setBackground(UiHelper.pillBg(this, UiHelper.alpha(color, 0.1f), UiHelper.alpha(color, 0.35f), 18));
        TextView tag = UiHelper.text(this, getString(tagRes), 14, color, UiHelper.TF_BOLD);
        tag.setLetterSpacing(0.04f);
        tag.setGravity(Gravity.CENTER);
        tag.setSingleLine(true);
        p.addView(tag);
        TextView cap = UiHelper.text(this, getString(capRes), 12, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        cap.setGravity(Gravity.CENTER);
        cap.setPadding(0, dp(6), 0, 0);
        p.addView(cap);
        return p;
    }

    /** Checklist tile: number (or ✓ when done), icon, label, and "Open" / value on the right. */
    private View stepTile(int n, int icon, String label, boolean done, String value, View.OnClickListener open) {
        LinearLayout t = new LinearLayout(this);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(14), dp(14), dp(16), dp(14));
        int color = done ? UiHelper.COL_TEAL : UiHelper.COL_PRIMARY;
        t.setBackground(UiHelper.ripple(this, UiHelper.pillBg(this, done ? UiHelper.alpha(UiHelper.COL_TEAL, 0.08f) : UiHelper.COL_SURFACE,
            done ? UiHelper.alpha(UiHelper.COL_TEAL, 0.4f) : UiHelper.COL_OUTLINE, 20), 20));
        t.setOnClickListener(open);

        FrameLayout num = new FrameLayout(this);
        num.setBackground(done ? UiHelper.gradientBg(this, 99, UiHelper.COL_TEAL, 0xFF5EEAD4)
            : UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_PRIMARY, 0.14f), 0, 99));
        if (done) {
            num.addView(UiHelper.icon(this, R.drawable.ui_check, UiHelper.COL_ON_PRIMARY, 18),
                new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
        } else {
            TextView nt = UiHelper.text(this, String.valueOf(n), 15, UiHelper.COL_PRIMARY, UiHelper.TF_BOLD);
            num.addView(nt, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(dp(34), dp(34));
        np.rightMargin = dp(12);
        t.addView(num, np);
        ImageView ic = UiHelper.icon(this, icon, color, 20);
        ((LinearLayout.LayoutParams) ic.getLayoutParams()).rightMargin = dp(12);
        t.addView(ic);
        TextView l = UiHelper.text(this, label, 15, UiHelper.COL_TEXT, UiHelper.TF_MEDIUM);
        l.setLineSpacing(dp(2), 1f);
        t.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView r = UiHelper.text(this, value != null ? value : getString(done ? R.string.ob3_done : R.string.ob3_open),
            13, done ? UiHelper.COL_TEAL : UiHelper.COL_PRIMARY, UiHelper.TF_BOLD);
        r.setPadding(dp(10), 0, 0, 0);
        t.addView(r);
        t.setLayoutParams(UiHelper.matchWrap(this, 10));
        return t;
    }

    private void styleAck(TextView ack) {
        ack.setText(getString(understood ? R.string.ob2_understood : R.string.ob2_understand));
        ack.setTextColor(understood ? UiHelper.COL_ON_PRIMARY : UiHelper.COL_TEXT);
        if (understood) {
            ack.setBackground(UiHelper.ripple(this, UiHelper.gradientBg(this, 18, UiHelper.COL_TEAL, 0xFF5EEAD4), 18));
        } else {
            UiHelper.styleButton(this, ack, false);
        }
        android.graphics.drawable.Drawable[] d = ack.getCompoundDrawables();
        if (d[0] != null) d[0].setTint(understood ? UiHelper.COL_ON_PRIMARY : UiHelper.COL_TEXT);
    }

    private void styleNext() {
        boolean needAck = page == 1 && !understood;
        nextBtn.setEnabled(!needAck);
        nextBtn.setAlpha(needAck ? 0.4f : 1f);
        if (needAck) nextBtn.setText(getString(R.string.ob2_understand_first));
        else if (page == 2 && !screenReady()) nextBtn.setText(getString(R.string.ob3_continue_test));
        else nextBtn.setText(getString(page < 2 ? R.string.ob_next : R.string.ob_done));
    }

    private int dp(float v) {
        return UiHelper.dp(this, v);
    }
}
