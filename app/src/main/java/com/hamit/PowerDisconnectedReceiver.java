package com.hamit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class PowerDisconnectedReceiver extends BroadcastReceiver {

    private static final long DEBOUNCE_MS = 2000; // bazı üniteler 1-2 kez üst üste yayın gönderebiliyor

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
        long now  = SystemClock.elapsedRealtime();
        long last = prefs.getLong("last_power_disconnected_ms", 0L);
        if (now - last < DEBOUNCE_MS) {
            LogUtils.logToFile(context, "PowerDisconnectedReceiver", "Debounce: tekrar tetikleme atlandı");
            return;
        }
        prefs.edit().putLong("last_power_disconnected_ms", now).apply();
        LogUtils.logToFile(context, "PowerDisconnectedReceiver", "POWER_DISCONNECTED yakalandı, GPSService durduruluyor");
        try {
            context.stopService(new Intent(context, GPSService.class));
            LogUtils.logToFile(context, "PowerDisconnectedReceiver", "GPSService stopService() çağrıldı");
        } catch (Exception e) {
            LogUtils.logToFile(context, "PowerDisconnectedReceiver", "stopService hata: " + e);
        }
    }
}