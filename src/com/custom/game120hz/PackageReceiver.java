package com.custom.game120hz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Set;

/** Automatically detects newly installed games and adds them to the 120Hz override list. */
public class PackageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            Set<String> pkgs = FpsApplier.syncInstalledGames(context);
            FpsApplier.apply120Hz(context, pkgs);
        }
    }
}
