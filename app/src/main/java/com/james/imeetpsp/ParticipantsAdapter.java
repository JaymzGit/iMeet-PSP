package com.james.imeetpsp;

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
    private List<Participant> participants;
    private List<String> selectedParticipants; // List to hold selected participant emails
    private OnParticipantClickListener listener;

    public interface OnParticipantClickListener {
        void onParticipantClick(int position, boolean isChecked);
    }

    public ParticipantsAdapter(List<Participant> participants, OnParticipantClickListener listener) {
        this.participants = participants;
        this.listener = listener;
        this.selectedParticipants = new ArrayList<>(); // Initialize the list
    }

    public List<String> getSelectedParticipants() {
        return selectedParticipants;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        Participant participant = participants.get(position);
        holder.bind(participant, selectedParticipants.contains(participant.getEmail()));
    }

    public void filterList(List<Participant> filteredParticipants) {
        participants.clear();
        participants.addAll(filteredParticipants);
        notifyDataSetChanged();
    }

    public void updateParticipants(List<Participant> newParticipants, List<String> selectedParticipants) {
        this.participants.clear();
        this.participants.addAll(newParticipants);
        this.selectedParticipants = selectedParticipants;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView textViewParticipant, textViewEmail;
        ImageView imageViewProfilePicture;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewParticipant = itemView.findViewById(R.id.textViewParticipantName);
            textViewEmail = itemView.findViewById(R.id.textViewParticipantEmail);
            imageViewProfilePicture = itemView.findViewById(R.id.imageViewProfilePicture);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Participant participant = participants.get(position);
                        boolean isChecked = !selectedParticipants.contains(participant.getEmail());
                        listener.onParticipantClick(position, isChecked);
                        if (isChecked) {
                            selectedParticipants.add(participant.getEmail()); // Add selected participant email to the list
                            itemView.setBackgroundResource(R.drawable.button_rounded_bg_selected);
                        } else {
                            selectedParticipants.remove(participant.getEmail()); // Remove unselected participant email from the list
                            itemView.setBackgroundResource(R.drawable.button_rounded_bg);
                        }
                    }
                }
            });
        }

        public void bind(Participant participant, boolean isChecked) {
            textViewParticipant.setText(" " + participant.getName());
            textViewEmail.setText(" " + participant.getEmail());

            // Load profile picture using Glide
            Glide.with(itemView.getContext())
                    .load(participant.getProfilePictureUrl())
                    .placeholder(R.drawable.default_image)
                    .into(imageViewProfilePicture);

// Set background based on selection state using XML drawable files
            itemView.setBackgroundResource(isChecked ? R.drawable.button_rounded_bg_selected : R.drawable.button_rounded_bg);
        }
    }
}