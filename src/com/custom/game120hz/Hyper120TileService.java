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
        Hyper120Widget.updateAll(this);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean active = FpsApplier.isAutoEnabled(this);
        int fps = FpsApplier.getTargetFps(this);
        Set<String> pkgs = FpsApplier.loadPackages(this);
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.app_name));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(active ? getString(R.string.tile_sub, fps, pkgs.size()) : getString(R.string.tile_off));
        }
        tile.updateTile();
    }
}
