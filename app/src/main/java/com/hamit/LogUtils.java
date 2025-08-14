package com.hamit;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;

public class LogUtils {

    public static void logToFile(Context context, String tag, String message) {
        // Normal logcat'e yaz
        android.util.Log.d(tag, message);

        try {
            File logFile = new File(context.getExternalFilesDir(null), "gps_log.txt");
            try (FileWriter fw = new FileWriter(logFile, true)) {
                String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault()).format(new java.util.Date());
                fw.write(time + " [" + tag + "] " + message + "\n");
            }
        } catch (Exception e) {
            android.util.Log.e("LogUtils", "Dosyaya log yazılamadı", e);
        }
    }
}
