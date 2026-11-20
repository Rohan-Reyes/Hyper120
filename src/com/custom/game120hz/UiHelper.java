package com.custom.game120hz;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** Hyper120 design system: colour tokens, type scale and small view builders. */
public final class UiHelper {
    public static final String BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/rohan_reyes";
    public static final String GITHUB_REPO_URL = "https://github.com/Rohan-Reyes/Hyper120";

    // Colour tokens
    public static final int COL_BG = Color.parseColor("#0A0D14");
    public static final int COL_SURFACE = Color.parseColor("#11161F");
    public static final int COL_SURFACE_2 = Color.parseColor("#181E2A");
    public static final int COL_SURFACE_SEL = Color.parseColor("#0F2231");
    public static final int COL_OUTLINE = Color.parseColor("#222A3A");
    public static final int COL_DIVIDER = Color.parseColor("#1A202C");
    public static final int COL_PRIMARY = Color.parseColor("#38BDF8");
    public static final int COL_INDIGO = Color.parseColor("#6366F1");
    public static final int COL_TEAL = Color.parseColor("#2DD4BF");
    public static final int COL_ON_PRIMARY = Color.parseColor("#04121C");
    public static final int COL_TEXT = Color.parseColor("#F1F5F9");
    public static final int COL_MUTED = Color.parseColor("#8B97AC");
    public static final int COL_DIM = Color.parseColor("#5A6478");
    public static final int COL_WARN = Color.parseColor("#FBBF24");
    public static final int COL_ERROR = Color.parseColor("#F87171");
    public static final int COL_COFFEE_BG = Color.parseColor("#FFDD00");
    public static final int COL_COFFEE_FG = Color.parseColor("#101820");

    public static final Typeface TF_REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface TF_MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    public static final Typeface TF_BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    public static final Typeface TF_DISPLAY = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private UiHelper() {}

    public static int dp(Activity act, float v) {
        return Math.round(v * act.getResources().getDisplayMetrics().density);
    }

    public static int alpha(int color, float a) {
        return (Math.round(a * 255) << 24) | (color & 0x00FFFFFF);
    }

    public static void setupDarkBars(Activity act) {
        Window w = act.getWindow();
        w.setStatusBarColor(COL_BG);
        w.setNavigationBarColor(COL_BG);
    }

    // ---------- Drawables ----------

    public static GradientDrawable pillBg(Activity act, int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(act, radiusDp));
        if (strokeColor != 0) {
            gd.setStroke(dp(act, 1), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable gradientBg(Activity act, int radiusDp, int... colors) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        gd.setCornerRadius(dp(act, radiusDp));
        return gd;
    }

    /** Wraps a background in a ripple so taps get Material feedback. */
    public static Drawable ripple(Activity act, Drawable content, int radiusDp) {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(act, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(0x22FFFFFF), content, mask);
    }

    // ---------- Text ----------

    public static TextView text(Activity act, CharSequence s, float sp, int color, Typeface tf) {
        TextView tv = new TextView(act);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(tf);
        tv.setIncludeFontPadding(false);
        return tv;
    }

    /** Small all-caps section label. */
    public static TextView overline(Activity act, String s, int color) {
        TextView tv = text(act, s.toUpperCase(), 11, color, TF_BOLD);
        tv.setLetterSpacing(0.12f);
        return tv;
    }

    // ---------- Icons ----------

    public static ImageView icon(Activity act, int resId, int color, int sizeDp) {
        ImageView iv = new ImageView(act);
        iv.setImageResource(resId);
        iv.setImageTintList(ColorStateList.valueOf(color));
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(act, sizeDp), dp(act, sizeDp)));
        return iv;
    }

    /** Icon inside a tinted rounded square, used at the start of list rows. */
    public static FrameLayout iconBadge(Activity act, int resId, int color) {
        FrameLayout f = new FrameLayout(act);
        f.setBackground(pillBg(act, alpha(color, 0.12f), 0, 12));
        ImageView iv = icon(act, resId, color, 20);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(act, 20), dp(act, 20), Gravity.CENTER);
        f.addView(iv, lp);
        LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(dp(act, 40), dp(act, 40));
        outer.rightMargin = dp(act, 14);
        f.setLayoutParams(outer);
        return f;
    }

    /** Circular 44dp icon button. */
    public static FrameLayout iconButton(Activity act, int resId, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(act);
        f.setBackground(ripple(act, pillBg(act, COL_SURFACE, COL_OUTLINE, 22), 22));
        f.addView(icon(act, resId, COL_TEXT, 20),
            new FrameLayout.LayoutParams(dp(act, 20), dp(act, 20), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 44), dp(act, 44)));
        f.setOnClickListener(l);
        f.setClickable(true);
        return f;
    }

    // ---------- Layout pieces ----------

    public static LinearLayout card(Activity act) {
        LinearLayout c = new LinearLayout(act);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(pillBg(act, COL_SURFACE, COL_OUTLINE, 24));
        c.setPadding(dp(act, 18), dp(act, 18), dp(act, 18), dp(act, 18));
        c.setLayoutParams(matchWrap(act, 12));
        return c;
    }

    public static LinearLayout.LayoutParams matchWrap(Activity act, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(act, topMarginDp);
        return lp;
    }

    public static View divider(Activity act, int insetStartDp) {
        View v = new View(act);
        v.setBackgroundColor(COL_DIVIDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(act, 1)));
        lp.leftMargin = dp(act, insetStartDp);
        v.setLayoutParams(lp);
        return v;
    }

    /** Top bar for secondary screens: back button + title. */
    public static LinearLayout topBar(Activity act, String title) {
        LinearLayout bar = new LinearLayout(act);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, 0, 0, dp(act, 8));
        bar.addView(iconButton(act, R.drawable.ui_back, v -> act.finish()));
        TextView t = text(act, title, 20, COL_TEXT, TF_BOLD);
        t.setPadding(dp(act, 14), 0, 0, 0);
        bar.addView(t);
        return bar;
    }

    /**
     * A list row: icon badge, title, optional subtitle and an optional trailing view.
     * Returns the row; clickable when {@code l} is non-null.
     */
    public static LinearLayout row(Activity act, int iconRes, int iconColor, String title,
                                   String subtitle, View trailing, View.OnClickListener l) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(act, 4), dp(act, 12), dp(act, 4), dp(act, 12));
        row.setMinimumHeight(dp(act, 60));
        if (iconRes != 0) {
            row.addView(iconBadge(act, iconRes, iconColor));
        }
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = text(act, title, 15.5f, COL_TEXT, TF_MEDIUM);
        col.addView(t);
        if (subtitle != null) {
            TextView s = text(act, subtitle, 12.5f, COL_MUTED, TF_REGULAR);
            s.setPadding(0, dp(act, 5), 0, 0);
            s.setLineSpacing(dp(act, 2), 1f);
            s.setTag("subtitle");
            col.addView(s);
        }
        row.addView(col);
        if (trailing != null) {
            row.addView(trailing);
        } else if (l != null) {
            row.addView(icon(act, R.drawable.ui_chevron, COL_DIM, 18));
        }
        if (l != null) {
            row.setBackground(ripple(act, null, 14));
            row.setOnClickListener(l);
        }
        return row;
    }

    public static TextView subtitleOf(View row) {
        return (TextView) row.findViewWithTag("subtitle");
    }

    static View faqItem(Activity act, FaqData.Item item) {
        LinearLayout q = new LinearLayout(act);
        q.setOrientation(LinearLayout.VERTICAL);
        q.setPadding(0, dp(act, 13), 0, dp(act, 13));
        LinearLayout qrow = new LinearLayout(act);
        qrow.setGravity(Gravity.CENTER_VERTICAL);
        TextView qt = UiHelper.text(act, item.question, 14.5f, UiHelper.COL_TEXT, UiHelper.TF_MEDIUM);
        qrow.addView(qt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView plus = UiHelper.text(act, "+", 18, UiHelper.COL_PRIMARY, UiHelper.TF_BOLD);
        plus.setPadding(dp(act, 10), 0, 0, 0);
        qrow.addView(plus);
        q.addView(qrow);
        View answer = formatAnswer(act, item.answer);
        answer.setVisibility(View.GONE);
        q.addView(answer);
        q.setOnClickListener(v -> {
            boolean show = answer.getVisibility() != View.VISIBLE;
            answer.setVisibility(show ? View.VISIBLE : View.GONE);
            plus.setText(show ? "\u2212" : "+");
            qt.setTextColor(show ? UiHelper.COL_PRIMARY : UiHelper.COL_TEXT);
        });
        return q;
    }

    /** Renders an answer: paragraphs, "1. " numbered steps, "- " bullets, **bold**. */
    private static View formatAnswer(Activity act, String src) {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(act, 4), dp(act, 8), dp(act, 2));
        for (String raw : src.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String marker = null;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)\\.\\s+(.*)$").matcher(line);
            if (m.matches()) {
                marker = m.group(1);
                line = m.group(2);
            } else if (line.startsWith("- ")) {
                marker = "\u2022";
                line = line.substring(2);
            }
            TextView t = UiHelper.text(act, boldSpans(line), 13.5f, UiHelper.COL_MUTED, UiHelper.TF_REGULAR);
            t.setLineSpacing(dp(act, 3), 1f);
            if (marker == null) {
                t.setPadding(0, dp(act, 6), 0, dp(act, 2));
                box.addView(t);
                continue;
            }
            LinearLayout row = new LinearLayout(act);
            row.setPadding(0, dp(act, 7), 0, 0);
            boolean num = !marker.equals("\u2022");
            TextView mk = UiHelper.text(act, marker, num ? 11 : 15, UiHelper.COL_PRIMARY, UiHelper.TF_BOLD);
            mk.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams mp;
            if (num) {
                mk.setBackground(UiHelper.pillBg(act, UiHelper.alpha(UiHelper.COL_PRIMARY, 0.15f), 0, 99));
                mp = new LinearLayout.LayoutParams(dp(act, 20), dp(act, 20));
            } else {
                mp = new LinearLayout.LayoutParams(dp(act, 20), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            mp.rightMargin = dp(act, 10);
            row.addView(mk, mp);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            box.addView(row);
        }
        return box;
    }

    private static CharSequence boldSpans(String s) {
        android.text.SpannableStringBuilder b = new android.text.SpannableStringBuilder();
        String[] parts = s.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            int st = b.length();
            b.append(parts[i]);
            if (i % 2 == 1) {
                b.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), st, b.length(), 0);
                b.setSpan(new android.text.style.ForegroundColorSpan(UiHelper.COL_TEXT), st, b.length(), 0);
            }
        }
        return b;
    }

    public static Switch styledSwitch(Activity act) {
        Switch sw = new Switch(act);
        int[][] states = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{COL_PRIMARY, Color.parseColor("#9AA5B8")}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{alpha(COL_PRIMARY, 0.45f), COL_OUTLINE}));
        sw.setPadding(dp(act, 12), 0, 0, 0);
        return sw;
    }

    /** Full-width button. Primary = sky→indigo gradient, otherwise outlined surface. */
    public static TextView button(Activity act, String label, int iconRes, boolean primary, View.OnClickListener l) {
        TextView b = text(act, label, 15, primary ? COL_ON_PRIMARY : COL_TEXT, TF_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setPadding(dp(act, 16), 0, dp(act, 16), 0);
        styleButton(act, b, primary);
        if (iconRes != 0) {
            Drawable d = act.getDrawable(iconRes).mutate();
            d.setTint(primary ? COL_ON_PRIMARY : COL_TEXT);
            d.setBounds(0, 0, dp(act, 18), dp(act, 18));
            b.setCompoundDrawables(d, null, null, null);
            b.setCompoundDrawablePadding(dp(act, 8));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 52));
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    public static void styleButton(Activity act, TextView b, boolean primary) {
        Drawable bg = primary
            ? gradientBg(act, 18, COL_PRIMARY, Color.parseColor("#818CF8"))
            : pillBg(act, COL_SURFACE_2, COL_OUTLINE, 18);
        b.setBackground(ripple(act, bg, 18));
    }

    /** Status chip with a leading dot. */
    public static TextView chip(Activity act, String label, int color) {
        TextView c = text(act, label, 11.5f, color, TF_BOLD);
        c.setLetterSpacing(0.06f);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        dot.setSize(dp(act, 7), dp(act, 7));
        c.setCompoundDrawablesWithIntrinsicBounds(dot, null, null, null);
        c.setCompoundDrawablePadding(dp(act, 7));
        c.setPadding(dp(act, 10), dp(act, 6), dp(act, 12), dp(act, 6));
        c.setBackground(pillBg(act, alpha(color, 0.12f), alpha(color, 0.28f), 99));
        return c;
    }

    public static void setChip(Activity act, TextView c, String label, int color) {
        c.setText(label);
        c.setTextColor(color);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        dot.setSize(dp(act, 7), dp(act, 7));
        c.setCompoundDrawablesWithIntrinsicBounds(dot, null, null, null);
        c.setBackground(pillBg(act, alpha(color, 0.12f), alpha(color, 0.28f), 99));
    }

    // ---------- Insets ----------

    /** Applies padding that clears status bars, notches and the gesture bar. */
    public static void applyNotchSafePadding(Activity act, View root, int sideDp, int extraTopDp, int bottomDp) {
        int side = dp(act, sideDp);
        int extraTop = dp(act, extraTopDp);
        int bottom = dp(act, bottomDp);

        root.setPadding(side, dp(act, 40) + extraTop, side, bottom);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets sys = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                topInset = sys.top;
                bottomInset = sys.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        topInset = Math.max(topInset, cutout.getSafeInsetTop());
                    }
                }
            }
            v.setPadding(side, Math.max(topInset, dp(act, 24)) + extraTop, side, bottom + bottomInset);
            return insets;
        });
        root.requestApplyInsets();
    }

    public static void openUrl(Activity act, String url) {
        act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    // ---------- Keep-alive helpers ----------

    public static boolean isBatteryUnrestricted(android.content.Context ctx) {
        android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(android.content.Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
    }

    /**
     * Opens App info, where HyperOS keeps "Battery saver → No restrictions". (We no longer use the
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS prompt: that permission made security scanners flag the app.)
     */
    public static void requestBatteryExemption(Activity act) {
        openAppInfo(act);
    }

    /** HyperOS / MIUI Autostart manager; falls back to App info on other ROMs. */
    public static void openAutostart(Activity act) {
        try {
            Intent i = new Intent();
            i.setComponent(new android.content.ComponentName("com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            act.startActivity(i);
        } catch (Exception e) {
            openAppInfo(act);
        }
    }

    /** Settings → Display, where HyperOS keeps "Refresh rate". */
    public static void openDisplaySettings(Activity act) {
        try {
            act.startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));
        } catch (Exception e) {
            act.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        }
    }

    public static void openAppInfo(Activity act) {
        act.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", act.getPackageName(), null)));
    }

    // ---------- Bottom sheet ----------

    /** Dark rounded sheet anchored to the bottom of the screen. */
    public static android.app.Dialog sheet(Activity act, View content) {
        android.app.Dialog d = new android.app.Dialog(act);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        android.widget.ScrollView sv = new android.widget.ScrollView(act);
        sv.setBackground(pillBg(act, COL_SURFACE, COL_OUTLINE, 28));
        sv.setPadding(dp(act, 20), dp(act, 22), dp(act, 20), dp(act, 20));
        sv.addView(content);
        d.setContentView(sv);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            w.setGravity(android.view.Gravity.BOTTOM);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            lp.y = dp(act, 12);
            w.setAttributes(lp);
            w.getDecorView().setPadding(dp(act, 10), 0, dp(act, 10), 0);
            w.setDimAmount(0.6f);
        }
        return d;
    }

    public static String appVersion(Activity act) {
        try {
            return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
