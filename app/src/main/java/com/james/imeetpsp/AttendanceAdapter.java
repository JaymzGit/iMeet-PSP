package com.james.imeetpsp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
    private List<Participant> participants;
    private List<String> selectedParticipants; // List to hold selected participant emails
    private AttendanceListener listener;

    public interface AttendanceListener {
        void onAttendanceChanged(int position, boolean isChecked);
    }

    public AttendanceAdapter(List<Participant> participants, AttendanceListener listener) {
        this.participants = participants;
        this.listener = listener;
        this.selectedParticipants = new ArrayList<>(); // Initialize the list
    }

    public List<String> getSelectedParticipants() {
        return selectedParticipants;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Participant participant = participants.get(position);
        holder.bind(participant, selectedParticipants.contains(participant.getEmail()));
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView textViewParticipant, textViewEmail;
        ImageView imageViewProfilePicture;
        CheckBox checkBoxAttendance;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewParticipant = itemView.findViewById(R.id.textViewParticipantName);
            textViewEmail = itemView.findViewById(R.id.textViewParticipantEmail);
            imageViewProfilePicture = itemView.findViewById(R.id.imageViewProfilePicture);
            checkBoxAttendance = itemView.findViewById(R.id.checkBoxAttendance);

            checkBoxAttendance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onAttendanceChanged(position, isChecked);
                        if (isChecked) {
                            selectedParticipants.add(participants.get(position).getEmail()); // Add selected participant email to the list
                        } else {
                            selectedParticipants.remove(participants.get(position).getEmail()); // Remove unselected participant email from the list
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

            checkBoxAttendance.setChecked(isChecked);
        }
    }
}