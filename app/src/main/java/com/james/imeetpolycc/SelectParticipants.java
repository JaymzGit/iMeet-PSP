package com.james.imeetpolycc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectParticipants extends AppCompatActivity implements ParticipantsAdapter.OnParticipantClickListener {

    // Firebase instance
    private FirebaseFirestore fStore;

    // UI elements
    private ImageButton btnBack;
    private EditText etSearch;
    private RecyclerView rvParticipants;
    private TextView tvTotalParticipants;

    // Adapter and data
    private ParticipantsAdapter participantsAdapter;
    private List<Participant> allParticipants;
    private ArrayList<String> selectedParticipants;

    // Counter for selected participants
    private int selectedCount = 0;

    // SharedPreferences key
    private static final String SELECTED_PARTICIPANTS_KEY = "selectedParticipants";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_participants);

        // Apply system-wide dark mode setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize Firebase instance and UI elements
        fStore = FirebaseFirestore.getInstance();
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        rvParticipants = findViewById(R.id.rvParticipants);
        tvTotalParticipants = findViewById(R.id.tvTotalParticipants);

        // Initialize data structures and adapter
        allParticipants = new ArrayList<>();
        selectedParticipants = getSharedPreferencesData(); // Load selected participants from SharedPreferences

        // Set the selected count based on the already selected participants
        selectedCount = selectedParticipants.size();
        updateSelectedParticipantsCount(); // Update the count initially

        participantsAdapter = new ParticipantsAdapter(new ArrayList<>(), this);
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        rvParticipants.setAdapter(participantsAdapter);

        // Fetch participants from Firestore
        fetchParticipantsFromFirestore();

        // Set up back button functionality
        btnBack.setOnClickListener(v -> {
            saveSharedPreferencesData(selectedParticipants);
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            super.onBackPressed(); // Call the default behavior for onBackPressed
        });

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

    // Fetch participants data from Firestore
    private void fetchParticipantsFromFirestore() {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        fStore.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot queryDocumentSnapshots = task.getResult();
                        if (queryDocumentSnapshots != null) {
                            allParticipants.clear();
                            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                String fname = document.getString("fname");
                                String email = document.getString("email");
                                String imageUrl = document.getString("imageUrl");
                                Boolean attendanceField = document.getBoolean("attendance");
                                boolean attendance = attendanceField != null && attendanceField;
                                String reason = document.getString("reason");

                                // Exclude the current user from being added to the list
                                if (!email.equals(currentUserEmail)) {
                                    Participant participant = new Participant(fname, email, imageUrl, attendance, reason);
                                    allParticipants.add(participant);
                                }
                            }
                            participantsAdapter.updateParticipants(allParticipants, selectedParticipants);

                            // Update the total participants count after fetching the data
                            updateSelectedParticipantsCount();
                        }
                    } else {
                        Toast.makeText(SelectParticipants.this, "Error fetching participants: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onParticipantClick(int position, boolean isChecked) {
        Participant participant = allParticipants.get(position);
        if (isChecked) {
            selectedParticipants.add(participant.getEmail());
            selectedCount++; // Increment count
        } else {
            selectedParticipants.remove(participant.getEmail());
            selectedCount--; // Decrement count
        }

        // Save the updated list of selected participants to SharedPreferences
        saveSharedPreferencesData(selectedParticipants);

        // Update TextView with the new counts
        updateSelectedParticipantsCount();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSharedPreferencesData(selectedParticipants);
    }

    @Override
    public void onBackPressed() {
        saveSharedPreferencesData(selectedParticipants);
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed(); // Call the default behavior for onBackPressed
    }

    // Save selected participants to SharedPreferences
    private void saveSharedPreferencesData(ArrayList<String> selectedParticipants) {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> selectedParticipantsSet = new HashSet<>(selectedParticipants);
        editor.putStringSet(SELECTED_PARTICIPANTS_KEY, selectedParticipantsSet);
        editor.apply();
    }

    // Load selected participants from SharedPreferences
    private ArrayList<String> getSharedPreferencesData() {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        Set<String> selectedParticipantsSet = preferences.getStringSet(SELECTED_PARTICIPANTS_KEY, new HashSet<>());
        return new ArrayList<>(selectedParticipantsSet);
    }

    // Filter participants based on search input
    private void filterParticipants(String searchText) {
        ArrayList<Participant> filteredParticipants = new ArrayList<>();
        for (Participant participant : allParticipants) {
            if (participant.getName().toLowerCase().contains(searchText.toLowerCase())) {
                filteredParticipants.add(participant);
            }
        }
        participantsAdapter.filterList(filteredParticipants);
    }

    // Update the TextView with the current selected participants count
    private void updateSelectedParticipantsCount() {
        String text = "Total Participants - " + allParticipants.size() + " | Selected Participants - " + selectedCount;
        tvTotalParticipants.setText(text);
    }
}