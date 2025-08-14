package com.hamit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class PowerDisconnectedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) return;

        Log.d("PowerDisconnectedReceiver", "POWER_DISCONNECTED yakalandı, servis durduruluyor");

        // Servisi nazikçe durdur
        try {
            context.stopService(new Intent(context, GPSService.class));
        } catch (Exception e) {
            Log.e("PowerDisconnectedReceiver", "stopService hata", e);
        }

        // (İsteğe bağlı) bir işaret bırak: güç kesildi
        try {
            SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
            prefs.edit().putLong("last_power_disconnected_ms", System.currentTimeMillis()).apply();
        } catch (Exception ignore) {}
    }
}