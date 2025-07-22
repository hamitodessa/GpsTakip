package com.hamit;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class GPSService extends Service {

    private static final String CHANNEL_ID = "gps_service_channel";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String cachedDeviceName;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("GPSService", "Service started");

        String deviceName = android.os.Build.MODEL;
        cachedDeviceName = deviceName.replaceAll("[^a-zA-Z0-9_-]", "_");
        Log.e("DeviceName", cachedDeviceName);

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Takip Aktif")
                .setContentText("Konum verisi gönderiliyor...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                handleNewLocation(location);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("GPSService", "Permission denied");
            stopSelf();
            return;
        }

        // GPS provider aktif mi?
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d("GPSService", "GPS provider aktif mi? → " + isGpsEnabled);

        // Son bilinen konumu çek ve işle
        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnown != null) {
            Log.d("GPSService", "Son bilinen konum: " + lastKnown.getLatitude() + "," + lastKnown.getLongitude());
            handleNewLocation(lastKnown);
        } else {
            Log.w("GPSService", "Son bilinen konum yok.");
        }

        // Konum dinlemeye başla
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30_000, 0, locationListener);
    }

    private void handleNewLocation(Location location) {
        Log.d("GPSService", "Konum alındı: " + location.getLatitude() + "," + location.getLongitude());
        String log = getFormattedLocation(location);
        Log.d("GPSService", log);

        new Thread(() -> {
            try {
                boolean success = FtpUploadHelper.uploadToFTP(cachedDeviceName, log);
                if (!success) {
                    CachedLogHelper.cacheLogLocally(getApplicationContext(), log);
                } else {
                    CachedLogHelper.sendCachedLogs(getApplicationContext(), cachedDeviceName);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        Log.d("GPSService", "Service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
