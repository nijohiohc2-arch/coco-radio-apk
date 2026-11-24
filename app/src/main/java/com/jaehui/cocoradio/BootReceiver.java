package com.jaehui.cocoradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!shouldAutoStart(action)) return;

        // Play last station in the service even if the UI cannot come to the front
        // (Android 10+ background activity limits). This is what car navis need.
        RadioService.enqueueAutoPlay(context);

        try {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } catch (Exception ignored) {
        }
    }

    private static boolean shouldAutoStart(String action) {
        if (action == null) return false;
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) return true;
        if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) return true;
        if ("android.intent.action.QUICKBOOT_POWERON".equals(action)) return true;
        if ("com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) return true;
        if ("android.intent.action.ACC_ON".equals(action)) return true;
        if ("android.intent.action.ACTION_ACC_ON".equals(action)) return true;
        if ("com.android.action.ACC_ON".equals(action)) return true;
        return action.endsWith("ACC_ON") || action.contains("QUICKBOOT");
    }
}
