package com.custom.game120hz;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

/** Shared Material styling, URL, and notch-safe inset helpers for Hyper120. */
public final class UiHelper {
    public static final String BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/rohan_reyes";
    public static final String GITHUB_REPO_URL = "https://github.com/Rohan-Reyes/Hyper120";

    public static final int COL_BG = Color.parseColor("#0E1117");
    public static final int COL_SURFACE = Color.parseColor("#181D27");
    public static final int COL_SURFACE_SEL = Color.parseColor("#172636");
    public static final int COL_OUTLINE = Color.parseColor("#2A3244");
    public static final int COL_PRIMARY = Color.parseColor("#38BDF8");
    public static final int COL_ON_PRIMARY = Color.parseColor("#06202E");
    public static final int COL_COFFEE_BG = Color.parseColor("#FFDD00");
    public static final int COL_COFFEE_FG = Color.parseColor("#101820");
    public static final int COL_TEXT = Color.parseColor("#F1F5F9");
    public static final int COL_MUTED = Color.parseColor("#94A3B8");
    public static final int COL_TEAL = Color.parseColor("#2DD4BF");

    private UiHelper() {}

    public static int dp(Activity act, int v) {
        return Math.round(v * act.getResources().getDisplayMetrics().density);
    }

    public static void setupDarkBars(Activity act) {
        Window w = act.getWindow();
        w.setStatusBarColor(COL_BG);
        w.setNavigationBarColor(COL_BG);
    }

    public static GradientDrawable pillBg(Activity act, int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(act, radiusDp));
        if (strokeColor != 0) {
            gd.setStroke(dp(act, 1), strokeColor);
        }
        return gd;
    }

    /** Applies padding that clears status bars and drop/punch-hole notches cleanly. */
    public static void applyNotchSafePadding(Activity act, View root, int sideDp, int extraTopDp, int bottomDp) {
        int side = dp(act, sideDp);
        int extraTop = dp(act, extraTopDp);
        int bottom = dp(act, bottomDp);

        root.setPadding(side, dp(act, 48) + extraTop, side, bottom);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = 0;
            int bottomInset = 0;
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
            v.setPadding(side, Math.max(topInset, dp(act, 28)) + extraTop, side, bottom + bottomInset);
            return insets;
        });
        root.requestApplyInsets();
    }

    public static void openUrl(Activity act, String url) {
        act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
