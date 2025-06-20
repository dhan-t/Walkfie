// com.example.walkfie/Post.java
package com.example.walkfie;

import com.google.firebase.Timestamp; // Changed from java.util.Date
import java.io.Serializable;
// Removed java.util.Date import as it's no longer directly used for the timestamp field
import java.util.Date;
import java.util.List;

public class Post implements Serializable {
    private String id;
    private String userId;
    private String username;
    private String userProfilePicUrl;
    private String mediaUrl;
    private String caption;
    private Timestamp timestamp; // Changed to com.google.firebase.Timestamp
    private long likesCount;
    private long commentsCount;
    private List<String> likedBy; // New field for tracking user IDs who liked the post
    private String text;
    private String profilePicUrl;
    private boolean edited;
    private com.google.firebase.Timestamp editedAt;

    public Post() {
        // Required public no-argument constructor for Firestore deserialization
    }

    // Constructor for new posts without an ID (ID will be set by Firestore)
    public Post(String userId, String username, String userProfilePicUrl, String mediaUrl, String caption) {
        this.userId = userId;
        this.username = username;
        this.userProfilePicUrl = userProfilePicUrl;
        this.mediaUrl = mediaUrl;
        this.caption = caption;
        this.likesCount = 0;
        this.commentsCount = 0;
        // Timestamp will be set by @ServerTimestamp or when fetched from Firestore
    }

    // Full constructor (useful for testing or if you need to manually construct with all fields)
    public Post(String id, String userId, String username, String userProfilePicUrl, String mediaUrl, String caption, Timestamp timestamp, long likesCount, long commentsCount, List<String> likedBy, String text, String profilePicUrl, boolean edited, com.google.firebase.Timestamp editedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.userProfilePicUrl = userProfilePicUrl;
        this.mediaUrl = mediaUrl;
        this.caption = caption;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.likedBy = likedBy;
        this.text = text;
        this.profilePicUrl = profilePicUrl;
        this.edited = edited;
        this.editedAt = editedAt;
    }


    // --- Getters ---
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUserProfilePicUrl() { return userProfilePicUrl; }
    public String getMediaUrl() { return mediaUrl; }
    public String getCaption() { return caption; }
    public String getText() { return text; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public boolean getEdited() { return edited; }
    public com.google.firebase.Timestamp getEditedAt() { return editedAt; }

    // Removed @ServerTimestamp from getter as it's primarily for writing to Firestore
    public Date getTimestamp() { return timestamp.toDate(); }
    public long getLikesCount() { return likesCount; }
    public long getCommentsCount() { return commentsCount; }
    public List<String> getLikedBy() { return likedBy; } // New getter for likedBy

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setUserProfilePicUrl(String userProfilePicUrl) { this.userProfilePicUrl = userProfilePicUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; } // Changed parameter type
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }
    public void setCommentsCount(long commentsCount) { this.commentsCount = commentsCount; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; } // New setter for likedBy
    public void setText(String text) { this.text = text; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
    public void setEdited(boolean edited) { this.edited = edited; }
    public void setEditedAt(com.google.firebase.Timestamp editedAt) { this.editedAt = editedAt; }

    // You might want to add a convenience method to get java.util.Date if needed elsewhere
    public java.util.Date getTimestampAsDate() {
        return timestamp != null ? timestamp.toDate() : null;
    }
}