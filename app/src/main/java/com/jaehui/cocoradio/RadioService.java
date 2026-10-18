
package com.jaehui.cocoradio;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class RadioService extends Service {
    private static final String CHANNEL_ID = "coco_radio_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        Notification notif = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("재희미용실 라디오")
                .setContentText("코코가 방송을 이어가고 있어요 🎧")
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setOngoing(true)
                .build();
        startForeground(1, notif);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Coco Radio", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("네비에서 지도 켜도 소리 계속");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
