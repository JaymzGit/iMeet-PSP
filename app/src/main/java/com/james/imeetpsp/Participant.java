package com.james.imeetpsp;

public class Participant {
    private String fname;
    private String email;
    private String imageUrl;

    public Participant() {
    }

    public Participant(String fname, String email, String imageUrl) {
        this.fname = fname;
        this.email = email;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public String getName() {
        return fname;
    }

    public void setName(String fname) {
        this.fname = fname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePictureUrl() {
        return imageUrl;
    }

    public void setProfilePictureUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
