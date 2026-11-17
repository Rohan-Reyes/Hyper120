package com.custom.game120hz;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Home-screen widget: target rate, on/off and a one-tap switch to the next rate. */
public class Hyper120Widget extends AppWidgetProvider {
    private static final String ACTION_TOGGLE = "com.custom.game120hz.widget.TOGGLE";
    private static final String ACTION_NEXT_RATE = "com.custom.game120hz.widget.NEXT_RATE";

    /** Redraws every placed widget. Safe to call from anywhere state changes. */
    public static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, Hyper120Widget.class));
        if (ids == null || ids.length == 0) return;
        mgr.updateAppWidget(ids, build(ctx));
    }

    /** Asks the launcher to place the widget (shows the system preview dialog). */
    public static boolean requestPin(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        if (!mgr.isRequestPinAppWidgetSupported()) return false;
        return mgr.requestPinAppWidget(new ComponentName(ctx, Hyper120Widget.class), null, null);
    }

    /**
     * HyperOS / MIUI gate widget and shortcut pinning behind the per-app "Home screen shortcuts"
     * permission (MIUI app-op 10017). When it is off, pin requests are dropped without any UI.
     * Returns true if the permission is granted or the ROM has no such permission.
     */
    public static boolean isMiuiShortcutAllowed(Context ctx) {
        try {
            android.app.AppOpsManager ops = (android.app.AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
            java.lang.reflect.Method m = android.app.AppOpsManager.class.getMethod(
                "checkOpNoThrow", int.class, int.class, String.class);
            int mode = (Integer) m.invoke(ops, 10017, android.os.Process.myUid(), ctx.getPackageName());
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) {
            return true;
        }
    }

    /** HyperOS / MIUI per-app permission screen; falls back to App info. */
    public static void openMiuiPermissions(android.app.Activity act) {
        try {
            Intent i = new Intent("miui.intent.action.APP_PERM_EDITOR");
            i.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
            i.putExtra("extra_pkgname", act.getPackageName());
            act.startActivity(i);
        } catch (Exception e) {
            UiHelper.openAppInfo(act);
        }
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        mgr.updateAppWidget(ids, build(ctx));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String action = intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            boolean next = !FpsApplier.isAutoEnabled(ctx);
            FpsApplier.setAutoEnabled(ctx, next);
            Intent svc = new Intent(ctx, Auto120Service.class);
            if (next) {
                ctx.startForegroundService(svc);
                FpsApplier.apply120Hz(ctx, FpsApplier.syncInstalledGames(ctx));
            } else {
                ctx.stopService(svc);
            }
        } else if (ACTION_NEXT_RATE.equals(action)) {
            FpsApplier.setTargetFps(ctx, nextRate(ctx));
            FpsApplier.apply120Hz(ctx, FpsApplier.loadPackages(ctx));
            if (FpsApplier.isAutoEnabled(ctx)) ctx.startForegroundService(new Intent(ctx, Auto120Service.class));
        } else {
            return;
        }
        Hyper120TileService.requestTileSync(ctx);
        updateAll(ctx);
    }

    private static int nextRate(Context ctx) {
        int[] rates = FpsApplier.detectSupportedFps(ctx);
        int cur = FpsApplier.getTargetFps(ctx);
        for (int r : rates) if (r > cur) return r;
        return rates[0];
    }

    private static RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_hyper120);
        boolean on = FpsApplier.isAutoEnabled(ctx);
        int fps = FpsApplier.getTargetFps(ctx);
        int apps = FpsApplier.loadPackages(ctx).size();

        v.setTextViewText(R.id.widget_hz, String.valueOf(fps));
        v.setTextViewText(R.id.widget_status, "● " + ctx.getString(on ? R.string.widget_status_on : R.string.widget_status_off));
        v.setTextColor(R.id.widget_status, on ? UiHelper.COL_TEAL : UiHelper.COL_DIM);
        v.setTextViewText(R.id.widget_apps, ctx.getResources().getQuantityString(R.plurals.apps_count, apps, apps));
        v.setTextViewText(R.id.widget_toggle, ctx.getString(on ? R.string.widget_turn_off : R.string.widget_turn_on));
        v.setTextViewText(R.id.widget_rate, "→ " + nextRate(ctx) + "Hz");

        v.setOnClickPendingIntent(R.id.widget_toggle, broadcast(ctx, ACTION_TOGGLE, 1));
        v.setOnClickPendingIntent(R.id.widget_rate, broadcast(ctx, ACTION_NEXT_RATE, 2));
        v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(ctx, 3,
            new Intent(ctx, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return v;
    }

    private static PendingIntent broadcast(Context ctx, String action, int code) {
        Intent i = new Intent(ctx, Hyper120Widget.class).setAction(action);
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
