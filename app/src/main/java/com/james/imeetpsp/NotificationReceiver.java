package com.james.imeetpsp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "iMeetPSP";
    private static final String CHANNEL_NAME = "iMeetPSP Channel";
    private static final String CHANNEL_DESCRIPTION = "Channel for iMeetPSP notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieve meeting details from intent extras
        String meetingTitle = intent.getStringExtra("meetingTitle");
        String notificationMessage = intent.getStringExtra("notificationMessage");
        String meetingId = intent.getStringExtra("meetingId");

        // Create an intent to open MeetingDetails activity
        Intent meetingDetailsIntent = new Intent(context, MeetingDetails.class);
        // Pass meeting ID to MeetingDetails activity
        meetingDetailsIntent.putExtra("meetingId", meetingId);
        // Create a PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, meetingDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the notification channel
        createNotificationChannel(context);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(meetingTitle)
                .setContentText(notificationMessage)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the content intent
                .setContentIntent(pendingIntent);

        // Get the notification manager
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        // Notify with unique ID
        notificationManagerCompat.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel(Context context) {
        // Check if the device is running on Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the notification channel
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);

            // Get the notification manager
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            // Create the channel
            notificationManager.createNotificationChannel(channel);
        }
    }
}