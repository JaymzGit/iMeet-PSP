package com.james.imeetpsp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectParticipants extends AppCompatActivity implements ParticipantsAdapter.OnParticipantClickListener {
    private RecyclerView recyclerViewParticipants;
    private ParticipantsAdapter participantsAdapter;
    private List<Participant> allParticipants;
    private ArrayList<String> selectedParticipants;
    private FirebaseFirestore firestore;
    private EditText editTextSearch;

    private static final String SELECTED_PARTICIPANTS_KEY = "selectedParticipants";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_participants);

        //apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        recyclerViewParticipants = findViewById(R.id.recyclerViewParticipants);
        editTextSearch = findViewById(R.id.editTextSearch);

        allParticipants = new ArrayList<>();
        participantsAdapter = new ParticipantsAdapter(new ArrayList<>(), this);
        selectedParticipants = getSharedPreferencesData(); // Load selected participants from SharedPreferences
        recyclerViewParticipants.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewParticipants.setAdapter(participantsAdapter);

        firestore = FirebaseFirestore.getInstance();

        fetchParticipantsFromFirestore(); // Fetch participants from Firestore

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterParticipants(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchParticipantsFromFirestore() {
        // Get the email of the current user
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        firestore.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot queryDocumentSnapshots = task.getResult();
                            if (queryDocumentSnapshots != null) {
                                allParticipants.clear();
                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    String fname = document.getString("fname");
                                    String email = document.getString("email");
                                    String imageUrl = document.getString("imageUrl");

                                    // Check if the participant's email is not the same as the current user's email
                                    if (!email.equals(currentUserEmail)) {
                                        Participant participant = new Participant(fname, email, imageUrl);
                                        allParticipants.add(participant);
                                    }
                                }
                                participantsAdapter.updateParticipants(allParticipants, selectedParticipants); // Update adapter with selected participants
                            }
                        } else {
                            Toast.makeText(SelectParticipants.this, "Error fetching participants: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onParticipantClick(int position, boolean isChecked) {
        Participant participant = allParticipants.get(position);
        if (isChecked) {
            selectedParticipants.add(participant.getEmail());
        } else {
            selectedParticipants.remove(participant.getEmail());
        }

        // Save the updated list of selected participants to SharedPreferences
        saveSharedPreferencesData(selectedParticipants);
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
        super.onBackPressed();
    }

    private void saveSharedPreferencesData(ArrayList<String> selectedParticipants) {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> selectedParticipantsSet = new HashSet<>(selectedParticipants);
        editor.putStringSet(SELECTED_PARTICIPANTS_KEY, selectedParticipantsSet);
        editor.apply();
    }


    private ArrayList<String> getSharedPreferencesData() {
        SharedPreferences preferences = getSharedPreferences(SELECTED_PARTICIPANTS_KEY, MODE_PRIVATE);
        Set<String> selectedParticipantsSet = preferences.getStringSet(SELECTED_PARTICIPANTS_KEY, new HashSet<>());
        return new ArrayList<>(selectedParticipantsSet);
    }

    private void filterParticipants(String searchText) {
        ArrayList<Participant> filteredParticipants = new ArrayList<>();
        for (Participant participant : allParticipants) {
            if (participant.getName().toLowerCase().contains(searchText.toLowerCase())) {
                filteredParticipants.add(participant);
            }
        }
        participantsAdapter.filterList(filteredParticipants);
    }
}
