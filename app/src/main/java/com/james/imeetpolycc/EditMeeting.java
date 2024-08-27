package com.james.imeetpolycc;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class EditMeeting extends AppCompatActivity {

    // Firebase Instances
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    // UI Elements
    private ImageButton btnBack;
    private EditText etTitle, etDate, etTime, etParticipants;
    private Button btnUpdateMeeting, btnDeleteMeeting;

    private static final int SELECT_PARTICIPANTS_REQUEST_CODE = 1001;
    private static final String SELECTED_PARTICIPANTS_KEY = "selectedParticipants";
    private String meetingId; // To store the ID of the meeting being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_meeting);

        // Apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize Firebase instances
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack);
        etTitle = findViewById(R.id.etMeetingTitle);
        etDate = findViewById(R.id.etMeetingDate);
        etTime = findViewById(R.id.etMeetingTime);
        etParticipants = findViewById(R.id.etAddParticipants);
        btnUpdateMeeting = findViewById(R.id.btnUpdateMeeting);
        btnDeleteMeeting = findViewById(R.id.btnDeleteMeeting);

        // Retrieve the meeting ID from the Intent
        meetingId = getIntent().getStringExtra("MEETING_ID");
        if (meetingId != null) {
            loadMeetingDetails(meetingId);
        }

        // Set click listeners
        etDate.setOnClickListener(v -> showDatePickerDialog());
        etTime.setOnClickListener(v -> showTimePickerDialog());
        etParticipants.setOnClickListener(v -> {
            Intent intent = new Intent(EditMeeting.this, SelectParticipants.class);
            startActivityForResult(intent, SELECT_PARTICIPANTS_REQUEST_CODE);
        });

        btnBack.setOnClickListener(v -> onBackPressed());

        btnUpdateMeeting.setOnClickListener(v -> {
            // Retrieve input values
            String title = etTitle.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            // Check if any field is empty
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
                Toast.makeText(EditMeeting.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update meeting data in Firestore
            updateMeetingInFirestore(meetingId, title, date, time);
        });

        btnDeleteMeeting.setOnClickListener(v -> {
            // Confirm deletion
            new androidx.appcompat.app.AlertDialog.Builder(EditMeeting.this)
                    .setTitle("Delete Meeting")
                    .setMessage("Are you sure you want to delete this meeting?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteMeetingFromFirestore(meetingId))
                    .setNegativeButton("No", null)
                    .create()
                    .show();
        });

        // Handle back press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                clearSharedPreferencesData();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish(); // Optionally close the current activity
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PARTICIPANTS_REQUEST_CODE && resultCode == RESULT_OK) {
            // Update the UI to display the number of selected participants
            ArrayList<String> selectedParticipants = getSharedPreferencesData();
            int participantCount = selectedParticipants.size();
            String message = participantCount + (participantCount == 1 ? " participant selected" : " participants selected");
            etParticipants.setText(message);
        }
    }

    @Override
    public void onDestroy() {
        clearSharedPreferencesData();
        super.onDestroy();
    }

    private void clearSharedPreferencesData() {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear(); // Clear all data
        editor.apply();
    }

    private void loadMeetingDetails(String meetingId) {
        DocumentReference meetingRef = fStore.collection("meetings").document(meetingId);

        meetingRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String title = documentSnapshot.getString("title");
                String date = documentSnapshot.getString("date");
                String time = documentSnapshot.getString("time");
                ArrayList<String> participants = (ArrayList<String>) documentSnapshot.get("participants");

                etTitle.setText(title);
                etDate.setText(date);
                etTime.setText(time);

                // Update participants display
                if (participants != null) {
                    int participantCount = participants.size();
                    String message = participantCount + (participantCount == 1 ? " participant selected" : " participants selected");
                    etParticipants.setText(message);
                }
            } else {
                Toast.makeText(EditMeeting.this, "Meeting not found", Toast.LENGTH_SHORT).show();
                finish(); // Close activity if meeting not found
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error loading meeting details", e);
            Toast.makeText(EditMeeting.this, "Error loading meeting details", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateMeetingInFirestore(String meetingId, String title, String date, String time) {
        ArrayList<String> selectedParticipants = getSharedPreferencesData();

        if (!selectedParticipants.isEmpty()) {
            // Get the current user's email (organiser)
            String organiserEmail = fAuth.getCurrentUser().getEmail();

            // Update meeting data in Firestore
            DocumentReference meetingRef = fStore.collection("meetings").document(meetingId);

            Map<String, Object> meetingData = new HashMap<>();
            meetingData.put("title", title);
            meetingData.put("date", date);
            meetingData.put("time", time);
            meetingData.put("participants", selectedParticipants);

            meetingRef.update(meetingData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditMeeting.this, "Meeting updated successfully", Toast.LENGTH_SHORT).show();
                        // Clear input fields and SharedPreferences
                        etTitle.setText("");
                        etDate.setText("");
                        etTime.setText("");
                        etParticipants.setText("");
                        clearSharedPreferencesData();
                        finish(); // Close activity after update
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditMeeting.this, "Failed to update meeting", Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Error updating document", e);
                    });
        } else {
            Toast.makeText(EditMeeting.this, "Please select meeting participants", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteMeetingFromFirestore(String meetingId) {
        DocumentReference meetingRef = fStore.collection("meetings").document(meetingId);

        meetingRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditMeeting.this, "Meeting deleted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity after deletion
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditMeeting.this, "Failed to delete meeting", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error deleting document", e);
                });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialogTheme, // Apply custom theme here
                (view, year1, month1, dayOfMonth1) -> {
                    // Check if the selected date is not in the past
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, month1, dayOfMonth1);
                    Calendar currentDate = Calendar.getInstance();
                    if (selectedDate.before(currentDate)) {
                        // Date is in the past, show a message or take appropriate action
                        Toast.makeText(EditMeeting.this, "Date is not valid", Toast.LENGTH_SHORT).show();
                    } else {
                        // Do something with the selected date
                        String selectedDateStr = dayOfMonth1 + "/" + (month1 + 1) + "/" + year1;
                        etDate.setText(selectedDateStr);
                    }
                },
                year,
                month,
                dayOfMonth
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                R.style.CustomTimePickerDialogTheme, // Apply custom theme here
                (view, hourOfDay, minute1) -> {
                    // Format the time as HH:mm
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    etTime.setText(timeStr);
                },
                hour,
                minute,
                true
        );
        timePickerDialog.show();
    }

    private ArrayList<String> getSharedPreferencesData() {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        HashSet<String> participantsSet = (HashSet<String>) preferences.getStringSet(SELECTED_PARTICIPANTS_KEY, new HashSet<>());
        return new ArrayList<>(participantsSet);
    }
}
