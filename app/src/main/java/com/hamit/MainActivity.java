package com.hamit;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView permissionStatus;
    private EditText deviceNameInput;
    private static final String PREFS_NAME = "GPSPrefs";
    private static final String KEY_DEVICE_NAME = "device_name";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("GPSService", "Program Başladı");

        permissionStatus = findViewById(R.id.permissionStatus);
        deviceNameInput = findViewById(R.id.deviceNameInput);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Kaydedilmiş cihaz adı yükleniyor
        String savedDeviceName = prefs.getString(KEY_DEVICE_NAME, "");
        deviceNameInput.setText(savedDeviceName);

        Button btnSaveDeviceName = findViewById(R.id.btnSaveDeviceName);
        btnSaveDeviceName.setOnClickListener(v -> {
            String name = deviceNameInput.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Lütfen cihaz adı girin!", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString(KEY_DEVICE_NAME, name).apply();
                Toast.makeText(this, "Cihaz adı kaydedildi", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, GPSService.class));
            finishAffinity();
        });

        Button btnMinimize = findViewById(R.id.btnMinimize);
        btnMinimize.setOnClickListener(v -> moveTaskToBack(true));

        updatePermissionStatus();
        checkAndStartService();
    }

    private void checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasFineLocationPermission()) {
                if (hasBackgroundLocationPermission()) {
                    if (Build.VERSION.SDK_INT >= 34 && !hasForegroundLocationPermission()) {
                        requestForegroundLocationPermission();
                        return;
                    }
                    startGPSServiceIfValid();
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
                startGPSServiceIfValid();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void startGPSServiceIfValid() {
        String deviceName = deviceNameInput.getText().toString().trim();
        if (deviceName.isEmpty()) {
            permissionStatus.setText("⚠ Lütfen cihaz adı girin!");
            return;
        }

        // Cihaz adını SharedPreferences'e kaydet
        prefs.edit().putString(KEY_DEVICE_NAME, deviceName).apply();

        Intent serviceIntent = new Intent(this, GPSService.class);
        serviceIntent.putExtra("device_name", deviceName);
        startService(serviceIntent);
        Log.d("GPSService", "Servis Başlatıldı");
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
                startGPSServiceIfValid();
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
                        + "2. Açılan ekranda 'İzinler' bölümüne girin\n"
                        + "3. 'Konum' seçeneğine dokunun\n"
                        + "4. 'Her zaman izin ver' seçeneğini seçin\n\n"
                        + "5. Pil > Pil optimizasyonu > Arka plan etkinliği → 'Sınırsız' seçeneğini seçin")
                .setPositiveButton("Ayarları Aç", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("İptal", null)
                .show();
    }
}
