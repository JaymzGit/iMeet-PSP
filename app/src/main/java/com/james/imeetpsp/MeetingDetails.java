package com.james.imeetpsp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class MeetingDetails extends AppCompatActivity {

    // Firebase instance
    private FirebaseAuth fAuth;

    // UI elements
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvDate;
    private TextView tvTime;
    private ImageView ivOrganizerImage;
    private TextView tvOrganizerName;
    private TextView tvOrganizerEmail;
    private TextView tvStatus;
    private Button btnViewParticipants;
    private Button btnUpdateAttendance;
    private RadioGroup radioGroupAttendance;
    private Spinner spinnerReason;
    private Button btnEditMeeting;

    // Meeting details
    private String title;
    private String date;
    private String time;
    private String organizer;
    private String status;
    private String meetingID;
    private String currentUserEmail;
    private ArrayList<String> participants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_details);

        // Apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize FirebaseAuth
        fAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        ivOrganizerImage = findViewById(R.id.ivOrganizer);
        tvOrganizerName = findViewById(R.id.tvOrganizerName);
        tvOrganizerEmail = findViewById(R.id.tvOrganizerEmail);
        tvStatus = findViewById(R.id.tvStatus);
        btnViewParticipants = findViewById(R.id.btnViewParticipants);
        btnUpdateAttendance = findViewById(R.id.btnUpdateAttendance);
        radioGroupAttendance = findViewById(R.id.radioGroupAttendance);
        spinnerReason = findViewById(R.id.spinnerReason);
        btnEditMeeting = findViewById(R.id.btnEditMeeting);

        // Check if the current user is not null before accessing its email
        if (fAuth.getCurrentUser() != null) {
            currentUserEmail = fAuth.getCurrentUser().getEmail();
        } else {
            // Redirect to login if the current user is null
            startActivity(new Intent(this, Login.class));
            finish();
            return; // Exit the onCreate method to prevent further execution
        }

        // Set up back button functionality
        btnBack.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), MainActivity.class)));

        // Retrieve data from the intent
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                meetingID = extras.getString("meetingId");
                title = extras.getString("title");
                date = extras.getString("date");
                time = extras.getString("time");
                organizer = extras.getString("organizer");
                status = extras.getString("status");
                participants = extras.getStringArrayList("participants");

                // Set data to UI elements
                tvTitle.setText(title);
                tvDate.setText(" Date: " + date);
                tvTime.setText(" Time: " + time);
                tvStatus.setText(" Status: " + status);

                // Load organizer details
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .whereEqualTo("email", organizer)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                if (error != null) {
                                    tvOrganizerName.setText("Error: " + error.getMessage());
                                    return;
                                }

                                if (value != null && !value.isEmpty()) {
                                    for (DocumentSnapshot document : value.getDocuments()) {
                                        String fullName = document.getString("fname");
                                        String email = document.getString("email");
                                        String imageUrl = document.getString("imageUrl");

                                        tvOrganizerName.setText(fullName);
                                        tvOrganizerEmail.setText(email);
                                        if (imageUrl != null) {
                                            Glide.with(MeetingDetails.this)
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.default_user_image)
                                                    .error(R.drawable.default_user_image)
                                                    .into(ivOrganizerImage);
                                        } else {
                                            ivOrganizerImage.setImageResource(R.drawable.default_user_image);
                                        }
                                    }
                                } else {
                                    tvOrganizerName.setText("Organizer not found");
                                    tvOrganizerEmail.setText("");
                                    ivOrganizerImage.setImageResource(R.drawable.default_user_image);
                                }
                            }
                        });

                // Check if the current user is the organizer
                if (currentUserEmail.equals(organizer)) {
                    // Hide attendance section if the current user is the organizer
                    findViewById(R.id.attendanceCardView).setVisibility(View.GONE);
                    btnUpdateAttendance.setVisibility(View.GONE);
                    btnEditMeeting.setVisibility(View.VISIBLE);
                    btnEditMeeting.setOnClickListener(v -> {
                        Intent editIntent = new Intent(MeetingDetails.this, EditMeeting.class);
                        editIntent.putStringArrayListExtra("participants", participants);
                        startActivity(editIntent);
                    });
                }

                // Initialize attendance section views
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.reasons_array, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerReason.setAdapter(adapter);

                radioGroupAttendance.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.radioButtonNo) {
                        spinnerReason.setVisibility(View.VISIBLE);
                    } else {
                        spinnerReason.setVisibility(View.GONE);
                    }
                });

                btnUpdateAttendance.setOnClickListener(v -> updateAttendance());

                // Set up view participants button
                btnViewParticipants.setOnClickListener(v -> {
                    Intent participantsIntent = new Intent(MeetingDetails.this, EditAttendance.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("meetingId", meetingID);
                    bundle.putString("organizer", organizer);
                    bundle.putStringArrayList("participants", participants);
                    participantsIntent.putExtras(bundle);
                    startActivity(participantsIntent);
                });
            }
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish(); // Optionally close the current activity
            }
        });
    }

    private void updateAttendance() {
        // Get the selected attendance option
        int checkedRadioButtonId = radioGroupAttendance.getCheckedRadioButtonId();

        if (checkedRadioButtonId != -1) {
            boolean attendance;
            String reason;
            RadioButton radioButton = findViewById(checkedRadioButtonId);
            if (radioButton.getId() == R.id.radioButtonYes) {
                reason = "";
                attendance = true;
            } else {
                attendance = false;
                reason = spinnerReason.getSelectedItem().toString();
            }

            // Update attendance and reason in Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("meetings")
                    .document(meetingID)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ArrayList<Map<String, Object>> participantsList = (ArrayList<Map<String, Object>>) documentSnapshot.get("participants");
                            if (participantsList != null) {
                                for (Map<String, Object> participant : participantsList) {
                                    String participantEmail = (String) participant.get("email");
                                    if (participantEmail != null && participantEmail.equals(currentUserEmail)) {
                                        participant.put("attendance", attendance);
                                        participant.put("reason", reason);
                                        documentSnapshot.getReference().update("participants", participantsList)
                                                .addOnSuccessListener(aVoid -> Toast.makeText(MeetingDetails.this, "Attendance updated successfully", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e -> Toast.makeText(MeetingDetails.this, "Failed to update attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                        break;
                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(MeetingDetails.this, "Failed to update attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(MeetingDetails.this, "Please select your attendance status", Toast.LENGTH_SHORT).show();
        }
    }
}