package com.james.imeetpsp;

import java.util.HashMap;
import java.util.Map;

public class Participant {

    // Participant attributes
    private String fname;
    private String email;
    private String imageUrl;
    private boolean attended;
    private String reason;

    // Default constructor for Firebase
    public Participant() {
    }

    // Parameterized constructor
    public Participant(String fname, String email, String imageUrl, boolean attended, String reason) {
        this.fname = fname;
        this.email = email;
        this.imageUrl = imageUrl;
        this.attended = attended;
        this.reason = reason;
    }

    // Getter and setter for participant's name
    public String getName() {
        return fname;
    }

    public void setName(String fname) {
        this.fname = fname;
    }

    // Getter and setter for participant's email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getter and setter for participant's profile picture URL
    public String getProfilePictureUrl() {
        return imageUrl;
    }

    public void setProfilePictureUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Getter and setter for attendance status
    public boolean getAttendance() {
        return attended;
    }

    public void setAttendance(boolean attended) {
        this.attended = attended;
    }

    // Getter and setter for reason
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // Convert Participant object to a map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> participantMap = new HashMap<>();
        participantMap.put("fname", fname);
        participantMap.put("email", email);
        participantMap.put("imageUrl", imageUrl);
        participantMap.put("attended", attended);
        participantMap.put("reason", reason);
        return participantMap;
    }
}