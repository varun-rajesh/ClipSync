package com.varunrajesh.clipsync;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.varunrajesh.clipsync.Constants.CHANNEL_ID;
import static com.varunrajesh.clipsync.Constants.CLIP_UPLOAD;
import static com.varunrajesh.clipsync.Constants.DIRECTION;


public class UploadPopup extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(DIRECTION, CLIP_UPLOAD);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                1, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Push Clip")
                .setSmallIcon(R.drawable.ic_upload)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

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
