package com.hamit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class AlarmKurucu {
    public static void alarmKur(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, GPSService.class);
        PendingIntent pendingIntent = PendingIntent.getForegroundService(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        long baslangic = System.currentTimeMillis() + 10_000; // 10 saniye sonra başlasın
        long aralik = 15 * 60 * 1000; // 15 dakika

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                baslangic,
                aralik,
                pendingIntent
        );
    }
}

