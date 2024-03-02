package com.james.imeetpsp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterMeeting extends AppCompatActivity {
    EditText etTitle, etDate, etTime, etParticipants;
    Button btnAddMeeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_meeting);

        //apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        etTitle = findViewById(R.id.editTextMeetingTitle);
        etDate = findViewById(R.id.editTextMeetingDate);
        etTime = findViewById(R.id.editTextMeetingTime);
        etParticipants = findViewById(R.id.editTextParticipants);
        btnAddMeeting = findViewById(R.id.buttonAddMeeting);

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        etTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        btnAddMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve input values
                String title = etTitle.getText().toString().trim();
                String date = etDate.getText().toString().trim();
                String time = etTime.getText().toString().trim();
                String participants = etParticipants.getText().toString().trim();

                // Check if any field is empty
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
                    Toast.makeText(RegisterMeeting.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> meetingData = new HashMap<>();
                meetingData.put("title", title);
                meetingData.put("date", date);
                meetingData.put("time", time);
                meetingData.put("status", "Upcoming");

                // Get the current user's email (organiser)
                String organiserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                meetingData.put("organiser", organiserEmail);

                // Add meeting data to Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("meetings")
                        .add(meetingData)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(RegisterMeeting.this, "Meeting added successfully", Toast.LENGTH_SHORT).show();
                            // Clear input fields
                            etTitle.setText("");
                            etDate.setText("");
                            etTime.setText("");
                            etParticipants.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(RegisterMeeting.this, "Failed to add meeting", Toast.LENGTH_SHORT).show();
                            Log.e("Firestore", "Error adding document", e);
                        });
            }
        });
    }

    public void redirectRegister(View view){
        startActivity(new Intent(getApplicationContext(), Register.class));
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    // Method to show DatePickerDialog
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Check if the selected date is not in the past
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        Calendar currentDate = Calendar.getInstance();
                        if (selectedDate.before(currentDate)) {
                            // Date is in the past, show a message or take appropriate action
                            // For example:
                            Toast.makeText(RegisterMeeting.this, "Date is not valid", Toast.LENGTH_SHORT).show();
                        } else {
                            // Do something with the selected date
                            String selectedDateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
                            etDate.setText(selectedDateStr);
                        }
                    }
                },
                year,
                month,
                dayOfMonth
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // Method to show TimePickerDialog
    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // Do something with the selected time
                        String selectedTime = hourOfDay + ":" + minute;
                        etTime.setText(selectedTime);
                    }
                },
                hourOfDay,
                minute,
                false
        );
        timePickerDialog.show();
    }
}