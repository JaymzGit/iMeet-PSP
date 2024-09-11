package com.james.imeetpolycc;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditAttendance extends AppCompatActivity implements AttendanceAdapter.AttendanceListener {

    private FirebaseFirestore fStore;
    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;
    private List<Participant> allParticipants;
    private ImageButton btnBack;
    private TextView tvTotalParticipants;
    private EditText etSearch;

    private String meetingID;
    private int attendingCount = 0;
    private int notAttendingCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_attendance);

        // Apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize FirebaseFirestore
        fStore = FirebaseFirestore.getInstance();

        // Retrieve meeting ID from the intent
        meetingID = getIntent().getStringExtra("meetingId");

        // Retrieve organizer email from FirebaseAuth
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack);
        tvTotalParticipants = findViewById(R.id.tvTotalParticipants);
        etSearch = findViewById(R.id.etSearch);

        // Set up the RecyclerView
        recyclerView = findViewById(R.id.rvParticipantAttendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with the current instance as the listener
        adapter = new AttendanceAdapter(this, currentUserEmail);
        recyclerView.setAdapter(adapter);

        allParticipants = new ArrayList<>();

        // Load participants from Firestore
        loadParticipants();

        // Set up back button functionality
        btnBack.setOnClickListener(v -> super.onBackPressed());

        // Set up search filter functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterParticipants(s.toString()); }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadParticipants() {
        if (meetingID == null || meetingID.isEmpty()) {
            Log.e("EditAttendance", "Meeting ID is null or empty.");
            return;
        }

        fStore.collection("meetings")
                .document(meetingID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            List<Participant> participantsList = new ArrayList<>();
                            attendingCount = 0;
                            notAttendingCount = 0;

                            List<Map<String, Object>> participantsArray = (List<Map<String, Object>>) document.get("participants");
                            if (participantsArray != null) {
                                Log.d("EditAttendance", "Participants array size: " + participantsArray.size());

                                List<String> emails = new ArrayList<>();
                                for (Map<String, Object> participantData : participantsArray) {
                                    String email = (String) participantData.get("email");
                                    emails.add(email);
                                }

                                fStore.collection("users")
                                        .whereIn("email", emails)
                                        .get()
                                        .addOnCompleteListener(userTask -> {
                                            if (userTask.isSuccessful()) {
                                                List<DocumentSnapshot> userDocuments = userTask.getResult().getDocuments();
                                                Map<String, DocumentSnapshot> userMap = new HashMap<>();
                                                for (DocumentSnapshot userDoc : userDocuments) {
                                                    userMap.put(userDoc.getString("email"), userDoc);
                                                }

                                                for (Map<String, Object> participantData : participantsArray) {
                                                    String email = (String) participantData.get("email");
                                                    Boolean attendedField = (Boolean) participantData.get("attendance");
                                                    boolean attended = attendedField != null && attendedField;
                                                    String reason = (String) participantData.get("reason");

                                                    DocumentSnapshot userDoc = userMap.get(email);
                                                    String fname = userDoc != null ? userDoc.getString("fname") : "test";
                                                    String imageUrl = userDoc != null ? userDoc.getString("imageUrl") : "";

                                                    Participant participant = new Participant(fname, email, imageUrl, attended, reason);
                                                    participantsList.add(participant);

                                                    // Increment attending/not attending counts
                                                    if (attended) {
                                                        attendingCount++;
                                                    } else {
                                                        notAttendingCount++;
                                                    }
                                                }

                                                allParticipants.clear();
                                                allParticipants.addAll(participantsList);
                                                adapter.updateParticipants(allParticipants);

                                                // Update the total participants text view
                                                tvTotalParticipants.setText(String.format("Total Participants - %d \n Attending - %d | Not Attending - %d",
                                                        allParticipants.size(), attendingCount, notAttendingCount));
                                            } else {
                                                Log.d("EditAttendance", "Error fetching user documents: ", userTask.getException());
                                            }
                                        });
                            } else {
                                Log.d("EditAttendance", "No participants found.");
                            }
                        } else {
                            Log.d("EditAttendance", "No participants found.");
                        }
                    } else {
                        Log.d("EditAttendance", "No such document.");
                    }
                });
    }

    // Filter participants based on search input
    private void filterParticipants(String searchText) {
        ArrayList<Participant> filteredParticipants = new ArrayList<>();
        for (Participant participant : allParticipants) {
            if (participant.getName().toLowerCase().contains(searchText.toLowerCase())) {
                filteredParticipants.add(participant);
            }
        }
        adapter.filterList(filteredParticipants);
    }

    @Override
    public void onAttendanceChanged(int position, boolean isChecked) {
        Participant participant = allParticipants.get(position);
        participant.setAttendance(isChecked);

        // Update count based on checkbox state
        if (isChecked) {
            attendingCount++;
            notAttendingCount--;
        } else {
            attendingCount--;
            notAttendingCount++;
        }

        // Update the UI with the new count
        onAttendanceCountChanged(attendingCount, notAttendingCount);

        // Update Firestore
        fStore.collection("meetings")
                .document(meetingID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();

                        // Update the participants array in Firestore
                        List<Map<String, Object>> participantsArray = (List<Map<String, Object>>) document.get("participants");
                        if (participantsArray != null) {
                            for (Map<String, Object> participantData : participantsArray) {
                                if (participant.getEmail().equals(participantData.get("email"))) {
                                    participantData.put("attendance", isChecked);
                                    break;
                                }
                            }
                            fStore.collection("meetings")
                                    .document(meetingID)
                                    .update("participants", participantsArray)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Log.d("EditAttendance", "Attendance updated successfully");
                                        } else {
                                            Log.d("EditAttendance", "Error updating attendance: ", updateTask.getException());
                                        }
                                    });
                        }
                    } else {
                        Log.d("EditAttendance", "Meeting document not found.");
                    }
                });
    }

    @Override
    public void onAttendanceCountChanged(int attendingCount, int notAttendingCount) {
        this.attendingCount = attendingCount;
        this.notAttendingCount = notAttendingCount;

        tvTotalParticipants.setText(String.format("Total Participants - %d \n Attending - %d | Not Attending - %d",
                allParticipants.size(), attendingCount, notAttendingCount));
    }
}