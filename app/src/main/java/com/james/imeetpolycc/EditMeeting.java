package com.james.imeetpolycc;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import java.util.List;
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

        // Retrieve the meetingId from the intent
        Intent intent = getIntent();
        if (intent != null) {
            meetingId = intent.getStringExtra("meetingId");

            // Query Firestore for meeting details using meetingId
            loadMeetingDetails(meetingId);
        }

        // Set click listeners
        etDate.setOnClickListener(v -> showDatePickerDialog());
        etTime.setOnClickListener(v -> showTimePickerDialog());
        etParticipants.setOnClickListener(v -> {
            Intent participantSelectIntent = new Intent(EditMeeting.this, SelectParticipants.class);
            startActivityForResult(participantSelectIntent, SELECT_PARTICIPANTS_REQUEST_CODE);
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
            // Inflate the custom layout
            View customDialogView = getLayoutInflater().inflate(R.layout.delete_meeting_dialog_box, null);

            // Find views in the custom layout
            TextView dialogTitle = customDialogView.findViewById(R.id.dialogTitle);
            TextView dialogMessage = customDialogView.findViewById(R.id.dialogMessage);
            TextView buttonNo = customDialogView.findViewById(R.id.buttonNo);
            TextView buttonYes = customDialogView.findViewById(R.id.buttonYes);

            // Create and set up the AlertDialog
            androidx.appcompat.app.AlertDialog customDialog = new androidx.appcompat.app.AlertDialog.Builder(EditMeeting.this)
                    .setView(customDialogView)
                    .create();

            // Make the dialog background transparent
            if (customDialog.getWindow() != null) {
                customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Set button listeners
            buttonNo.setOnClickListener(view -> customDialog.dismiss());

            buttonYes.setOnClickListener(view -> {
                // Delete meeting from Firestore
                deleteMeetingFromFirestore(meetingId);

                // Call the static method to cancel the notification
                MeetingNotificationWorker.cancelNotification(EditMeeting.this, meetingId);

                // Redirect to MainActivity
                Intent deletedIntent = new Intent(EditMeeting.this, MainActivity.class);
                deletedIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(deletedIntent);
                finish();  // Optional: Close EditMeeting activity if desired
            });

            // Show the dialog
            customDialog.show();
        });

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Return to MeetingDetails activity with updated data
                Intent intent = new Intent(EditMeeting.this, MeetingDetails.class);
                intent.putExtra("meetingId", meetingId);
                intent.putExtra("title", etTitle.getText().toString().trim());
                intent.putExtra("date", etDate.getText().toString().trim());
                intent.putExtra("time", etTime.getText().toString().trim());
                intent.putStringArrayListExtra("participants", getSharedPreferencesData());
                startActivity(intent);
                finish();
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
                List<Map<String, Object>> participants = (List<Map<String, Object>>) documentSnapshot.get("participants");

                etTitle.setText(title);
                etDate.setText(date);
                etTime.setText(time);

                // Update participants display
                if (participants != null) {
                    ArrayList<String> participantEmails = new ArrayList<>();
                    for (Map<String, Object> participant : participants) {
                        String email = (String) participant.get("email");
                        participantEmails.add(email);
                    }
                    int participantCount = participantEmails.size();
                    String message = participantCount + (participantCount == 1 ? " participant selected" : " participants selected");
                    etParticipants.setText(message);

                    // Save participants to SharedPreferences for auto-selection
                    saveParticipantsToSharedPreferences(participantEmails);
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

            // Prepare participants data
            List<Map<String, Object>> participantsData = new ArrayList<>();
            for (String participantEmail : selectedParticipants) {
                Map<String, Object> participantData = new HashMap<>();
                participantData.put("email", participantEmail);
                participantData.put("attendance", false); // Initialize attendance as false
                participantData.put("reason", ""); // Initialize reason as empty string
                participantsData.add(participantData);
            }

            // Prepare meeting data
            Map<String, Object> meetingData = new HashMap<>();
            meetingData.put("title", title);
            meetingData.put("date", date);
            meetingData.put("time", time);
            meetingData.put("status", "Upcoming");
            meetingData.put("organiser", organiserEmail);
            meetingData.put("participants", participantsData);

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

    // Show DatePickerDialog to select a date
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialogTheme, // Apply custom theme here
                (view, year1, month1, dayOfMonth1) -> {
                    // Set the selected date to the EditText
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, month1, dayOfMonth1);
                    Calendar currentDate = Calendar.getInstance();
                    if (selectedDate.before(currentDate)) {
                        // Date is in the past, show a message
                        Toast.makeText(EditMeeting.this, "Date is not valid", Toast.LENGTH_SHORT).show();
                    } else {
                        // Set the selected date to the EditText
                        String selectedDateStr = dayOfMonth1 + "/" + (month1 + 1) + "/" + year1;
                        etDate.setText(selectedDateStr);
                    }
                },
                year,
                month,
                dayOfMonth
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // Show TimePickerDialog to select a time
    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                R.style.CustomTimePickerDialogTheme, // Apply custom theme here
                (view, hourOfDay1, minute1) -> {
                    // Set the selected time to the EditText
                    String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay1, minute1);
                    etTime.setText(selectedTime);
                },
                hourOfDay,
                minute,
                false
        );
        timePickerDialog.show();
    }

    private void saveParticipantsToSharedPreferences(ArrayList<String> participants) {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(SELECTED_PARTICIPANTS_KEY, new HashSet<>(participants));
        editor.apply();
    }

    private ArrayList<String> getSharedPreferencesData() {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        HashSet<String> participantsSet = (HashSet<String>) preferences.getStringSet(SELECTED_PARTICIPANTS_KEY, new HashSet<>());
        return new ArrayList<>(participantsSet);
    }
}