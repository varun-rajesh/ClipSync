package com.varunrajesh.clipsync;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.varunrajesh.clipsync.Constants.CHANNEL_ID;
import static com.varunrajesh.clipsync.Constants.CLIP_DOWNLOAD;
import static com.varunrajesh.clipsync.Constants.DIRECTION;

public class DownloadPopup extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(DIRECTION, CLIP_DOWNLOAD);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pull Clip")
                .setSmallIcon(R.drawable.ic_download)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(2, notification);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
