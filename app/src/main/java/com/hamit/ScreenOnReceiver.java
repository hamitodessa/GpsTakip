package com.hamit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ScreenOnReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            Log.d("ScreenOnReceiver", "Ekran açıldı, servis kontrol ediliyor");
            SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
            boolean runOnStartup = prefs.getBoolean("runOnStartup", false);
            if (runOnStartup) {
                String deviceName = prefs.getString("device_name", "");
                String emailName = prefs.getString("email_name", "");

                if (!deviceName.isEmpty() && !emailName.isEmpty()) {
                    Intent serviceIntent = new Intent(context, GPSService.class);
                    serviceIntent.putExtra("device_name", deviceName);
                    serviceIntent.putExtra("email_name", emailName);
                    context.startForegroundService(serviceIntent);
                    Log.d("ScreenOnReceiver", "Servis USER_PRESENT ile başlatıldı");
                } else {
                    Log.e("ScreenOnReceiver", "Cihaz adı veya e-posta eksik");
                }
            }
        }
    }
}