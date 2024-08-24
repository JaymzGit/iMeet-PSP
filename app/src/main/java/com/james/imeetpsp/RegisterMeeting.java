package com.james.imeetpsp;

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
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.activity.OnBackPressedCallback;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RegisterMeeting extends AppCompatActivity {

    // UI elements
    ImageButton btnBack;
    EditText etTitle, etDate, etTime, etParticipants;
    Button btnAddMeeting;
    ProgressBar progressBar;

    // Constants
    private static final String SELECTED_PARTICIPANTS_KEY = "selectedParticipants";

    // ActivityResultLauncher for SelectParticipants activity
    private final ActivityResultLauncher<Intent> selectParticipantsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Update the UI to display the number of selected participants
                    ArrayList<String> selectedParticipants = getSharedPreferencesData();
                    int participantCount = selectedParticipants.size();
                    String message = participantCount + (participantCount == 1 ? " participant selected" : " participants selected");
                    etParticipants.setText(message);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_meeting);

        // Apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack);
        etTitle = findViewById(R.id.etMeetingTitle);
        etDate = findViewById(R.id.etMeetingDate);
        etTime = findViewById(R.id.etMeetingTime);
        etParticipants = findViewById(R.id.etAddParticipants);
        btnAddMeeting = findViewById(R.id.btnAddMeeting);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE); // Hide the progress bar from the user until the login button is pressed

        // Set up listeners
        etDate.setOnClickListener(v -> showDatePickerDialog());
        etTime.setOnClickListener(v -> showTimePickerDialog());
        etParticipants.setOnClickListener(v -> openSelectParticipantsActivity());

        btnBack.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), MainActivity.class)));
        btnAddMeeting.setOnClickListener(v -> addMeetingIfValid());

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

    // Launch the SelectParticipants activity for result
    private void openSelectParticipantsActivity() {
        Intent intent = new Intent(RegisterMeeting.this, SelectParticipants.class);
        selectParticipantsLauncher.launch(intent);
    }

    // Validate input and add meeting if valid
    private void addMeetingIfValid() {
        // Retrieve input values
        String title = etTitle.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();

        // Check if any field is empty
        if (isInputValid(title, date, time)) {
            addMeetingToFirestore(title, date, time);
            progressBar.setVisibility(View.VISIBLE);
            btnAddMeeting.setVisibility(View.INVISIBLE);
        } else {
            Toast.makeText(RegisterMeeting.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        }
    }

    // Validate input fields
    private boolean isInputValid(String title, String date, String time) {
        return !TextUtils.isEmpty(title) && !TextUtils.isEmpty(date) && !TextUtils.isEmpty(time);
    }

    // Add meeting data to Firestore
    private void addMeetingToFirestore(String title, String date, String time) {
        // Retrieve selected participants from SharedPreferences
        ArrayList<String> selectedParticipants = getSharedPreferencesData();

        if (!selectedParticipants.isEmpty()) {
            // Get the current user's email (organiser)
            String organiserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

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

            // Add meeting data to Firestore
            FirebaseFirestore fStore = FirebaseFirestore.getInstance();
            fStore.collection("meetings")
                    .add(meetingData)
                    .addOnSuccessListener(documentReference -> {
                        // Show success message and clear fields
                        Toast.makeText(RegisterMeeting.this, "Meeting added successfully", Toast.LENGTH_SHORT).show();
                        clearInputFields();
                        clearSharedPreferencesData();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    })
                    .addOnFailureListener(e -> {
                        // Show error message
                        Toast.makeText(RegisterMeeting.this, "Failed to add meeting", Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Error adding document", e);
                    });
        } else {
            Toast.makeText(RegisterMeeting.this, "Please select meeting participants", Toast.LENGTH_SHORT).show();
        }
    }

    // Clear input fields
    private void clearInputFields() {
        etTitle.setText("");
        etDate.setText("");
        etTime.setText("");
        etParticipants.setText("");
    }

    // Clear SharedPreferences data on activity destruction
    @Override
    public void onDestroy() {
        clearSharedPreferencesData();
        super.onDestroy();
    }

    // Clear all data from SharedPreferences
    private void clearSharedPreferencesData() {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear(); // Clear all data
        editor.apply();
    }

    // Retrieve selected participants from SharedPreferences
    private ArrayList<String> getSharedPreferencesData() {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        return new ArrayList<>(preferences.getStringSet(SELECTED_PARTICIPANTS_KEY, new HashSet<>()));
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
                    // Check if the selected date is not in the past
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, month1, dayOfMonth1);
                    Calendar currentDate = Calendar.getInstance();
                    if (selectedDate.before(currentDate)) {
                        // Date is in the past, show a message
                        Toast.makeText(RegisterMeeting.this, "Date is not valid", Toast.LENGTH_SHORT).show();
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
}