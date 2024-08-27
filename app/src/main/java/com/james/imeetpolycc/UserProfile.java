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
            // Confirm deletion
            new androidx.appcompat.app.AlertDialog.Builder(UserProfile.this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account? We cannot restore accounts once they have been deleted.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Sign out the user
                        FirebaseUser user = fAuth.getCurrentUser();
                        if (user != null) {
                            String currentUserEmail = user.getEmail();

                            // Reference to Firestore "meetings" collection
                            CollectionReference meetingsRef = fStore.collection("meetings");

                            meetingsRef.get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot meetingDoc : task.getResult()) {
                                        // Reference to participants subcollection
                                        CollectionReference participantsRef = meetingDoc.getReference().collection("participants");

                                        participantsRef.whereEqualTo("email", currentUserEmail)
                                                .get().addOnCompleteListener(participantTask -> {
                                                    if (participantTask.isSuccessful()) {
                                                        for (QueryDocumentSnapshot participantDoc : participantTask.getResult()) {
                                                            participantDoc.getReference().delete()
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        // Participant successfully deleted
                                                                    }).addOnFailureListener(e -> {
                                                                        Toast.makeText(UserProfile.this, "Failed to delete participant data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    Toast.makeText(UserProfile.this, "Failed to retrieve meetings data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                            // Reference to Firestore "users" collection to delete the user document
                            DocumentReference userDocRef = fStore.collection("users").document(user.getUid());
                            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String imageUrl = documentSnapshot.getString("imageUrl"); // Get image URL

                                    // Delete user document from Firestore
                                    userDocRef.delete().addOnSuccessListener(aVoid -> {
                                        // Delete the image from Firebase Storage if it exists
                                        if (imageUrl != null && !imageUrl.isEmpty()) {
                                            // Extract the storage reference from the image URL
                                            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                                            imageRef.delete().addOnSuccessListener(aVoid1 -> {
                                            }).addOnFailureListener(e -> {
                                                Toast.makeText(UserProfile.this, "Failed to delete user profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                        }

                                    }).addOnFailureListener(e -> {
                                        Toast.makeText(UserProfile.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });

                            // Proceed to delete the user from FirebaseAuth
                            user.delete().addOnSuccessListener(aVoid -> {
                                Toast.makeText(UserProfile.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                fAuth.signOut();
                                startActivity(new Intent(getApplicationContext(), StartActivity.class));
                                finish();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(UserProfile.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // Dismiss the dialog
                        dialog.dismiss();
                    })
                    .create()
                    .show();
        });

        btnLogout.setOnClickListener(v -> {
            // Confirm deletion
            new androidx.appcompat.app.AlertDialog.Builder(UserProfile.this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to logout?")

                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Sign out the user
                        FirebaseAuth.getInstance().signOut();

                        // Navigate to StartActivity and finish the current activity
                        Intent intent = new Intent(getApplicationContext(), StartActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        // Dismiss the dialog
                        dialog.dismiss();
                    })
                    .create()
                    .show();
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