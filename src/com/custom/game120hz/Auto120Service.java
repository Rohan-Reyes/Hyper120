package com.custom.game120hz;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/** Lightweight background service that keeps PowerKeeper's 120Hz overrides active. */
public class Auto120Service extends Service {
    private static final String CHANNEL_ID = "hyper120_bg";
    private static final int NOTIF_ID = 120;
    private static final long REFRESH_INTERVAL_MS = 15000L;

    private Handler handler;
    private PowerManager powerManager;
    private BroadcastReceiver screenReceiver;

    private final Runnable periodicApply = new Runnable() {
        @Override
        public void run() {
            if (powerManager == null || powerManager.isInteractive()) {
                FpsApplier.apply120Hz(Auto120Service.this, FpsApplier.loadPackages(Auto120Service.this));
            }
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        createChannel();
        startForeground(NOTIF_ID, buildNotification());

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    FpsApplier.apply120Hz(context, FpsApplier.loadPackages(context));
                }
            }
        };
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        handler.post(periodicApply);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FpsApplier.apply120Hz(this, FpsApplier.syncInstalledGames(this));
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacks(periodicApply);
        }
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "Hyper120 Background", NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Hyper120 Active")
            .setContentText("Auto-overriding selected apps to 120fps")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }
}
