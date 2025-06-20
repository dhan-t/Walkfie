package com.example.walkfie;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    private String uid;
    private String firstName; // Added
    private String lastName;  // Added
    private String username;  // Still useful for combined display or if you only use this
    private String bio;
    private String profilePicUrl;
    private String email;
    private Date createdAt;

    public User() {}

    // Constructor with firstName and lastName for comprehensive profile
    public User(String uid, String firstName, String lastName, String username, String bio, String profilePicUrl, String email) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username; // Can be derived as firstName + " " + lastName
        this.bio = bio;
        this.profilePicUrl = profilePicUrl;
        this.email = email;
        // createdAt will be set by @ServerTimestamp
    }

    // --- Getters ---
    public String getUid() { return uid; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getUsername() { return username; }
    public String getBio() { return bio; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public String getEmail() { return email; }

    @ServerTimestamp
    public Date getCreatedAt() { return createdAt; }

    // --- Setters ---
    public void setUid(String uid) { this.uid = uid; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setUsername(String username) { this.username = username; }
    public void setBio(String bio) { this.bio = bio; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
    public void setEmail(String email) { this.email = email; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}