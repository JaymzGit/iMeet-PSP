package com.james.imeetpolycc;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    // Firebase instances
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    // UI elements
    private ImageButton btnBack;
    private ImageView ivProfilePicture;
    private TextView tvUserName, tvUserEmail, tvUserPhone, tvNoMeetings;
    private Button btnEditProfile, btnDeleteAccount, btnLogout;
    private RecyclerView rvPastMeetings;

    // Data variables
    private ArrayList<Meeting> pastMeetingsList;
    private MeetingAdapter meetingAdapter;
    private String userID, imageURL;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply system-wide dark mode setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_user_profile);

        // Initialize Firebase instances
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnLogout = findViewById(R.id.btnLogout);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);

        tvNoMeetings = findViewById(R.id.tvNoMeetings);
        rvPastMeetings = findViewById(R.id.rvPastMeetings);
        pastMeetingsList = new ArrayList<>();
        meetingAdapter = new MeetingAdapter(pastMeetingsList);
        rvPastMeetings.setLayoutManager(new LinearLayoutManager(this));
        rvPastMeetings.setAdapter(meetingAdapter);

        // Check if user is logged in, otherwise redirect to Login screen
        if (fAuth.getCurrentUser() != null) {
            userID = fAuth.getCurrentUser().getUid();
            currentUserEmail = fAuth.getCurrentUser().getEmail();
        } else {
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
            return;
        }

        // Set up button listeners
        btnBack.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), StartActivity.class)));
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), EditProfile.class)));
        btnDeleteAccount.setOnClickListener(v -> {
            // Inflate the custom layout for the confirmation dialog
            View customDialogView = getLayoutInflater().inflate(R.layout.delete_account_dialog_box, null);

            // Find views in the custom layout
            TextView dialogTitle = customDialogView.findViewById(R.id.dialogTitle);
            TextView dialogMessage = customDialogView.findViewById(R.id.dialogMessage);
            TextView buttonNo = customDialogView.findViewById(R.id.buttonNo);
            TextView buttonYes = customDialogView.findViewById(R.id.buttonYes);

            androidx.appcompat.app.AlertDialog customDialog = new androidx.appcompat.app.AlertDialog.Builder(UserProfile.this)
                    .setView(customDialogView)
                    .create();

            customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: to make it transparent
            customDialog.show();
            customDialog.getWindow().setLayout(1000, 600); // Set your desired width and height here

            // Set dialog title and message
            dialogTitle.setText("Delete Account?");
            dialogMessage.setText("This action cannot be undone");

            // Set button listeners
            buttonNo.setOnClickListener(view -> customDialog.dismiss());

            buttonYes.setOnClickListener(view -> {
                FirebaseUser user = fAuth.getCurrentUser();
                if (user != null) {
                    String currentUserEmail = user.getEmail();
                    CollectionReference meetingsRef = fStore.collection("meetings");

                    meetingsRef.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot meetingDoc : task.getResult()) {
                                ArrayList<Map<String, Object>> participants = (ArrayList<Map<String, Object>>) meetingDoc.get("participants");

                                if (participants != null) {
                                    for (int i = 0; i < participants.size(); i++) {
                                        Map<String, Object> participant = participants.get(i);
                                        String email = (String) participant.get("email");

                                        if (email != null && email.equals(currentUserEmail)) {
                                            participants.remove(i); // Remove participant
                                            meetingDoc.getReference().update("participants", participants)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Participant successfully deleted
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(UserProfile.this, "Failed to delete participant data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                            break; // Exit loop once the participant is found and removed
                                        }
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(UserProfile.this, "Failed to retrieve meetings data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Proceed with deleting user data in the "users" collection and Firebase Auth
                    DocumentReference userDocRef = fStore.collection("users").document(user.getUid());
                    userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String imageUrl = documentSnapshot.getString("imageUrl");

                            userDocRef.delete().addOnSuccessListener(aVoid -> {
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                                    imageRef.delete().addOnSuccessListener(aVoid1 -> {
                                        // Image deleted successfully
                                    }).addOnFailureListener(e -> {
                                        Toast.makeText(UserProfile.this, "Failed to delete user profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }).addOnFailureListener(e -> {
                                Toast.makeText(UserProfile.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });

                    user.delete().addOnSuccessListener(aVoid -> {
                        Toast.makeText(UserProfile.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                        fAuth.signOut();
                        startActivity(new Intent(getApplicationContext(), StartActivity.class));
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(UserProfile.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });

            // Show the dialog
            customDialog.show();
        });

        btnLogout.setOnClickListener(v -> {
            View customDialogView = getLayoutInflater().inflate(R.layout.logout_dialog_box, null);

            TextView dialogTitle = customDialogView.findViewById(R.id.dialogTitle);
            TextView dialogMessage = customDialogView.findViewById(R.id.dialogMessage);
            TextView buttonNo = customDialogView.findViewById(R.id.buttonNo);
            TextView buttonYes = customDialogView.findViewById(R.id.buttonYes);

            androidx.appcompat.app.AlertDialog customDialog = new androidx.appcompat.app.AlertDialog.Builder(UserProfile.this)
                    .setView(customDialogView)
                    .create();

            customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: to make it transparent
            customDialog.show();
            customDialog.getWindow().setLayout(1000, 600); // Set your desired width and height here

            // Set button listeners
            buttonNo.setOnClickListener(view -> customDialog.dismiss());
            buttonYes.setOnClickListener(view -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), StartActivity.class);
                startActivity(intent);
                finish();
            });
        });

        // Load user profile information
        DocumentReference documentReference = fStore.collection("users").document(userID);
        documentReference.addSnapshotListener(this, (value, error) -> {
            if (error != null) {
                Log.e("User Settings", "Error fetching document: ", error);
                return;
            }

            if (value != null && value.exists()) {
                String nameValue = value.getString("fname");
                String emailValue = value.getString("email");
                String phoneValue = value.getString("phoneNo");
                imageURL = value.getString("imageUrl");

                Glide.with(getApplication())
                        .load(imageURL)
                        .placeholder(R.drawable.default_user_image)
                        .error(R.drawable.default_user_image)
                        .into(ivProfilePicture);

                tvUserName.setText(nameValue);
                tvUserEmail.setText(emailValue);
                tvUserPhone.setText(phoneValue);
            } else {
                Log.d("User Settings", "Document does not exist or is null");
            }
        });

        // Fetch and display past meetings
        fStore.collection("meetings")
                .whereEqualTo("status", "Ended")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Error fetching documents: ", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        pastMeetingsList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Meeting meeting = document.toObject(Meeting.class);

                            if (meeting.getOrganiser() != null && meeting.getOrganiser().equals(currentUserEmail)) {
                                pastMeetingsList.add(meeting);
                            } else if (meeting.getParticipants() != null) {
                                for (Participant participant : meeting.getParticipants()) {
                                    if (participant.getEmail().equals(currentUserEmail)) {
                                        pastMeetingsList.add(meeting);
                                        break;
                                    }
                                }
                            }
                        }
                        meetingAdapter.notifyDataSetChanged();

                        // Update visibility of the "No Meetings" text
                        if (pastMeetingsList.isEmpty()) {
                            tvNoMeetings.setVisibility(View.VISIBLE);
                            rvPastMeetings.setVisibility(View.GONE);
                        } else {
                            tvNoMeetings.setVisibility(View.GONE);
                            rvPastMeetings.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d("Firestore", "No documents found.");
                        tvNoMeetings.setVisibility(View.VISIBLE);
                        rvPastMeetings.setVisibility(View.GONE);
                    }
                });

        // Handle back press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish(); // Optionally close the current activity
            }
        });
    }
}