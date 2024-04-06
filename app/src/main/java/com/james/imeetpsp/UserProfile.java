package com.james.imeetpsp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class UserProfile extends AppCompatActivity {
    TextView fname, email, phone, textViewNoMeetings;
    RecyclerView rvPastMeetings;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    ImageView profileImage;
    String userID, imageURL;
    private ArrayList<Meeting> pastMeetingsList;
    private MeetingAdapter meetingAdapter;
    FirebaseFirestore db;
    String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        db = FirebaseFirestore.getInstance();

        //check if a user is logged in, if yes, send them to MainActivity
        if (fAuth.getCurrentUser() != null) {
            userID = fAuth.getCurrentUser().getUid();
            currentUserEmail = fAuth.getCurrentUser().getEmail();
        }else {
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        }

        textViewNoMeetings = findViewById(R.id.textViewNoMeetings);

        fname = findViewById(R.id.textViewParticipantName);
        email = findViewById(R.id.textViewParticipantEmail);
        phone = findViewById(R.id.textViewPhoneNumber);
        profileImage = findViewById(R.id.imageViewProfile);
        rvPastMeetings = findViewById(R.id.rvPastMeetings);

        pastMeetingsList = new ArrayList<>();
        meetingAdapter = new MeetingAdapter(pastMeetingsList);
        rvPastMeetings.setLayoutManager(new LinearLayoutManager(this));
        rvPastMeetings.setAdapter(meetingAdapter);

        DocumentReference documentReference = fStore.collection("users").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    // Handle any errors that occurred during the snapshot listener
                    Log.e("User Settings", "Error fetching document: ", error);
                    return;
                }

                if (value != null && value.exists()) {
                    // Document exists, retrieve its fields
                    String fnameValue = value.getString("fname");
                    String emailValue = value.getString("email");
                    String phoneValue = value.getString("phoneNo");
                    imageURL = value.getString("imageUrl");

                    Glide.with(getApplication())
                            .load(imageURL)
                            .placeholder(R.drawable.default_image)
                            .error(R.drawable.default_image)
                            .into(profileImage);

                    Log.d("TAG", "Image URL is" + imageURL);

                    // Update UI with the retrieved values
                    fname.setText(fnameValue);
                    email.setText(emailValue);
                    phone.setText(phoneValue);
                } else {
                    // Document doesn't exist or is null, handle accordingly
                    Log.d("User Settings", "Document does not exist or is null");
                }
            }
        });

        // Fetch and display past meetings
        fetchPastMeetings();
    }

    private void fetchPastMeetings() {
        db.collection("meetings")
                .whereEqualTo("status", "Ended") // Only fetch meetings with status "Ended"
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Error fetching documents: ", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        pastMeetingsList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Meeting meeting = document.toObject(Meeting.class);
                            // Check if the current user is the organizer or a participant of the meeting
                            if (meeting.getOrganiser().equals(currentUserEmail)) { //TODO: || meeting.getParticipants().contains(userID))
                                pastMeetingsList.add(meeting);
                            }
                        }
                        meetingAdapter.notifyDataSetChanged();
                        if (pastMeetingsList.isEmpty()) {
                            textViewNoMeetings.setVisibility(View.VISIBLE);
                        } else {
                            textViewNoMeetings.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d("Firestore", "No documents found.");
                        textViewNoMeetings.setVisibility(View.VISIBLE);
                    }
                });
    }

    public void editProfile(View view){
        startActivity(new Intent(getApplicationContext(), EditProfile.class));
    }
    public void logout(View view){
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getApplicationContext(), Login.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }
}
