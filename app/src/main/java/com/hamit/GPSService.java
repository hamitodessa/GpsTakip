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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GPSService extends Service {

    private static final String CHANNEL_ID = "gps_service_channel";
    private LocationManager locationManager;
    private LocationListener locationListener;

    private String fileName;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("GPSService", "onCreate çağrıldı");

        Log.d("GPSService", "onCreate çalıştı!!!");
        Toast.makeText(this, "GPS Servis Başladı!", Toast.LENGTH_LONG).show(); // GEÇİCİ TEST İÇİN

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

        if (cachedDeviceName.toLowerCase().endsWith(".txt")) {
            cachedDeviceName = cachedDeviceName.substring(0, cachedDeviceName.length() - 4);
        }
        cachedDeviceName = cachedDeviceName.replaceAll("[^a-zA-Z0-9_-]", "_");
        cachedEmail = cachedEmail.replaceAll("[^a-zA-Z0-9@._-]", "_");
        if (cachedDeviceName.toLowerCase().endsWith(".txt")) {
            cachedDeviceName = cachedDeviceName.substring(0, cachedDeviceName.length() - 4);
        }
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
            Log.e("GPSService", "İzin yok → servis kapatılıyor");
            stopSelf();
            return START_NOT_STICKY;
        }

        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d("GPSService", "GPS provider aktif mi? → " + isGpsEnabled);
        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnown != null) {
            Log.d("GPSService", "Son bilinen konum: " + lastKnown.getLatitude() + "," + lastKnown.getLongitude());
            handleNewLocation(lastKnown);
        } else {
            Log.w("GPSService", "Son bilinen konum yok.");
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000, 0, locationListener);
        return START_STICKY;
    }

    private void handleNewLocation(Location location) {
        Log.d("GPSService", "Konum alındı: " + location.getLatitude() + "," + location.getLongitude());
        String log = getFormattedLocation(location);

        new Thread(() -> {
            try {
                boolean success = FtpUploadHelper.uploadToFTP(fileName, log);
                if (!success) {
                    CachedLogHelper.cacheLogLocally(getApplicationContext(), log);
                } else {
                    CachedLogHelper.sendCachedLogs(getApplicationContext(), fileName);
                }
            } catch (Exception e) {
                Log.e("GPSService", "FTP upload hatası: " + e.getMessage(), e);
            }
        }).start();
    }
    private String getFormattedLocation(Location location) {
        String datetime = java.time.LocalDateTime.now().toString().replace("T", " ");
        return datetime + "," + location.getLatitude() + "," + location.getLongitude();
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
        Log.d("GPSService", "Servis durduruldu");
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
