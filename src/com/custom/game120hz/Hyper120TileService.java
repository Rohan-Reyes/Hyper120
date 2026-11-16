package com.custom.game120hz;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import java.util.Set;

/** Quick Settings tile to toggle Hyper120 background override and show target FPS. */
public class Hyper120TileService extends TileService {

    public static void requestTileSync(Context ctx) {
        try {
            TileService.requestListeningState(
                ctx, new ComponentName(ctx, Hyper120TileService.class));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean next = !FpsApplier.isAutoEnabled(this);
        FpsApplier.setAutoEnabled(this, next);
        Intent svc = new Intent(this, Auto120Service.class);
        if (next) {
            startForegroundService(svc);
            FpsApplier.apply120Hz(this, FpsApplier.syncInstalledGames(this));
        } else {
            stopService(svc);
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean active = FpsApplier.isAutoEnabled(this);
        int fps = FpsApplier.getTargetFps(this);
        Set<String> pkgs = FpsApplier.loadPackages(this);
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Hyper120");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(active ? (fps + "Hz \u00B7 " + pkgs.size() + " apps") : "Off");
        }
        tile.updateTile();
    }
}
