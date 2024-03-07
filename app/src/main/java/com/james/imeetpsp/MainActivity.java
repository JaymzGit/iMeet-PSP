package com.james.imeetpsp;

import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.appcheck.internal.util.Logger;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    ImageView btnUserProfile;
    TextView textViewNoMeetings;
    Button btnRegisterMeeting;
    RecyclerView rvUpcomingMeetings;
    private ArrayList<Meeting> upcomingMeetingsList;
    private MeetingAdapter meetingAdapter;
    FirebaseFirestore db;
    FirebaseAuth fAuth;
    String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        fAuth = FirebaseAuth.getInstance();
        // Check if the current user is not null before accessing its email
        if (fAuth.getCurrentUser() != null) {
            currentUserEmail = fAuth.getCurrentUser().getEmail();
        } else {
            // If the current user is null, redirect to the login activity
            startActivity(new Intent(this, Login.class));
            finish();
            return; // Exit the onCreate method to prevent further execution
        }

        textViewNoMeetings = findViewById(R.id.textViewNoMeetings);
        btnUserProfile = findViewById(R.id.buttonUserProfile);
        btnRegisterMeeting = findViewById(R.id.buttonRegisterMeeting);
        rvUpcomingMeetings = findViewById(R.id.rvUpcomingMeetings);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        upcomingMeetingsList = new ArrayList<>();
        meetingAdapter = new MeetingAdapter(upcomingMeetingsList);
        rvUpcomingMeetings.setLayoutManager(new LinearLayoutManager(this));
        rvUpcomingMeetings.setAdapter(meetingAdapter);

        fetchUpcomingMeetings();

       btnRegisterMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), RegisterMeeting.class));
            }
        });

        btnUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), UserProfile.class));
                finish();
            }
        });

        btnRegisterMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), RegisterMeeting.class));
                finish();
            }
        });

        startService(new Intent(this, MeetingMonitoringService.class));
    }

    private void fetchUpcomingMeetings() {
        db.collection("meetings")
                .whereIn("status", Arrays.asList("Ongoing", "Upcoming"))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Error fetching documents: ", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        upcomingMeetingsList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Meeting meeting = document.toObject(Meeting.class);
                            // Check if the current user is the organizer or a participant of the meeting
                            if (meeting.getOrganiser() != null && meeting.getOrganiser().equals(currentUserEmail)) {
                                upcomingMeetingsList.add(meeting);
                            } else if (meeting.getParticipants() != null && meeting.getParticipants().contains(currentUserEmail)) {
                                upcomingMeetingsList.add(meeting);
                            }
                        }

                        // Sort the list based on meeting date and time with ongoing meetings first
                        Collections.sort(upcomingMeetingsList, new Comparator<Meeting>() {
                            @Override
                            public int compare(Meeting meeting1, Meeting meeting2) {
                                boolean isOngoingMeeting1 = meeting1.getStatus().equals("Ongoing");
                                boolean isOngoingMeeting2 = meeting2.getStatus().equals("Ongoing");

                                // If one meeting is ongoing and the other is not, prioritize ongoing meeting
                                if (isOngoingMeeting1 && !isOngoingMeeting2) {
                                    return -1; // Meeting 1 comes before Meeting 2
                                } else if (!isOngoingMeeting1 && isOngoingMeeting2) {
                                    return 1; // Meeting 2 comes before Meeting 1
                                } else {
                                    // If both meetings are ongoing or upcoming, compare their date and time
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
                            }
                        });

                        meetingAdapter.notifyDataSetChanged();
                        if (upcomingMeetingsList.isEmpty()) {
                            textViewNoMeetings.setVisibility(View.VISIBLE);
                        } else {
                            textViewNoMeetings.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d("Firestore", "No documents found.");
                    }
                });
    }

    @Override
    public void onBackPressed() {
        //TODO: Are you sure you want to log out? prompt
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getApplicationContext(), Login.class));
        finish();
    }
}