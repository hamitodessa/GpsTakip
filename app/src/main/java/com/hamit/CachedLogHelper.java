package com.hamit;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class CachedLogHelper {
    private static final String CACHE_FILE = "cached_gps.txt";

    public static void cacheLogLocally(Context context, String log) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(log + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendCachedLogs(Context context, String deviceName) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        if (!file.exists()) return;

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            boolean allSent = true;
            for (String line : lines) {
                boolean success = FtpUploadHelper.uploadToFTP(deviceName,line);
                if (!success) {
                    allSent = false;
                    break;
                }
            }
            if (allSent) file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
