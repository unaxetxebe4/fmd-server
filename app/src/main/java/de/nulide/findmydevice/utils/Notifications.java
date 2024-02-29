package de.nulide.findmydevice.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.ui.MainActivity;

public class Notifications {

    public static final int CHANNEL_USAGE = 42;
    public static final int CHANNEL_LIFE = 43;
    public static final int CHANNEL_PIN = 44;
    public static final int CHANNEL_SERVER = 45;

    public static final int CHANNEL_SECURITY = 46;

    public static final int CHANNEL_FAILED = 47;

    private static boolean silent;


    public static void notify(Context context, String title, String text, int channelID) {
        if (!silent) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, new Integer(channelID).toString())
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            if (channelID == CHANNEL_SECURITY) {
                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                builder.setAutoCancel(true);
                builder.setContentIntent(pendingIntent);
            }
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(channelID, builder.build());
        }
    }

    public static void init(Context context, boolean silentWish) {
        silent = silentWish;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(new Integer(CHANNEL_USAGE).toString(), context.getString(R.string.Notification_Usage), NotificationManager.IMPORTANCE_DEFAULT);
            channel1.setDescription(context.getString(R.string.Notification_Usage_Description));
            NotificationChannel channel2 = new NotificationChannel(new Integer(CHANNEL_LIFE).toString(), context.getString(R.string.Notification_Lifecycle), NotificationManager.IMPORTANCE_DEFAULT);
            channel2.setDescription(context.getString(R.string.Notification_Lifecycle_Description));
            NotificationChannel channel3 = new NotificationChannel(new Integer(CHANNEL_PIN).toString(), context.getString(R.string.Pin_Usage), NotificationManager.IMPORTANCE_DEFAULT);
            channel3.setDescription(context.getString(R.string.Notification_Pin_Usage_Description));
            NotificationChannel channel4 = new NotificationChannel(new Integer(CHANNEL_SERVER).toString(), context.getString(R.string.Notification_Server), NotificationManager.IMPORTANCE_DEFAULT);
            channel4.setDescription(context.getString(R.string.NotificationServer_Description));
            NotificationChannel channel5 = new NotificationChannel(new Integer(CHANNEL_SECURITY).toString(), context.getString(R.string.Notification_Security), NotificationManager.IMPORTANCE_DEFAULT);
            channel5.setDescription(context.getString(R.string.Notification_Security_Description));
            NotificationChannel channel6 = new NotificationChannel(new Integer(CHANNEL_FAILED).toString(), context.getString(R.string.Notification_FAIL), NotificationManager.IMPORTANCE_HIGH);
            channel6.setDescription(context.getString(R.string.Notification_Fail_Description));

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel1);
            notificationManager.createNotificationChannel(channel2);
            notificationManager.createNotificationChannel(channel3);
            notificationManager.createNotificationChannel(channel4);
            notificationManager.createNotificationChannel(channel5);
            notificationManager.createNotificationChannel(channel6);
        }
    }

}
