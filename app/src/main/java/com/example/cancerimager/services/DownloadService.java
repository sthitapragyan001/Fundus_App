package com.example.cancerimager.services;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.cancerimager.R;
import com.example.cancerimager.model.DownloadRequestBody;
import com.example.cancerimager.network.ApiService;
import com.example.cancerimager.network.RetrofitClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadService extends IntentService {

    private static final String TAG = "DownloadService";
    private static final String CHANNEL_ID = "download_channel";

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;

        List<String> photoUrls = intent.getStringArrayListExtra("photo_urls");
        if (photoUrls == null || photoUrls.isEmpty()) return;

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        DownloadRequestBody body = new DownloadRequestBody(photoUrls);
        Call<ResponseBody> call = apiService.downloadPhotos(body);

        try {
            Response<ResponseBody> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                saveZipFile(response.body());
            } else {
                Log.e(TAG, "Server error while downloading zip: " + response.message());
                sendDownloadFailedNotification();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error executing download call", e);
            sendDownloadFailedNotification();
        }
    }

    private void saveZipFile(ResponseBody body) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "cancerimager_" + timeStamp + ".zip";

        ContentResolver resolver = getApplicationContext().getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        }

        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

        if (uri == null) {
            Log.e(TAG, "Failed to create new MediaStore record.");
            sendDownloadFailedNotification();
            return;
        }

        try (InputStream inputStream = body.byteStream();
             OutputStream outputStream = resolver.openOutputStream(uri)) {

            if (outputStream == null) {
                Log.e(TAG, "Failed to open output stream for " + uri);
                sendDownloadFailedNotification();
                return;
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.d(TAG, "Successfully downloaded: " + fileName);
            sendDownloadCompleteNotification(fileName);
        } catch (IOException e) {
            // If there's an error, delete the incomplete file entry.
            resolver.delete(uri, null, null);
            sendDownloadFailedNotification();
            throw e;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void sendDownloadCompleteNotification(String fileName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper download icon if you have one
                .setContentTitle("Download Complete")
                .setContentText(fileName + " has been downloaded.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void sendDownloadFailedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper error icon if you have one
                .setContentTitle("Download Failed")
                .setContentText("Could not download the zip file.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(2, builder.build());
    }
}
