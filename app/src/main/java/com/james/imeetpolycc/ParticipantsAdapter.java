package com.james.imeetpolycc;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder> {

    // List of participants and selected participants
    private List<Participant> participants;
    private List<String> selectedParticipants;
    private OnParticipantClickListener listener;

    // Interface for handling participant clicks
    public interface OnParticipantClickListener {
        void onParticipantClick(int position, boolean isChecked);
    }

    // Constructor
    public ParticipantsAdapter(List<Participant> participants, OnParticipantClickListener listener) {
        this.participants = participants;
        this.listener = listener;
        this.selectedParticipants = new ArrayList<>(); // Initialize the list
    }

    // Get the list of selected participants
    public List<String> getSelectedParticipants() {
        return selectedParticipants;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        // Bind participant data to view holder
        Participant participant = participants.get(position);
        holder.bind(participant, selectedParticipants.contains(participant.getEmail()));
    }

    // Update the list of participants and selected participants
    public void updateParticipants(List<Participant> newParticipants, List<String> selectedParticipants) {
        this.participants.clear();
        this.participants.addAll(newParticipants);
        this.selectedParticipants = selectedParticipants;
        notifyDataSetChanged();
    }

    // Filter participants based on criteria
    public void filterList(List<Participant> filteredParticipants) {
        participants.clear();
        participants.addAll(filteredParticipants);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        // UI elements for participant item
        private TextView textViewParticipant, textViewEmail;
        private ImageView imageViewProfilePicture;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewParticipant = itemView.findViewById(R.id.textViewParticipantName);
            textViewEmail = itemView.findViewById(R.id.textViewParticipantEmail);
            imageViewProfilePicture = itemView.findViewById(R.id.imageViewProfilePicture);

            // Handle item click
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Participant participant = participants.get(position);
                    boolean isChecked = !selectedParticipants.contains(participant.getEmail());
                    listener.onParticipantClick(position, isChecked);

                    if (isChecked) {
                        selectedParticipants.add(participant.getEmail()); // Add selected participant
                        itemView.setBackgroundResource(R.drawable.button_selected);
                    } else {
                        selectedParticipants.remove(participant.getEmail()); // Remove unselected participant
                        itemView.setBackgroundResource(R.drawable.button_outline);
                    }
                }
            });
        }

        // Bind participant data to UI elements
        public void bind(Participant participant, boolean isChecked) {
            textViewParticipant.setText(" " + participant.getName());
            textViewEmail.setText(" " + participant.getEmail());

            // Load profile picture using Glide
            Glide.with(itemView.getContext())
                    .load(participant.getProfilePictureUrl())
                    .placeholder(R.drawable.default_user_image)
                    .into(imageViewProfilePicture);

            // Set background based on selection state
            itemView.setBackgroundResource(isChecked ? R.drawable.button_selected : R.drawable.button_outline);
        }
    }
}