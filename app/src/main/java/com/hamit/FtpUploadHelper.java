package com.hamit;

import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FtpUploadHelper {

    public static boolean uploadToFTP(String deviceName, String content) {
        FTPClient ftpClient = new FTPClient();
        ByteArrayInputStream inputStream = null;
        try {
            Log.d("FTP", "Bağlantı kuruluyor...");
            ftpClient.connect("78.189.76.247", 21);
            boolean login = ftpClient.login("hamitadmin", "SDFks9hfji3#DEd");
            if (!login) {
                Log.e("FTP", "Login başarısız!");
                return false;
            }
            Log.d("FTP", "Bağlantı kuruluyor...");
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            String remoteFilePath = "GPS/" + deviceName + "_log.txt";

            inputStream = new ByteArrayInputStream((content + "\n").getBytes());
            boolean success = ftpClient.appendFile(remoteFilePath, inputStream);
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
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("FTP", "inputStream kapatılırken hata", e);
                }
            }
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex2) {
                Log.e("FTP", "Bağlantı kapatma hatası: " + ex2.getMessage(), ex2);
            }
        }
    }
}