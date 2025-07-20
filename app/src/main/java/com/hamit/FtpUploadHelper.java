package com.hamit;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FtpUploadHelper {

    public static boolean uploadToFTP(String deviceName, String content) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect("78.189.76.247", 21);
            boolean login = ftpClient.login("hamitadmin", "SDFks9hfji3#DEd");
            if (!login) {
                Log.e("FTP", "Login başarısız!");
                return false;
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            String remoteFilePath = "GPS/" + deviceName + "_log.txt";

            ByteArrayInputStream inputStream = new ByteArrayInputStream((content + "\n").getBytes());
            boolean success = ftpClient.appendFile(remoteFilePath, inputStream);
            inputStream.close();
            if (success) {
                Log.d("FTP", "Veri gönderildi: " + content);
            } else {
                Log.e("FTP", "Gönderim başarısız.");
            }
            ftpClient.logout();
            ftpClient.disconnect();
            return success;
        } catch (IOException ex) {
            Log.e("FTP", "Hata: " + ex.getMessage(), ex);
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex2) {
                Log.e("FTP", "Bağlantı kapatma hatası: " + ex2.getMessage(), ex2);
            }
            return false;
        }
    }
}
