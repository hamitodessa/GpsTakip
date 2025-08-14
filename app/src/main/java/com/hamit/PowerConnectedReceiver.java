package com.hamit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class PowerConnectedReceiver extends BroadcastReceiver {
    private static final long DEBOUNCE_MS = 3000;

    @Override public void onReceive(Context context, Intent intent) {
        final String action = intent != null ? intent.getAction() : null;
        if (!Intent.ACTION_POWER_CONNECTED.equals(action)) return;

        Log.d("PowerConnectedReceiver", "POWER_CONNECTED yakalandı");

        SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
        boolean runOnStartup = prefs.getBoolean("runOnStartup", false);
        if (!runOnStartup) {
            Log.d("PowerConnectedReceiver", "runOnStartup=false, servis başlatılmıyor");
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long last = prefs.getLong("last_power_connected_ms", 0L);
        if (now - last < DEBOUNCE_MS) {
            Log.d("PowerConnectedReceiver", "Debounce: tekrarlı tetikleme atlandı");
            return;
        }
        prefs.edit().putLong("last_power_connected_ms", now).apply();

        String deviceName = prefs.getString("device_name", "");
        String emailName  = prefs.getString("email_name", "");
        if (deviceName.isEmpty() || emailName.isEmpty()) {
            Log.e("PowerConnectedReceiver", "device_name veya email_name eksik");
            return;
        }

        try {
            Intent svc = new Intent(context, GPSService.class)
                    .putExtra("device_name", deviceName)
                    .putExtra("email_name",  emailName);
            context.startForegroundService(svc);
            Log.d("PowerConnectedReceiver", "Servis POWER_CONNECTED ile başlatıldı");
        } catch (Exception e) {
            Log.e("PowerConnectedReceiver", "startForegroundService hata", e);
            // Watchdog'a güvenebilmek için işaret bırak
            prefs.edit().putBoolean("need_service_start", true).apply();
        }
    }
}
