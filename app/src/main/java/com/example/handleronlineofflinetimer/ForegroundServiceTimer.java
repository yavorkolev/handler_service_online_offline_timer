package com.example.handleronlineofflinetimer;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class ForegroundServiceTimer extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final long DELAYED_TIME = 1000;
    final static String MY_ACTION = "MY_ACTION";
    private int seconds = 0;
    public boolean running;
    public static TimerThread timerThread;
    @Override
    public void onCreate() {
        super.onCreate();
        if(timerThread == null) timerThread = new TimerThread();
        running = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerThread.stop();
        timerThread = null;
        running = false;
        seconds = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    class TimerThread implements Runnable {
        private boolean isThreadStopped;
        Thread t;

        TimerThread()
        {
            t = new Thread(this);
            isThreadStopped = false;
            t.start();
        }

        @SuppressLint("ForegroundServiceType")//TODO fix ForegroundServiceTimer changes in android 14/sdk34
        public void run()
        {
            createNotificationChannel();
            Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, FLAG_UPDATE_CURRENT);
            final NotificationCompat.Builder notification = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                    .setContentTitle("Foreground Service")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                    .setVibrate(null)
                    .setContentIntent(pendingIntent);
            while (!isThreadStopped) {
                try {
                    Thread.sleep(DELAYED_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                notification.setContentText("Time: " + time);
                Intent intent = new Intent();
                intent.setAction(MY_ACTION);
                intent.putExtra("DATA_PASSED", time);
                sendBroadcast(intent);
                seconds++;
                startForeground(1, notification.build());
            }
        }

        public void stop()
        {
            isThreadStopped = true;
        }
        public void start()
        {
            isThreadStopped = false;
            timerThread.run();
        }
    }

}
