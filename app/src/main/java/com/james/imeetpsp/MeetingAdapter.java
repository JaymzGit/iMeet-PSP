package com.james.imeetpsp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.List;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.MeetingViewHolder> {
    private List<Meeting> meetings;

    public MeetingAdapter(List<Meeting> meetings) {
        this.meetings = meetings;
    }

    @NonNull
    @Override
    public MeetingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meeting, parent, false);
        return new MeetingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingViewHolder holder, int position) {
        Meeting meeting = meetings.get(position);
        holder.bind(meeting);
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    static class MeetingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivOrganiser, imageViewEdit;
        TextView tvMeetingTitle, tvMeetingDate, tvMeetingTime, tvOrganiser, tvDetails, tvStatus;
        Meeting meeting; // Declare the meeting variable

        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        public MeetingViewHolder(@NonNull View itemView) {
            super(itemView);

            imageViewEdit = itemView.findViewById(R.id.imageViewEdit);
            ivOrganiser = itemView.findViewById(R.id.imageViewOrganiser);
            tvMeetingTitle = itemView.findViewById(R.id.textViewMeetingTitle);
            tvMeetingDate = itemView.findViewById(R.id.textViewMeetingDate);
            tvMeetingTime = itemView.findViewById(R.id.textViewMeetingTime);
            tvOrganiser = itemView.findViewById(R.id.textViewMeetingOrganiser);
            tvStatus = itemView.findViewById(R.id.textViewMeetingStatus);
            tvDetails = itemView.findViewById(R.id.textViewDetails);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create an Intent object
                    Intent intent = new Intent(itemView.getContext(), MeetingDetails.class);

                    // Put data into the intent using a Bundle
                    Bundle bundle = new Bundle();
                    bundle.putString("title", meeting.getTitle());
                    bundle.putString("date", meeting.getDate());
                    bundle.putString("time", meeting.getTime());
                    bundle.putString("organizer", meeting.getOrganiser());
                    bundle.putString("status", meeting.getStatus());
//                    bundle.putString("participants", meeting.getParticipants());

                    intent.putExtras(bundle);

                    // Start the activity from the context of the itemView
                    itemView.getContext().startActivity(intent);
                }
            });
        }

        @SuppressLint("SetTextI18n")
        public void bind(Meeting meeting) {
            this.meeting = meeting;
            FirebaseFirestore fStore = FirebaseFirestore.getInstance();

            // Get the organizer's email address from the meeting object
            String organizerEmail = meeting.getOrganiser();

            // Create a final reference to tvOrganiser for use inside the lambda
            final TextView tvOrganiserFinal = tvOrganiser;

            // Query the Firestore 'users' collection to find the document with the matching email
            fStore.collection("users").whereEqualTo("email", organizerEmail)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e("Firestore", "Error fetching documents: ", error);
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            for (QueryDocumentSnapshot document : value) {
                                // Retrieve the user's name from the document
                                String organizerName = document.getString("fname");
                                // Set the organizer's name in your UI using the final reference
                                String limitedOrganizerName = organizerName.length() > 5 ? organizerName.substring(0, 5) + "..." : organizerName;
                                tvOrganiserFinal.setText(" Organised by: " + limitedOrganizerName);

                                // Retrieve the user's imageUrl from the document
                                String imageUrl = document.getString("imageUrl");
                                // Load image using Glide
                                Glide.with(itemView.getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_image)
                                        .error(R.drawable.default_image)
                                        .into(ivOrganiser);
                            }
                        } else {
                            Log.d("Firestore", "No documents found.");
                        }
                    });

            String limitMeetingName = meeting.getTitle().length() > 19 ? meeting.getTitle().substring(0, 19) + "..." : meeting.getTitle();
            tvMeetingTitle.setText(limitMeetingName);
            tvMeetingDate.setText(" Date: " + meeting.getDate());
            tvMeetingTime.setText(" Time: " + meeting.getTime());

            // Set the meeting status text
            String statusText = " Status: " + meeting.getStatus();

            // Check if the meeting status is "Ended"
            if (meeting.getStatus().equals("Ended")) {
                // Create a SpannableString to apply color only to the status text
                SpannableString spannableString = new SpannableString(statusText);

                // Calculate the index range for the status text
                int statusStartIndex = statusText.indexOf(meeting.getStatus());
                int statusEndIndex = statusStartIndex + meeting.getStatus().length();

                // Change the text color to red for the status text
                spannableString.setSpan(new ForegroundColorSpan(itemView.getContext().getResources().getColor(android.R.color.holo_red_light)),
                        statusStartIndex, statusEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Set the SpannableString to the TextView
                tvStatus.setText(spannableString);
            } else {
                // Set the status text without color
                tvStatus.setText(statusText);
            }

            // Check if the current user is the organizer of the meeting
            if (organizerEmail.equals(currentUserEmail)) {
                // If the current user is the organizer, show the edit icon
                // Assuming you have an ImageView for the edit icon with id imageViewEdit
                imageViewEdit.setVisibility(View.VISIBLE);
            } else {
                // If the current user is not the organizer, hide the edit icon
                imageViewEdit.setVisibility(View.GONE);
            }
        }
    }
}
