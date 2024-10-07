package com.example.finandetails;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class YourService extends Service {

    private static final String CHANNEL_ID = "YourServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We are not binding to this service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle the service's task here, e.g., reading SMS
        readSms(); // Call your SMS reading method
        return START_STICKY; // Ensure the service is restarted if killed
    }

    private void readSms() {
        // Implement your SMS reading logic here, similar to your MainActivity's readSms method
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Reading Service")
                .setContentText("Reading SMS messages...")
                .setSmallIcon(R.mipmap.ic_launcher) // Your app's icon
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup tasks if necessary
    }
}
