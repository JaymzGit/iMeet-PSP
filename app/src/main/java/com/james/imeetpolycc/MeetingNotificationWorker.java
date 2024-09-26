package com.james.imeetpolycc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MeetingNotificationWorker extends Worker {

    public static final String MEETING_ID_KEY = "meeting_id";
    public static final String MEETING_TITLE_KEY = "meeting_title";
    public static final String MEETING_DATE_KEY = "meeting_date";
    public static final String MEETING_TIME_KEY = "meeting_time";
    private static final String CHANNEL_ID = "meeting_notifications";
    private static final String SHARED_PREFS_NAME = "notification_prefs";

    private FirebaseFirestore fStore;
    private SharedPreferences sharedPreferences;

    public MeetingNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        fStore = FirebaseFirestore.getInstance();
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void cancelNotification(Context context, String meetingId) {
        // Cancel notifications using NotificationManager
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(meetingId.hashCode());  // Use the same ID as when scheduling the notification
        }

        // Cancel WorkManager jobs using WorkManager
        WorkManager.getInstance(context).cancelAllWorkByTag(meetingId);
    }

    @NonNull
    @Override
    public Result doWork() {
        String meetingId = getInputData().getString(MEETING_ID_KEY);
        String meetingTitle = getInputData().getString(MEETING_TITLE_KEY);
        String meetingDate = getInputData().getString(MEETING_DATE_KEY);
        String meetingTime = getInputData().getString(MEETING_TIME_KEY);

        if (meetingId == null || meetingTitle == null || meetingDate == null || meetingTime == null) {
            Log.e("MeetingNotificationWorker", "Missing input data");
            return Result.failure();
        }

        // Log the meeting info for debugging
        Log.d("MeetingNotificationWorker", "Checking meeting: " + meetingTitle + " at " + meetingDate + " " + meetingTime);

        // Check if the meeting time is now or has passed
        if (isMeetingOngoing(meetingDate, meetingTime)) {
            Log.d("MeetingNotificationWorker", "Meeting is ongoing or past. Updating status...");
            updateMeetingStatus(meetingId, "Ongoing");  // Update Firestore to mark the meeting as ongoing
        } else {
            sendNotification(meetingId, meetingTitle, meetingDate, meetingTime);
        }

        return Result.success();
    }

    private void sendNotification(String meetingId, String title, String date, String time) {
        Context context = getApplicationContext();

        // Create a notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Meeting Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for upcoming meetings");
            notificationManager.createNotificationChannel(channel);
        }

        // Notification intervals in milliseconds
        long[] notificationIntervals = {
                60 * 1000,               // 1 minute before
                5 * 60 * 1000,           // 5 minutes before
                60 * 60 * 1000,          // 1 hour before
                12 * 60 * 60 * 1000,     // 12 hours before
                24 * 60 * 60 * 1000,     // 1 day before
                2 * 24 * 60 * 60 * 1000  // 2 days before
        };

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        String notificationTitle = "Upcoming Meeting"; // Default title
        try {
            Date meetingDateTime = sdf.parse(date + " " + time);
            if (meetingDateTime != null) {
                long currentTimeMillis = System.currentTimeMillis();
                long meetingTimeMillis = meetingDateTime.getTime();
                long timeDiffMillis = meetingTimeMillis - currentTimeMillis;

                // Determine which notification to send based on the time difference
                if (timeDiffMillis <= notificationIntervals[0]) {
                    notificationTitle = "Upcoming meeting '" + title + "' in 1 minute❗";
                } else if (timeDiffMillis <= notificationIntervals[1] && timeDiffMillis > notificationIntervals[0]) {
                    notificationTitle = "Upcoming meeting '" + title + "' in 5 minutes❗";
                } else if (timeDiffMillis <= notificationIntervals[2] && timeDiffMillis > notificationIntervals[1]) {
                    notificationTitle = "Upcoming meeting '" + title + "' in 1 hour❗";
                } else if (timeDiffMillis <= notificationIntervals[3] && timeDiffMillis > notificationIntervals[2]) {
                    notificationTitle = "Upcoming meeting '" + title + "' in 12 hours❗";
                } else if (timeDiffMillis <= notificationIntervals[4] && timeDiffMillis > notificationIntervals[3]) {
                    notificationTitle = "Upcoming meeting '" + title + "' in 24 hours❗";
                } else if (timeDiffMillis <= notificationIntervals[5] && timeDiffMillis > notificationIntervals[4]) {
                    notificationTitle = "Upcoming meeting '" + title + "' in 48 hours❗";
                } else {
                    notificationTitle = "Meeting is starting now!";
                }
            } else {
                notificationTitle = "Upcoming meeting: " + title;
            }
        } catch (ParseException e) {
            Log.e("MeetingNotificationWorker", "Error parsing meeting date/time", e);
            notificationTitle = "Upcoming meeting: " + title;
        }

        // Create an intent that will open the MeetingDetails activity
        Intent intent = new Intent(context, MeetingDetails.class);
        intent.putExtra("meetingId", meetingId);  // Pass the meetingId to the activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Use FLAG_IMMUTABLE if you do not need to modify the PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                meetingId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE  // Use FLAG_IMMUTABLE or FLAG_MUTABLE
        );

        // Build and display the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(notificationTitle)
                .setContentText("Scheduled on " + date + " at " + time + "\nClick to see more details.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)  // Dismiss the notification when clicked
                .setContentIntent(pendingIntent);  // Set the PendingIntent on the notification

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(meetingId.hashCode(), builder.build());  // Unique notification ID based on meetingId

        markNotificationSent(meetingId);  // Mark as sent to avoid sending again
    }

    private boolean isMeetingOngoing(String meetingDate, String meetingTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        try {
            Date meetingDateTime = sdf.parse(meetingDate + " " + meetingTime);
            long currentTimeMillis = System.currentTimeMillis();

            if (meetingDateTime != null) {
                long meetingTimeMillis = meetingDateTime.getTime();

                // Check if the meeting is ongoing or in the past
                boolean isOngoing = meetingTimeMillis <= currentTimeMillis;

                Log.d("MeetingNotificationWorker", "Meeting time: " + meetingDateTime + " Current time: " + new Date(currentTimeMillis) + " Is ongoing: " + isOngoing);
                return isOngoing;
            }
        } catch (ParseException e) {
            Log.e("MeetingNotificationWorker", "Error parsing meeting date/time", e);
        }

        return false;
    }

    private void updateMeetingStatus(String meetingId, String newStatus) {
        Log.d("MeetingNotificationWorker", "Updating meeting " + meetingId + " to status: " + newStatus);

        fStore.collection("meetings")
                .document(meetingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check the current status to avoid unnecessary updates
                        String currentStatus = documentSnapshot.getString("status");
                        if (currentStatus != null && !currentStatus.equals(newStatus)) {
                            documentSnapshot.getReference().update("status", newStatus)
                                    .addOnSuccessListener(aVoid -> Log.d("MeetingNotificationWorker", "Meeting status updated to " + newStatus))
                                    .addOnFailureListener(e -> Log.e("MeetingNotificationWorker", "Error updating meeting status", e));
                        } else {
                            Log.d("MeetingNotificationWorker", "Meeting status is already " + newStatus);
                        }
                    } else {
                        Log.e("MeetingNotificationWorker", "Meeting document not found");
                    }
                })
                .addOnFailureListener(e -> Log.e("MeetingNotificationWorker", "Error retrieving meeting document", e));
    }

    // Mark that a notification has been sent for a particular meeting
    private void markNotificationSent(String meetingId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(meetingId, true);
        editor.apply();
    }
}