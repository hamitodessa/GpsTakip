package com.hamit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
            String deviceName = prefs.getString("device_name", "");
            if (!deviceName.isEmpty()) {
                Intent serviceIntent = new Intent(context, GPSService.class);
                serviceIntent.putExtra("device_name", deviceName);
                context.startForegroundService(serviceIntent);
            } else {
                Log.e("BootReceiver", "Cihaz adı boş, servis başlatılmadı");
            }
        }
    }
}