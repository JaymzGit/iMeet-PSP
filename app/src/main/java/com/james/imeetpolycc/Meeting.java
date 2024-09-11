package com.james.imeetpolycc;

import java.util.ArrayList;
import java.util.List;

public class Meeting {

    // Meeting attributes
    private String id;
    private String organiser;
    private String title;
    private String date;
    private String time;
    private String status;
    private List<Participant> participants;

    // Default constructor for Firebase
    public Meeting() {
    }

    public Meeting(String id, String title, String date, String time, String organiser, String status) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.organiser = organiser;
        this.status = status;
        this.participants = new ArrayList<>(); // Initialize participants list
    }

    public List<String> getParticipantEmails() {
        List<String> emails = new ArrayList<>();
        for (Participant participant : participants) {
            emails.add(participant.getEmail());
        }
        return emails;
    }

    // Getter and setter methods for participants
    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) { this.participants = participants; }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

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