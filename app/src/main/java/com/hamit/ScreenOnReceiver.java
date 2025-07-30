package com.hamit;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ScreenOnReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
            Log.d("ScreenOnReceiver", "Ekran açıldı, servis kontrol ediliyor");
            SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
            boolean runOnStartup = prefs.getBoolean("runOnStartup", false);
            if (runOnStartup) {
                String deviceName = prefs.getString("device_name", "");
                String emailName = prefs.getString("email_name", "");

                if (!deviceName.isEmpty() && !emailName.isEmpty()) {
                    if (!isServiceRunning(context, GPSService.class)) {
                        Intent serviceIntent = new Intent(context, GPSService.class);
                        serviceIntent.putExtra("device_name", deviceName);
                        serviceIntent.putExtra("email_name", emailName);
                        context.startForegroundService(serviceIntent);
                        Log.d("ScreenOnReceiver", "Servis USER_PRESENT ile başlatıldı");
                    } else {
                        Log.d("ScreenOnReceiver", "Servis zaten çalışıyor, tekrar başlatılmadı");
                    }
                } else {
                    Log.e("ScreenOnReceiver", "Cihaz adı veya e-posta eksik");
                }
            }
        }
    }

    private boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
