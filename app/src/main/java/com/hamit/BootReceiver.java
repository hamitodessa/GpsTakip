package com.hamit;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver", "BOOT_COMPLETED alındı!");
        Toast.makeText(context, "BootReceiver tetiklendi!", Toast.LENGTH_LONG).show();

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("GPSPrefs", Context.MODE_PRIVATE);
            boolean runOnStartup = prefs.getBoolean("runOnStartup", false);

            if (!runOnStartup) {
                Log.d("BootReceiver", "runOnStartup devre dışı, servis başlatılmıyor");
                return;
            }

            String deviceName = prefs.getString("device_name", "");
            String emailName = prefs.getString("email_name", "");

            if (!deviceName.isEmpty() && !emailName.isEmpty()) {
                if (!isServiceRunning(context, GPSService.class)) {
                    Intent serviceIntent = new Intent(context, GPSService.class);
                    serviceIntent.putExtra("device_name", deviceName);
                    serviceIntent.putExtra("email_name", emailName);
                    context.startForegroundService(serviceIntent);
                    Log.d("BootReceiver", "Servis otomatik başlatıldı");
                } else {
                    Log.d("BootReceiver", "Servis zaten çalışıyor, başlatılmadı");
                }
            } else {
                Log.e("BootReceiver", "Cihaz adı veya e-posta eksik, servis başlatılmadı");
            }
        }

        // İsteğe bağlı: alarm kurucu hâlâ dursun mu?
        // AlarmKurucu.alarmKur(context);
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
