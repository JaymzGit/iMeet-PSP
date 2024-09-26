package com.james.imeetpolycc;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Map;

public class MeetingDetails extends AppCompatActivity {

    // Firebase instance
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    // UI elements
    private ImageButton btnBack;
    private TextView tvTitle, tvDate, tvTime, tvOrganiserName, tvOrganiserEmail, tvStatus;
    private ImageView ivOrganiserImage;
    private Button btnViewParticipants, btnUpdateAttendance, btnEditMeeting, btnEndMeeting;
    private RadioGroup radioGroupAttendance;
    private Spinner spinnerReason;

    // Meeting details
    private String meetingID, currentUserEmail;
    private ArrayList<String> participants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_details);

        // Initialize FirebaseAuth and Firestore
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Get the current user's email
        currentUserEmail = fAuth.getCurrentUser() != null ? fAuth.getCurrentUser().getEmail() : null;

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        ivOrganiserImage = findViewById(R.id.ivOrganiserImage);
        tvOrganiserName = findViewById(R.id.tvOrganiserName);
        tvOrganiserEmail = findViewById(R.id.tvOrganiserEmail);
        tvStatus = findViewById(R.id.tvStatus);
        btnViewParticipants = findViewById(R.id.btnViewParticipants);
        btnUpdateAttendance = findViewById(R.id.btnUpdateAttendance);
        radioGroupAttendance = findViewById(R.id.radioGroupAttendance);
        spinnerReason = findViewById(R.id.spinnerReason);
        btnEditMeeting = findViewById(R.id.btnEditMeeting);
        btnEndMeeting = findViewById(R.id.btnEndMeeting);

        // Retrieve meeting ID from the intent
        meetingID = getIntent().getStringExtra("meetingId");

        // Load the meeting details from Firestore
        loadMeetingDetails();

        // Set up back button functionality
        btnBack.setOnClickListener(v -> navigateToMainActivity());

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToMainActivity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload meeting details to ensure the latest data
        loadMeetingDetails();
    }

    private void loadMeetingDetails() {
        fStore.collection("meetings").document(meetingID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String date = documentSnapshot.getString("date");
                        String time = documentSnapshot.getString("time");
                        String organiser = documentSnapshot.getString("organiser");
                        String status = documentSnapshot.getString("status");
                        participants = (ArrayList<String>) documentSnapshot.get("participants");

                        // Update UI with meeting details
                        tvTitle.setText(title);
                        tvDate.setText(" Date: " + date);
                        tvTime.setText(" Time: " + time);
                        tvStatus.setText(" Status: " + status);

                        // Load organizer details
                        loadOrganiserDetails(organiser);

                        // Check if the current user is the organizer
                        if (currentUserEmail != null && currentUserEmail.equals(organiser)) {
                            setupForOrganizer();
                        } else {
                            setupForParticipant();
                            preFillAttendanceStatus();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MeetingDetails.this, "Failed to load meeting details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void preFillAttendanceStatus() {
        fStore.collection("meetings").document(meetingID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ArrayList<Map<String, Object>> participantsList = (ArrayList<Map<String, Object>>) documentSnapshot.get("participants");
                        if (participantsList != null) {
                            for (Map<String, Object> participant : participantsList) {
                                String participantEmail = (String) participant.get("email");
                                if (participantEmail != null && participantEmail.equals(currentUserEmail)) {
                                    Boolean attendance = (Boolean) participant.get("attendance");
                                    String reason = (String) participant.get("reason");

                                    if (attendance != null) {
                                        if (attendance) {
                                            radioGroupAttendance.check(R.id.radioButtonYes);
                                        } else {
                                            radioGroupAttendance.check(R.id.radioButtonNo);
                                            spinnerReason.setVisibility(View.VISIBLE);
                                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerReason.getAdapter();
                                            int spinnerPosition = adapter.getPosition(reason);
                                            spinnerReason.setSelection(spinnerPosition);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MeetingDetails.this, "Failed to retrieve attendance details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadOrganiserDetails(String organiserEmail) {
        fStore.collection("users")
                .whereEqualTo("email", organiserEmail)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            tvOrganiserName.setText("Error: " + error.getMessage());
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            for (DocumentSnapshot document : value.getDocuments()) {
                                String fullName = document.getString("fname");
                                String email = document.getString("email");
                                String imageUrl = document.getString("imageUrl");

                                tvOrganiserName.setText(fullName);
                                tvOrganiserEmail.setText(email);
                                if (imageUrl != null) {
                                    Glide.with(MeetingDetails.this)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.default_user_image)
                                            .error(R.drawable.default_user_image)
                                            .into(ivOrganiserImage);
                                } else {
                                    ivOrganiserImage.setImageResource(R.drawable.default_user_image);
                                }
                            }
                        } else {
                            tvOrganiserName.setText("Organizer not found");
                            tvOrganiserEmail.setText("Email not found");
                            tvStatus.setText("Status not found");
                            ivOrganiserImage.setImageResource(R.drawable.default_user_image);
                        }
                    }
                });
    }

    private void setupForOrganizer() {
        // Hide attendance section if the current user is the organizer
        findViewById(R.id.attendanceCardView).setVisibility(View.GONE);
        btnUpdateAttendance.setVisibility(View.GONE);
        btnEditMeeting.setVisibility(View.VISIBLE);
        btnEndMeeting.setVisibility(View.VISIBLE);
        btnEditMeeting.setOnClickListener(v -> navigateToEditMeeting());
        btnViewParticipants.setOnClickListener(v -> navigateToViewParticipants());
        btnEndMeeting.setOnClickListener(v -> {
            View customDialogView = getLayoutInflater().inflate(R.layout.end_meeting_dialog_box, null);

            TextView dialogTitle = customDialogView.findViewById(R.id.dialogTitle);
            TextView dialogMessage = customDialogView.findViewById(R.id.dialogMessage);
            TextView buttonNo = customDialogView.findViewById(R.id.buttonNo);
            TextView buttonYes = customDialogView.findViewById(R.id.buttonYes);

            androidx.appcompat.app.AlertDialog customDialog = new androidx.appcompat.app.AlertDialog.Builder(MeetingDetails.this)
                    .setView(customDialogView)
                    .create();

            customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: to make it transparent
            customDialog.show();
            customDialog.getWindow().setLayout(1200, 800); // Set your desired width and height here

            // Set button listeners
            buttonNo.setOnClickListener(view -> customDialog.dismiss());
            buttonYes.setOnClickListener(view -> {
                FirebaseUser user = fAuth.getCurrentUser();
                endMeeting();
            });
        });
    }

    private void setupForParticipant() {
        // Get the reasons array from resources
        String[] reasonsArray = getResources().getStringArray(R.array.reasons_array);

        // Initialize the custom adapter
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(this, reasonsArray);
        spinnerReason.setAdapter(adapter);

        // Set the popup background color
        spinnerReason.setPopupBackgroundDrawable(new ColorDrawable(Color.rgb(203, 170, 141))); // Set popup background

        spinnerReason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedItemView, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                // Handle the selected item if needed
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case when nothing is selected if needed
            }
        });

        radioGroupAttendance.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioButtonNo) {
                spinnerReason.setVisibility(View.VISIBLE);
            } else {
                spinnerReason.setVisibility(View.GONE);
            }
        });

        btnUpdateAttendance.setOnClickListener(v -> updateAttendance());

        // Set up view participants button
        btnViewParticipants.setOnClickListener(v -> navigateToViewParticipants());
    }

    private void navigateToMainActivity() {
        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(mainIntent);
        finish(); // Optionally close the current activity
    }

    private void navigateToEditMeeting() {
        Intent editIntent = new Intent(MeetingDetails.this, EditMeeting.class);
        editIntent.putExtra("meetingId", meetingID);
        startActivity(editIntent);
    }

    private void navigateToViewParticipants() {
        Intent participantsIntent = new Intent(MeetingDetails.this, EditAttendance.class);
        participantsIntent.putExtra("meetingId", meetingID);
        startActivity(participantsIntent);
    }

    private void endMeeting() {
        Log.d("MeetingDetails", "Updating meeting " + meetingID + " to Ended.");

        fStore.collection("meetings")
                .document(meetingID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check the current status to avoid unnecessary updates
                        String currentStatus = documentSnapshot.getString("status");
                        if (currentStatus != null && !currentStatus.equals("Ended")) {
                            documentSnapshot.getReference().update("status", "Ended")
                                    .addOnSuccessListener(aVoid -> Log.d("MeetingDetails", "Meeting status updated to " + "Ended"))
                                    .addOnFailureListener(e -> Log.e("MeetingDetails", "Error updating meeting status", e));
                        } else {
                            Log.d("MeetingDetails", "Meeting status is already Ended");
                        }
                    } else {
                        Log.e("MeetingDetails", "Meeting document not found");
                    }

                    Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(mainIntent);
                    finish(); // Optionally close the current activity
                })
                .addOnFailureListener(e -> Log.e("MeetingDetails", "Error retrieving meeting document", e));
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
            fStore.collection("meetings")
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
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(MeetingDetails.this, "Attendance updated successfully", Toast.LENGTH_SHORT).show();
                                                    navigateToMainActivity();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(MeetingDetails.this, "Failed to update attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                        break;
                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(MeetingDetails.this, "Failed to retrieve meeting details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Please select an attendance option", Toast.LENGTH_SHORT).show();
        }
    }
}