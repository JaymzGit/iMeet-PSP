package com.james.imeetpsp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class MeetingDetails extends AppCompatActivity {
    String title, date, time, organizer, status;
    private ArrayList<String> participants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_details);

        // Apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Retrieve data from the intent
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                title = extras.getString("title");
                date = extras.getString("date");
                time = extras.getString("time");
                organizer = extras.getString("organizer");
                status = extras.getString("status");
                participants = extras.getStringArrayList("participants");

                // Now you have the data, you can set it to your UI elements
                // For example:
                TextView tvTitle = findViewById(R.id.textViewTitle);
                TextView tvDate = findViewById(R.id.textViewDate);
                TextView tvTime = findViewById(R.id.textViewTime);
                ImageView ivOrganizerImage = findViewById(R.id.imageViewOrganizer);
                TextView tvOrganizerName = findViewById(R.id.textViewOrganizerName);
                TextView tvOrganizerEmail = findViewById(R.id.textViewOrganizerEmail);
                TextView tvStatus = findViewById(R.id.textViewStatus);
                Button btnViewParticipants = findViewById(R.id.btnViewParticipants);

                // Set data to UI elements
                tvTitle.setText(title);
                tvDate.setText(" Date: " + date);
                tvTime.setText(" Time: " + time);
                tvStatus.setText(" Status: " + status);

                // Store a reference to the activity context
                final MeetingDetails activityContext = this;

                // Load organizer details
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .whereEqualTo("email", organizer)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                if (error != null) {
                                    // Handle errors here
                                    // For example:
                                    tvOrganizerName.setText("Error: " + error.getMessage());
                                    return;
                                }

                                if (value != null && !value.isEmpty()) {
                                    for (DocumentSnapshot document : value.getDocuments()) {
                                        // Retrieve the full name from the document
                                        String fullName = document.getString("fname");
                                        // Retrieve the email from the document
                                        String email = document.getString("email");
                                        // Retrieve the image URL from the document
                                        String imageUrl = document.getString("imageUrl");

                                        // Set the organizer's name to the TextView
                                        tvOrganizerName.setText(fullName);
                                        // Set the organizer's email to the TextView
                                        tvOrganizerEmail.setText(email);

                                        // Check if imageUrl and activityContext are not null before loading image
                                        if (imageUrl != null && activityContext != null) {
                                            // Load image using Glide with the stored activity context
                                            Glide.with(activityContext)
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.default_image)
                                                    .error(R.drawable.default_image)
                                                    .into(ivOrganizerImage);
                                        } else {
                                            // If imageUrl or activityContext is null, set the default image
                                            ivOrganizerImage.setImageResource(R.drawable.default_image);
                                        }
                                    }
                                } else {
                                    // Handle case where no matching document is found
                                    // For example:
                                    tvOrganizerName.setText("Organizer not found");
                                    tvOrganizerEmail.setText("");
                                    ivOrganizerImage.setImageResource(R.drawable.default_image);
                                }
                            }
                        });

                // Check if the current user is the organizer
                String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                if (currentUserEmail != null && currentUserEmail.equals(organizer)) {
                    // If the current user is the organizer, hide attendance section
                    findViewById(R.id.attendanceCardView).setVisibility(View.GONE);

                    // Show button to view participants
                    btnViewParticipants.setVisibility(View.VISIBLE);
                    btnViewParticipants.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Handle button click to view participants
                            Intent intent = new Intent(MeetingDetails.this, EditAttendance.class);
                            intent.putStringArrayListExtra("participants", participants);
                            startActivity(intent);
                        }
                    });
                }

                // Initialize attendance section views
                RadioGroup radioGroupAttendance = findViewById(R.id.radioGroupAttendance);
                RadioButton radioButtonYes = findViewById(R.id.radioButtonYes);
                RadioButton radioButtonNo = findViewById(R.id.radioButtonNo);
                Spinner spinnerReason = findViewById(R.id.spinnerReason);

                // Add listener for radio group
                radioGroupAttendance.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if (checkedId == R.id.radioButtonNo) {
                            // Show spinner when "No" is selected
                            spinnerReason.setVisibility(View.VISIBLE);
                        } else {
                            // Hide spinner for other options
                            spinnerReason.setVisibility(View.GONE);
                        }
                    }
                });

                // Populate spinner with reasons
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(MeetingDetails.this,
                        R.array.reasons_array, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerReason.setAdapter(adapter);

                // Set listener for spinner item selection
                spinnerReason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        TextView textView = (TextView) view;
                        // Set the text color to white
                        textView.setTextColor(Color.WHITE);
                        String reason = parent.getItemAtPosition(position).toString();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            }
        }
    }
}