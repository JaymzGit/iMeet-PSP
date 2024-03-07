package com.james.imeetpsp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class MeetingDetails extends AppCompatActivity {

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
                String title = extras.getString("title");
                String date = extras.getString("date");
                String time = extras.getString("time");
                String organiser = extras.getString("organizer");
                String status = extras.getString("status");
//                String participants = extras.getString("participants");

                // Now you have the data, you can set it to your UI elements
                // For example:
                TextView tvTitle = findViewById(R.id.textViewTitle);
                TextView tvDate = findViewById(R.id.textViewDate);
                TextView tvTime = findViewById(R.id.textViewTime);
                ImageView ivOrganiserImage = findViewById(R.id.imageViewOrganiser);
                TextView tvOrganiserName = findViewById(R.id.textViewOrganiserName);
                TextView tvOrganiserEmail = findViewById(R.id.textViewOrganiserEmail);
                TextView tvStatus = findViewById(R.id.textViewStatus);
                Button btnViewParticipants = findViewById(R.id.btnViewParticipants);
                View areYouAttendingSection = findViewById(R.id.relativeLayoutAttending);

                // Get the current user's email
                String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                // Check if the current user is the organizer
                if (currentUserEmail != null && currentUserEmail.equals(organiser)) {
                    // Hide the "Are you attending?" section
                    areYouAttendingSection.setVisibility(View.GONE);

                    // Show button to view participants
                    btnViewParticipants.setVisibility(View.VISIBLE);
                    btnViewParticipants.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Start the activity to view participants
                            // Replace PlaceholderActivity.class with the actual activity class to view participants
                            startActivity(new Intent(MeetingDetails.this, MainActivity.class));
                        }
                    });
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .whereEqualTo("email", organiser)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                if (error != null) {
                                    // Handle errors here
                                    // For example:
                                    tvOrganiserName.setText("Error: " + error.getMessage());
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
                                        tvOrganiserName.setText(fullName);
                                        // Set the organizer's email to the TextView
                                        tvOrganiserEmail.setText(email);

                                        // Check if imageUrl and context are not null before loading image
                                        if (imageUrl != null && MeetingDetails.this != null) {
                                            // Load image using Glide
                                            Glide.with(MeetingDetails.this)
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.default_image)
                                                    .error(R.drawable.default_image)
                                                    .into(ivOrganiserImage);
                                        } else {
                                            ivOrganiserImage.setImageResource(R.drawable.default_image);
                                        }
                                    }
                                } else {
                                    // Handle case where no matching document is found
                                    // For example:
                                    tvOrganiserName.setText("Organiser not found");
                                    tvOrganiserEmail.setText("");
                                    ivOrganiserImage.setImageResource(R.drawable.default_image);
                                }
                            }
                        });

                tvTitle.setText(title);
                tvDate.setText(" Date: " + date);
                tvTime.setText(" Time: " + time);
                tvStatus.setText(" Status: " + status);

                // Set other TextViews similarly
            }
        }
    }

}