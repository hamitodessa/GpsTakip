package com.hamit;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GPSService extends Service {

    private static final String CHANNEL_ID = "gps_service_channel";
    private static boolean isRunning = false;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private String fileName;
    private static final List<String> logBuffer = new CopyOnWriteArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        if (isRunning) {
            LogUtils.logToFile(this, "GPSService", "Zaten çalışıyor, tekrar başlatılmayacak");

            stopSelf();
            return;
        }
        isRunning = true;

        Toast.makeText(this, "GPS Servis Başladı!", Toast.LENGTH_LONG).show();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Takip Aktif")
                .setContentText("Konum verisi toplanıyor...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GPSService", "onStartCommand çağrıldı");
        SharedPreferences prefs = getSharedPreferences("GPSPrefs", MODE_PRIVATE);
        String cachedDeviceName = intent.getStringExtra("device_name");
        if (cachedDeviceName == null || cachedDeviceName.trim().isEmpty()) {
            cachedDeviceName = prefs.getString("device_name", "");
        }
        String cachedEmail = intent.getStringExtra("email_name");
        if (cachedEmail == null || cachedEmail.trim().isEmpty()) {
            cachedEmail = prefs.getString("email_name", "");
        }
        if (cachedDeviceName == null || cachedDeviceName.trim().isEmpty() ||
                cachedEmail == null || cachedEmail.trim().isEmpty()) {
            Log.e("GPSService", "Cihaz adı veya email boş, servis durduruluyor");
            stopSelf();
            return START_NOT_STICKY;
        }
        cachedDeviceName = cachedDeviceName.replaceAll("[^a-zA-Z0-9_-]", "_");
        cachedEmail = cachedEmail.replaceAll("[^a-zA-Z0-9@._-]", "_");
        fileName = cachedEmail + "_" + cachedDeviceName + "_log.txt";

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                handleNewLocation(location);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LogUtils.logToFile(this, "GPSService", "İzin yok → servis kapatılıyor");

            stopSelf();
            return START_NOT_STICKY;
        }

        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnown != null) {
            handleNewLocation(lastKnown);
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15_000, 0, locationListener);
        return START_STICKY;
    }

    private void handleNewLocation(Location location) {
        String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String log = datetime + "," + location.getLatitude() + "," + location.getLongitude();
        logBuffer.add(log);

        new Thread(() -> {
            try {

                List<String> sortedLogs = new java.util.ArrayList<>(logBuffer);
                Collections.sort(sortedLogs);
                logBuffer.clear();
                for (String entry : sortedLogs) {
                    boolean success = FtpUploadHelper.uploadToFTP(fileName, entry);
                    if (!success) {
                        CachedLogHelper.cacheLogLocally(getApplicationContext(), entry);
                    }
                }
                // Cache'leri de gönder
                CachedLogHelper.sendCachedLogs(getApplicationContext(), fileName);
            } catch (Exception e) {
                LogUtils.logToFile(this, "GPSService","Log gönderimi hatası: " + e.getMessage());

            }
        }).start();
    }

    private void createNotificationChannel() {
        CharSequence name = "GPS Servis Kanalı";
        String description = "Konum servisi bildirimi";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    @Override
     public void onDestroy() {
        super.onDestroy();

        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        isRunning = false;

        LogUtils.logToFile(this, "GPSService", "Servis durduruldu");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
