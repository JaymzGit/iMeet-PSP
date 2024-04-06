package com.james.imeetpsp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MeetingMonitoringService extends Service {
    private static final long CHECK_INTERVAL = 60000; // Check every minute
    private static final long TWO_DAYS = 48 * 60 * 60 * 1000; // 2 days in milliseconds
    private static final long TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private static final long TWELVE_HOURS = 12 * 60 * 60 * 1000; // 12 hours in milliseconds
    private static final long ONE_HOUR = 60 * 60 * 1000; // 1 hour in milliseconds
    private Handler handler = new Handler();
    private String currentUserEmail;

    private String meetingTitle;

    private Runnable checkMeetingStatusRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndUpdateMeetingStatus();
            handler.postDelayed(this, CHECK_INTERVAL); // Schedule the next check
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.postDelayed(checkMeetingStatusRunnable, CHECK_INTERVAL); // Start the periodic check

        // Create the notification channel
        createNotificationChannel();

        // Get the current user's email
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        currentUserEmail = null;
        if (currentUser != null) {
            currentUserEmail = currentUser.getEmail();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkMeetingStatusRunnable); // Stop the periodic check
    }

    private void createNotificationChannel() {
        // Check if the device is running on Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the notification channel
            NotificationChannel channel = new NotificationChannel("iMeet PSP", "iMeet PSP", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for iMeet PSP notifications");

            // Get the notification manager
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            // Create the channel
            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean isMeeting48HoursAway(String meetingDate, String meetingTime, long currentTimeMillis) {
        return calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis) <= TWO_DAYS &&
                calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis) > TWENTY_FOUR_HOURS;
    }

    private boolean isMeeting24HoursAway(String meetingDate, String meetingTime, long currentTimeMillis) {
        return calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis) <= TWENTY_FOUR_HOURS &&
                calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis) > TWELVE_HOURS;
    }

    private boolean isMeeting12HoursAway(String meetingDate, String meetingTime, long currentTimeMillis) {
        return calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis) <= TWELVE_HOURS &&
                calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis) > ONE_HOUR;
    }

    private boolean isMeeting1HourAway(String meetingDate, String meetingTime, long currentTimeMillis) {
        return calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis) <= ONE_HOUR;
    }

    private boolean isMeetingFewSecondsAway(String meetingDate, String meetingTime, long currentTimeMillis) {
        long timeDifference = calculateTimeDifference(meetingDate, meetingTime, currentTimeMillis);
        return timeDifference > 0 && timeDifference <= 1 * 60 * 1000; // Check if the meeting is within a minute
    }

    private long calculateTimeDifference(String meetingDate, String meetingTime, long currentTimeMillis) {
        // Parse meeting date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // Set timezone to GMT+8
        try {
            Date meetingDateTime = sdf.parse(meetingDate + " " + meetingTime);

            // Calculate the time difference
            return meetingDateTime.getTime() - currentTimeMillis;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // Return 0 if unable to parse the date or time
        }
    }

    private void updateMeetingStatus(String meetingId, String status) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference meetingRef = db.collection("meetings").document(meetingId);

        meetingRef
                .update("status", status)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("MeetingMonitoring", "Meeting status updated successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("MeetingMonitoring", "Error updating meeting status", e);
                    }
                });
    }

    private void checkAndUpdateMeetingStatus() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference meetingsRef = db.collection("meetings");

        long currentTimeMillis = System.currentTimeMillis();

        meetingsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e("MeetingMonitoring", "Listen failed.", e);
                    return;
                }

                if (querySnapshot != null) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String meetingDate = document.getString("date");
                        meetingTitle = document.getString("title");
                        String meetingTime = document.getString("time");
                        String status = document.getString("status");
                        String organiser = document.getString("organiser");
                        ArrayList<String> participants = (ArrayList<String>) document.get("participants");

                        if (meetingDate != null && meetingTime != null && status != null &&
                                !status.equals("Ended") && !status.equals("Ongoing")) {
                            if (isMeeting48HoursAway(meetingDate, meetingTime, currentTimeMillis)) {
                                // Schedule notification for 48 hours before the meeting
                                scheduleNotification(document.getId(), "Meeting is starting in 48 hours.", TWO_DAYS);
                            } else if (isMeeting24HoursAway(meetingDate, meetingTime, currentTimeMillis)) {
                                // Schedule notification for 24 hours before the meeting
                                scheduleNotification(document.getId(), "Meeting is starting in 24 hours.", TWENTY_FOUR_HOURS);
                            } else if (isMeeting12HoursAway(meetingDate, meetingTime, currentTimeMillis)) {
                                // Schedule notification for 12 hours before the meeting
                                scheduleNotification(document.getId(), "Meeting is starting in 12 hours.", TWELVE_HOURS);
                            } else if (isMeeting1HourAway(meetingDate, meetingTime, currentTimeMillis)) {
                                // Schedule notification for 1 hour before the meeting
                                scheduleNotification(document.getId(), "Meeting is starting in 1 hour.", ONE_HOUR);
                            }

                            if (isMeetingFewSecondsAway(meetingDate, meetingTime, currentTimeMillis)) {
                                // Check if the current user is included in the meeting participants
                                if ((participants != null && participants.contains(currentUserEmail)) || (currentUserEmail != null && currentUserEmail.equals(organiser))) {
                                    // Send notification and set status to "Ongoing"
                                    scheduleNotification(document.getId(), "Meeting is starting now!", 0); // 0 for immediate notification
                                    updateMeetingStatus(document.getId(), "Ongoing");
                                }
                            }
                        }
                    }
                } else {
                    Log.d("MeetingMonitoring", "No meetings found");
                }
            }
        });
    }

    private void scheduleNotification(String meetingId, String notificationMessage, long notificationTimeMillis) {
        // Use your existing NotificationReceiver class to handle notifications
        Intent intent = new Intent(getApplicationContext(), NotificationReceiver.class);
        intent.putExtra("meetingId", meetingId);
        intent.putExtra("meetingTitle", meetingTitle);
        intent.putExtra("notificationMessage", notificationMessage);
        // Use PendingIntent.getBroadcast to create a PendingIntent for a broadcast
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "iMeet PSP")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(meetingTitle)
                .setContentText(notificationMessage)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Get the notification manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        // Schedule the notification
        notificationManager.notify((int) notificationTimeMillis, builder.build());
    }
}