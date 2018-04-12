package com.kingpuller.gocompanion.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.graphics.drawable.PathInterpolatorCompat;
import android.support.v4.internal.view.SupportMenu;

import com.kingpuller.gocompanion.MainActivity;
import com.kingpuller.gocompanion.R;

public class NotificationHelper {

    public static Notification buildNotification(Context context, String channelId, String channelDescription, NotificationManager notificationManager) {
        Notification.Builder mBuilder;
        //Check if notification channel exists and if not create one
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            assert notificationManager != null;
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelId);
            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                notificationChannel = new NotificationChannel(channelId, channelDescription, importance);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            mBuilder = new Notification.Builder(context, channelId);
        } else {
            mBuilder = new Notification.Builder(context);

        }

        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
        mBuilder.setSmallIcon(R.drawable.ic_pokedex)
                .setContentTitle(context.getString(R.string.notification_service_title))
                .setContentText(context.getString(R.string.notification_service_content))
                .setOngoing(true)
                .setLights(SupportMenu.CATEGORY_MASK, PathInterpolatorCompat.MAX_NUM_POINTS, PathInterpolatorCompat.MAX_NUM_POINTS)
                .setContentIntent(pendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder.setChannelId(channelId);
        }
        return mBuilder.build();
    }

    public static Notification showDisconnectNotification(Context context, String channelId, String channelDescription, NotificationManager notificationManager) {
        Notification.Builder mBuilder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            assert notificationManager != null;
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelId);
            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                notificationChannel = new NotificationChannel(channelId, channelDescription, importance);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            mBuilder = new Notification.Builder(context, channelId);
        } else {
            mBuilder = new Notification.Builder(context);
        }

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(new ComponentName(Constants.POKEMON_PACKAGE_NAME, Constants.POKEMON_CLASS_NAME));
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mBuilder.setSmallIcon(R.drawable.ic_warning_black_24dp)
                .setContentTitle(context.getString(R.string.connection_lost))
                .setAutoCancel(true)
                .setContentText(context.getString(R.string.tap_to_open))
                .setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("notifications_new_message_ringtone", "")))
                .setLights(SupportMenu.CATEGORY_MASK, PathInterpolatorCompat.MAX_NUM_POINTS, PathInterpolatorCompat.MAX_NUM_POINTS)
                .setContentIntent(PendingIntent.getActivity(context, 0, i, 0));
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notifications_new_message_vibrate", true)) {
            int length = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("notifications_new_message_vibrate_length", "200"));
            //int count = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("notifications_new_message_vibrate_count", "2"));
            mBuilder.setVibrate(new long[]{0, length, 100, length});
        }
        return mBuilder.build();
    }

    public static Notification updateNotification(Context context, int batteryLevel) {
        Notification.Builder mBuilder;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            assert notificationManager != null;
            String channelId = "default_channel_id";
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelId);
            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                String channelDescription = "Service Channel";
                notificationChannel = new NotificationChannel(channelId, channelDescription, importance);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            mBuilder = new Notification.Builder(context, channelId);
        } else {
            mBuilder = new Notification.Builder(context);
        }

        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mBuilder.setSmallIcon(R.drawable.ic_pokedex)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_battery_level, batteryLevel))
                .setOngoing(true)
                .setLights(SupportMenu.CATEGORY_MASK, PathInterpolatorCompat.MAX_NUM_POINTS, PathInterpolatorCompat.MAX_NUM_POINTS)
                .setContentIntent(PendingIntent.getActivity(context, 0, i, 0));

        return mBuilder.build();
    }
}
