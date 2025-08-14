package com.hamit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.MY_PACKAGE_REPLACED".equals(action)) {
            return;
        }
        LogUtils.logToFile(context, "BootReceiver", "Boot/Update olayı yakalandı: " + action);

        SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
        boolean runOnStartup = prefs.getBoolean("runOnStartup", false);
        String deviceName    = prefs.getString("device_name", "");
        String emailName     = prefs.getString("email_name", "");

        if (!runOnStartup) {
            LogUtils.logToFile(context, "BootReceiver", "runOnStartup=false, servis başlatılmayacak");
            return;
        }
        if (deviceName.isEmpty() || emailName.isEmpty()) {
            LogUtils.logToFile(context, "BootReceiver", "device_name veya email_name boş, servis başlatılmadı");

            return;
        }

        // Bazı üniteler boot sonrası kısa sürede servis başlatmayı sever; minik gecikme faydalı olabilir.
        // (İsteğe bağlı) 1–2 sn debounce:
        long last = prefs.getLong("last_boot_receiver_ms", 0L);
        long now  = SystemClock.elapsedRealtime();
        if (now - last < 2000L) {
            LogUtils.logToFile(context, "BootReceiver", "Debounce: çok hızlı tekrar, atlandı");
           ;
            return;
        }
        prefs.edit().putLong("last_boot_receiver_ms", now).apply();

        try {
            Intent svc = new Intent(context, GPSService.class)
                    .putExtra("device_name", deviceName)
                    .putExtra("email_name",  emailName);
            context.startForegroundService(svc);
            LogUtils.logToFile(context, "BootReceiver", "GPSService boot/update ile başlatıldı");

        } catch (Exception e) {
            LogUtils.logToFile(context, "BootReceiver", "startForegroundService hata " + e);

        }
    }
}