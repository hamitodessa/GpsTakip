package com.hamit;

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
            boolean runOnStartup = prefs.getBoolean("runOnStartup", false); // 🔍 yeni kontrol
            if (!runOnStartup) {
                Log.d("BootReceiver", "runOnStartup devre dışı, servis başlatılmıyor");
                return;
            }
            String deviceName = prefs.getString("device_name", "");
            String emailName = prefs.getString("email_name", "");
            if (!deviceName.isEmpty() && !emailName.isEmpty()) {
                Intent serviceIntent = new Intent(context, GPSService.class);
                serviceIntent.putExtra("device_name", deviceName);
                serviceIntent.putExtra("email_name", emailName);
                context.startForegroundService(serviceIntent);
                Log.d("BootReceiver", "Servis otomatik başlatıldı");
            } else {
                Log.e("BootReceiver", "Cihaz adı veya e-posta eksik, servis başlatılmadı");
            }
        }
    }
}
