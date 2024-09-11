package com.james.imeetpolycc;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    // Define the interface
    public interface AttendanceListener {
        void onAttendanceChanged(int position, boolean isChecked);
        void onAttendanceCountChanged(int attendingCount, int notAttendingCount);
    }

    private final List<Participant> participants;
    private final AttendanceListener listener;
    private final String currentUserEmail;

    public AttendanceAdapter(AttendanceListener listener, String currentUserEmail) {
        this.participants = new ArrayList<>();
        this.listener = listener;
        this.currentUserEmail = currentUserEmail;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Participant participant = participants.get(position);

        holder.tvParticipantName.setText(participant.getName());
        holder.tvParticipantEmail.setText(participant.getEmail());
        Glide.with(holder.itemView.getContext())
                .load(participant.getProfilePictureUrl())
                .placeholder(R.drawable.default_user_image)
                .into(holder.ivProfilePicture);

        holder.checkBoxAttendance.setChecked(participant.getAttendance());

        holder.checkBoxAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onAttendanceChanged(position, isChecked);
            }
        });
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

    public void updateParticipants(List<Participant> newParticipants) {
        participants.clear();
        participants.addAll(newParticipants);
        notifyDataSetChanged();
    }

    private void updateAttendanceCounts() {
        int attendingCount = 0;
        int notAttendingCount = 0;
        for (Participant participant : participants) {
            if (participant.getAttendance()) {
                attendingCount++;
            } else {
                notAttendingCount++;
            }
        }
        if (listener != null) {
            listener.onAttendanceCountChanged(attendingCount, notAttendingCount);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBoxAttendance;
        private final ImageView ivProfilePicture;
        private final TextView tvParticipantName;
        private final TextView tvParticipantEmail;

        public ViewHolder(View itemView) {
            super(itemView);
            checkBoxAttendance = itemView.findViewById(R.id.checkBoxAttendance);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            tvParticipantEmail = itemView.findViewById(R.id.tvParticipantEmail);
        }
    }
}