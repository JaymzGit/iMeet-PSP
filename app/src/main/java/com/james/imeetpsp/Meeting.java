package com.james.imeetpsp;

import java.util.ArrayList;

public class Meeting {
    private String organiser;
    private String title;
    private String date;
    private String time;
    private String status;
    private ArrayList<String> participants; // Add field for participants

    public Meeting() {
        // Empty constructor required for Firebase
    }

    public Meeting(String title, String date, String time, String organiser, String status) {
        this.title = title;
        this.date = date;
        this.time = time;
        this.organiser = organiser;
        this.status = status;
        this.participants = new ArrayList<>(); // Initialize participants list
    }

    // Getter and setter methods for participants
    public ArrayList<String> getParticipants() {
        return participants;
    }

    public void setParticipants(ArrayList<String> participants) {
        this.participants = participants;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getOrganiser() { return organiser; }

    public void setOrganiser(String organiser) { this.organiser = organiser; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}