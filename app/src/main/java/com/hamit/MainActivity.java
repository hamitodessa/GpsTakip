package com.hamit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView permissionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("GPSService", "Program Başladı");

        permissionStatus = findViewById(R.id.permissionStatus); // TextView

        Button btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, GPSService.class));
            finishAffinity();
        });

        Button btnMinimize = findViewById(R.id.btnMinimize);
        btnMinimize.setOnClickListener(v -> moveTaskToBack(true));

        updatePermissionStatus();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasFineLocationPermission()) {
                if (hasBackgroundLocationPermission()) {
                    if (Build.VERSION.SDK_INT >= 34 && !hasForegroundLocationPermission()) {
                        requestForegroundLocationPermission();
                        return;
                    }
                    startService(new Intent(this, GPSService.class));
                    Log.d("GPSService", "Servis Başlatıldı");
                } else {
                    showPermissionSettingsDialog();
                }
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            if (hasFineLocationPermission()) {
                startService(new Intent(this, GPSService.class));
                Log.d("GPSService", "Servis Başlatıldı (API <29)");
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean hasFineLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBackgroundLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasForegroundLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestForegroundLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.FOREGROUND_SERVICE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updatePermissionStatus();
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (hasFineLocationPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (!hasBackgroundLocationPermission()) {
                        showPermissionSettingsDialog();
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 34 && !hasForegroundLocationPermission()) {
                        requestForegroundLocationPermission();
                        return;
                    }
                }
                startService(new Intent(this, GPSService.class));
                Log.d("GPSService", "İzinler verildi → Servis Başlatıldı");
            } else {
                Log.e("GPSService", "Fine Location reddedildi");
            }
        }
    }

    private void updatePermissionStatus() {
        StringBuilder sb = new StringBuilder("İzin Durumu:\n");

        if (hasFineLocationPermission()) sb.append("✓ Fine Location\n");
        else sb.append("✗ Fine Location\n");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasBackgroundLocationPermission()) sb.append("✓ Background Location\n");
            else sb.append("✗ Background Location\n");
        }

        if (Build.VERSION.SDK_INT >= 34) {
            if (hasForegroundLocationPermission()) sb.append("✓ Foreground Service Location\n");
            else sb.append("✗ Foreground Service Location\n");
        }

        if (permissionStatus != null) {
            permissionStatus.setText(sb.toString());
        }
    }

    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Gerekli İzinler ve Ayarlar")
                .setMessage("Bu uygulamanın arka planda doğru şekilde çalışabilmesi için aşağıdaki ayarları yapmanız gerekmektedir:\n\n"
                        + "1. 'Ayarları Aç' butonuna tıklayın\n"
                        + "2. Açılan ekranda 'İzinler' (Permissions) bölümüne girin\n"
                        + "3. 'Konum' (Location) seçeneğine dokunun\n"
                        + "4. 'Her zaman izin ver' (Allow all the time) seçeneğini seçin\n\n"
                        + "5. Geri gelin ve 'Pil' (Battery) > 'Pil optimizasyonu' veya 'Arka plan etkinliği' bölümüne girin\n"
                        + "6. Uygulama için 'Sınırsız' (Unrestricted) seçeneğini seçin\n\n"
                        + "Bu ayarlar yapılmazsa GPS servisi arka planda durdurulabilir.")
                .setPositiveButton("Ayarları Aç", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("İptal", null)
                .show();
    }
}
