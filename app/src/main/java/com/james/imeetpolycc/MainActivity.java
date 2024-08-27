package com.james.imeetpolycc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    // Firebase instances
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    // UI elements
    private ImageView btnUserProfile;
    private TextView tvNoMeetings;
    private RecyclerView rvUpcomingMeetings;
    private Button btnRegisterMeeting;

    // Data
    private ArrayList<Meeting> upcomingMeetingsList;
    private MeetingAdapter meetingAdapter;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply system-wide dark mode setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize Firebase instances
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Check if the current user is logged in
        if (fAuth.getCurrentUser() != null) {
            currentUserEmail = fAuth.getCurrentUser().getEmail();
        } else {
            // Redirect to login activity if no user is logged in
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        // Initialize UI elements
        btnUserProfile = findViewById(R.id.btnUserProfile);
        tvNoMeetings = findViewById(R.id.tvNoMeetings);
        rvUpcomingMeetings = findViewById(R.id.rvUpcomingMeetings);
        btnRegisterMeeting = findViewById(R.id.btnRegisterMeeting);

        // Initialize data and adapter
        upcomingMeetingsList = new ArrayList<>();
        meetingAdapter = new MeetingAdapter(upcomingMeetingsList);
        rvUpcomingMeetings.setLayoutManager(new LinearLayoutManager(this));
        rvUpcomingMeetings.setAdapter(meetingAdapter);

        // Fetch and display upcoming meetings
        fetchUpcomingMeetings();

        // Set up button listeners
        btnRegisterMeeting.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RegisterMeeting.class));
            finish();
        });

        btnUserProfile.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), UserProfile.class));
            finish();
        });

        // Handle back press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    private void fetchUpcomingMeetings() {
        fStore.collection("meetings")
                .whereIn("status", Arrays.asList("Ongoing", "Upcoming"))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Error fetching documents: ", error);
                        return;
                    }

                    if (value != null) {
                        upcomingMeetingsList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Meeting meeting = document.toObject(Meeting.class);
                            Log.d("Firestore", "Fetched meeting: " + meeting.getOrganiser() + ", Participants: " + meeting.getParticipants());

                            // Check if the current user is the organizer or a participant
                            if (meeting.getOrganiser() != null && meeting.getOrganiser().equals(currentUserEmail)) {
                                upcomingMeetingsList.add(meeting);
                            } else if (meeting.getParticipants() != null) {
                                List<String> participantEmails = meeting.getParticipantEmails();
                                if (participantEmails.contains(currentUserEmail)) {
                                    upcomingMeetingsList.add(meeting);
                                }
                            }
                        }

                        // Sort meetings: ongoing first, then by date and time
                        Collections.sort(upcomingMeetingsList, (meeting1, meeting2) -> {
                            boolean isOngoingMeeting1 = meeting1.getStatus().equals("Ongoing");
                            boolean isOngoingMeeting2 = meeting2.getStatus().equals("Ongoing");

                            // Prioritize ongoing meetings
                            if (isOngoingMeeting1 && !isOngoingMeeting2) {
                                return -1;
                            } else if (!isOngoingMeeting1 && isOngoingMeeting2) {
                                return 1;
                            } else {
                                // Sort by date and time
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                                try {
                                    Date date1 = sdf.parse(meeting1.getDate() + " " + meeting1.getTime());
                                    Date date2 = sdf.parse(meeting2.getDate() + " " + meeting2.getTime());
                                    return date1.compareTo(date2);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    return 0;
                                }
                            }
                        });

                        // Notify adapter and update UI
                        meetingAdapter.notifyDataSetChanged();

                        // Update visibility of the "No Meetings" text
                        if (upcomingMeetingsList.isEmpty()) {
                            tvNoMeetings.setVisibility(View.VISIBLE);
                            rvUpcomingMeetings.setVisibility(View.GONE);
                        } else {
                            tvNoMeetings.setVisibility(View.GONE);
                            rvUpcomingMeetings.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d("Firestore", "No documents found.");
                        tvNoMeetings.setVisibility(View.VISIBLE);
                        rvUpcomingMeetings.setVisibility(View.GONE);
                    }
                });
    }
}
