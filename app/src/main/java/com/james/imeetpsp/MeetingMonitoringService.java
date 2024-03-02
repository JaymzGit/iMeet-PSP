package com.james.imeetpsp;

import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MeetingMonitoringService extends Service {
    private static final long CHECK_INTERVAL = 60000; // Check every minute
    private static final long TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private static final long TWELVE_HOURS = 12 * 60 * 60 * 1000; // 12 hours in milliseconds
    private static final long ONE_HOUR = 60 * 60 * 1000; // 1 hour in milliseconds
    private Handler handler = new Handler();
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
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkMeetingStatusRunnable); // Stop the periodic check
    }

    private boolean isMeetingUpcoming(String meetingDate, String meetingTime) {
        // Get the current date and time in GMT+8 timezone
        Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));

        // Parse meeting date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // Set timezone to GMT+8
        try {
            Date meetingDateTime = sdf.parse(meetingDate + " " + meetingTime);

            // Check if the meeting date and time have passed
            if (meetingDateTime != null && currentTime.getTime().after(meetingDateTime)) {
                return true; // Meeting has passed
            }

            return false; // Meeting is still upcoming
        } catch (ParseException e) {
            e.printStackTrace();
            return false; // Return false if unable to parse the date or time
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
                        Log.d(TAG, "Meeting status updated successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating meeting status", e);
                    }
                });
    }

    private void checkAndUpdateMeetingStatus() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference meetingsRef = db.collection("meetings");

        meetingsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (querySnapshot != null) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String meetingDate = document.getString("date");
                        String meetingTime = document.getString("time");
                        String status = document.getString("status");

                        if (meetingDate != null && meetingTime != null && status != null && !status.equals("Ended") && !status.equals("Ongoing")) {
                            if (isMeetingUpcoming(meetingDate, meetingTime)) {
                                updateMeetingStatus(document.getId(), "Ongoing");
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "No meetings found");
                }
            }
        });
    }
}

