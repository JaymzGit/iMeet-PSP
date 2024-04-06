package com.james.imeetpsp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EditAttendance extends AppCompatActivity implements AttendanceAdapter.AttendanceListener {

    private static final String TAG = "EditAttendance";
    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_attendance);

        // Retrieve the list of participants from the intent's bundle
        Bundle bundle = getIntent().getExtras();
        ArrayList<String> participants = bundle.getStringArrayList("participants");

        // Set up the RecyclerView to display participants
        recyclerView = findViewById(R.id.recyclerViewParticipantAttendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create and set the adapter
        adapter = new AttendanceAdapter(createParticipantList(participants), this);
        recyclerView.setAdapter(adapter);
    }

    private List<Participant> createParticipantList(ArrayList<String> participantEmails) {
        List<Participant> participants = new ArrayList<>();
        for (String email : participantEmails) {
            // Assuming you have a method to retrieve participant details from email
            getParticipantByEmail(email, new OnParticipantRetrievedListener() {
                @Override
                public void onParticipantRetrieved(Participant participant) {
                    if (participant != null) {
                        participants.add(participant);
                        adapter.notifyDataSetChanged(); // Notify adapter that data set has changed
                    }
                }
            });
        }
        return participants;
    }

    // Implement a method to retrieve participant details by email
    private void getParticipantByEmail(String email, OnParticipantRetrievedListener listener) {
        // Implement your logic to retrieve participant details using their email from Firestore
        // Here's a sample implementation assuming you have a "users" collection in Firestore
        FirebaseFirestore.getInstance().collection("users").whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String name = document.getString("fname"); // Assuming 'fname' is the field for participant's name
                            String imageUrl = document.getString("imageUrl"); // Assuming 'imageUrl' is the field for participant's profile picture
                            Participant participant = new Participant(name, email, imageUrl);
                            listener.onParticipantRetrieved(participant);
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                        listener.onParticipantRetrieved(null);
                    }
                });
    }

    @Override
    public void onAttendanceChanged(int position, boolean isChecked) {
        // Handle attendance change
        // You can update the participant object in the list or perform any other action based on the change
    }

    // Interface for retrieving participant details asynchronously
    interface OnParticipantRetrievedListener {
        void onParticipantRetrieved(Participant participant);
    }
}