package com.custom.game120hz;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Three-page first-launch guide: what it does, game limits, keep it running. */
public class OnboardingActivity extends Activity {
    private int page = 0;
    private LinearLayout body;
    private LinearLayout dots;
    private TextView nextBtn;
    private TextView skipBtn;
    private TextView batteryBtn;
    private TextView refreshBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiHelper.setupDarkBars(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COL_BG);
        UiHelper.applyNotchSafePadding(this, root, 24, 16, 24);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        top.addView(logo, new LinearLayout.LayoutParams(dp(40), dp(40)));
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        skipBtn = UiHelper.text(this, getString(R.string.ob_skip), 15, UiHelper.COL_MUTED, UiHelper.TF_MEDIUM);
        skipBtn.setPadding(dp(14), dp(10), dp(4), dp(10));
        skipBtn.setOnClickListener(v -> finishGuide());
        top.addView(skipBtn);
        root.addView(top);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(36), 0, dp(24));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, 0, 0, dp(16));
        root.addView(dots, UiHelper.matchWrap(this, 0));

        nextBtn = UiHelper.button(this, "", 0, true, v -> {
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
        updateBatteryBtn();
    }

    @Override
    public void onBackPressed() {
        if (page > 0) {
            page--;
            render();
        } else {
            super.onBackPressed();
        }
    }

    private void finishGuide() {
        FpsApplier.setOnboarded(this);
        finish();
    }

    private void render() {
        body.removeAllViews();
        batteryBtn = null;
        refreshBtn = null;
        int[] titles = {R.string.ob1_title, R.string.ob2_title, R.string.ob3_title};
        int[] bodies = {R.string.ob1_body, R.string.ob2_body, R.string.ob3_body};
        int[] icons = {R.drawable.ui_zap, R.drawable.ui_gamepad, R.drawable.ui_settings};
        int[] colors = {UiHelper.COL_PRIMARY, UiHelper.COL_WARN, UiHelper.COL_TEAL};

        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(UiHelper.pillBg(this, UiHelper.alpha(colors[page], 0.12f),
            UiHelper.alpha(colors[page], 0.3f), 22));
        badge.addView(UiHelper.icon(this, icons[page], colors[page], 30),
            new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        body.addView(badge, new LinearLayout.LayoutParams(dp(68), dp(68)));

        TextView t = UiHelper.text(this, getString(titles[page]), 28, UiHelper.COL_TEXT, UiHelper.TF_BOLD);
        t.setLetterSpacing(-0.02f);
        t.setPadding(0, dp(26), 0, dp(14));
        body.addView(t);
        TextView b = UiHelper.text(this, getString(bodies[page]), 16, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        b.setLineSpacing(dp(5), 1f);
        body.addView(b);

        if (page == 1) {
            LinearLayout tags = UiHelper.card(this);
            tags.setLayoutParams(UiHelper.matchWrap(this, 22));
            tags.addView(tagRow(R.string.tag_120, UiHelper.COL_TEAL, R.string.ob2_tag120));
            tags.addView(tagRow(R.string.tag_90, UiHelper.COL_PRIMARY, R.string.ob2_tag90));
            tags.addView(tagRow(R.string.tag_60, UiHelper.COL_WARN, R.string.ob2_tag60));
            body.addView(tags);
        } else if (page == 2) {
            batteryBtn = UiHelper.button(this, "", R.drawable.ui_zap, false,
                v -> UiHelper.requestBatteryExemption(this));
            ((LinearLayout.LayoutParams) batteryBtn.getLayoutParams()).topMargin = dp(24);
            body.addView(batteryBtn);
            TextView bh = UiHelper.text(this, getString(R.string.ob3_battery_hint), 13,
                UiHelper.COL_DIM, UiHelper.TF_REGULAR);
            bh.setPadding(dp(6), dp(8), 0, dp(4));
            body.addView(bh);
            TextView auto = UiHelper.button(this, getString(R.string.ob3_autostart), R.drawable.ui_settings, false,
                v -> UiHelper.openAutostart(this));
            ((LinearLayout.LayoutParams) auto.getLayoutParams()).topMargin = dp(10);
            body.addView(auto);
            TextView hint = UiHelper.text(this, getString(R.string.ob3_autostart_hint), 13,
                UiHelper.COL_DIM, UiHelper.TF_REGULAR);
            hint.setPadding(dp(6), dp(10), 0, 0);
            body.addView(hint);
            refreshBtn = UiHelper.button(this, "", R.drawable.ui_gauge, false, v -> UiHelper.openDisplaySettings(this));
            ((LinearLayout.LayoutParams) refreshBtn.getLayoutParams()).topMargin = dp(14);
            body.addView(refreshBtn);
            TextView rh = UiHelper.text(this, getString(R.string.ob3_refresh_hint), 13, UiHelper.COL_DIM, UiHelper.TF_REGULAR);
            rh.setPadding(dp(6), dp(10), 0, 0);
            body.addView(rh);

            // Optional support card, kept visually separate from the setup steps
            LinearLayout coffee = UiHelper.card(this);
            coffee.setLayoutParams(UiHelper.matchWrap(this, 28));
            coffee.setPadding(dp(14), dp(4), dp(14), dp(4));
            coffee.setBackground(UiHelper.pillBg(this, UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.07f),
                UiHelper.alpha(UiHelper.COL_COFFEE_BG, 0.3f), 22));
            coffee.addView(UiHelper.row(this, R.drawable.ui_coffee, UiHelper.COL_COFFEE_BG,
                getString(R.string.about_coffee), getString(R.string.about_coffee_sub), null,
                v -> UiHelper.openUrl(this, UiHelper.BUY_ME_A_COFFEE_URL)));
            body.addView(coffee);
            updateBatteryBtn();
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
        nextBtn.setText(getString(page < 2 ? R.string.ob_next : R.string.ob_done));
        skipBtn.setVisibility(page < 2 ? View.VISIBLE : View.INVISIBLE);
    }

    private View tagRow(int tagRes, int color, int descRes) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(2), dp(8), 0, dp(8));
        TextView tag = UiHelper.text(this, getString(tagRes), 13, color, UiHelper.TF_BOLD);
        tag.setLetterSpacing(0.06f);
        r.addView(tag);
        TextView d = UiHelper.text(this, getString(descRes), 14, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
        d.setPadding(0, dp(5), 0, 0);
        d.setLineSpacing(dp(3), 1f);
        r.addView(d);
        return r;
    }

    private void updateBatteryBtn() {
        if (refreshBtn != null) {
            int v = FpsApplier.getSystemRefreshSetting(this);
            boolean ok = v >= 90 || v < 0;
            refreshBtn.setText(v >= 90 ? getString(R.string.ob3_refresh_done, v) : getString(R.string.ob3_refresh));
            refreshBtn.setTextColor(v >= 90 ? UiHelper.COL_TEAL : UiHelper.COL_TEXT);
        }
        if (batteryBtn == null) return;
        boolean ok = UiHelper.isBatteryUnrestricted(this);
        batteryBtn.setText(getString(ok ? R.string.ob3_battery_done : R.string.ob3_battery));
        batteryBtn.setTextColor(ok ? UiHelper.COL_TEAL : UiHelper.COL_TEXT);
        batteryBtn.setEnabled(!ok);
    }

    private int dp(float v) {
        return UiHelper.dp(this, v);
    }
}
