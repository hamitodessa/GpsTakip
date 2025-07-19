package com.hamit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("GPSService", "Program Başladı");

        // Minimize butonu
        Button btnMinimize = findViewById(R.id.btnMinimize);
        btnMinimize.setOnClickListener(v -> {
            Log.d("GPSService", "Kullanıcı minimize etti");
            moveTaskToBack(true);
        });

        // Konum ve foreground servis izinlerini kontrol et
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                startService(new Intent(this, GPSService.class));
                Log.d("GPSService", "Servis Başlatıldı");
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.FOREGROUND_SERVICE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE);
                Log.d("GPSService", "İzinler isteniyor...");
            }
        } else {
            // Android 9 ve altı
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startService(new Intent(this, GPSService.class));
                Log.d("GPSService", "Servis Başlatıldı");
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                Log.d("GPSService", "İzin isteniyor...");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            boolean foregroundGranted = true; // eski Android sürümleri için varsayılan olarak true

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    locationGranted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        permissions[i].equals(Manifest.permission.FOREGROUND_SERVICE_LOCATION)) {
                    foregroundGranted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                }
            }

            if (locationGranted && foregroundGranted) {
                startService(new Intent(this, GPSService.class));
                Log.d("GPSService", "İzinler verildi → Servis başlatıldı");
            } else {
                Log.e("GPSService", "Gerekli izinler reddedildi");
            }
        }
    }
}