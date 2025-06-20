// com.example.walkfie/Post.java
package com.example.walkfie;

import com.google.firebase.Timestamp; // Changed from java.util.Date
import java.io.Serializable;
// Removed java.util.Date import as it's no longer directly used for the timestamp field
import java.util.Date;

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
    public Post(String id, String userId, String username, String userProfilePicUrl, String mediaUrl, String caption, Timestamp timestamp, long likesCount, long commentsCount) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.userProfilePicUrl = userProfilePicUrl;
        this.mediaUrl = mediaUrl;
        this.caption = caption;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
    }


    // --- Getters ---
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUserProfilePicUrl() { return userProfilePicUrl; }
    public String getMediaUrl() { return mediaUrl; }
    public String getCaption() { return caption; }

    // Removed @ServerTimestamp from getter as it's primarily for writing to Firestore
    public Date getTimestamp() { return timestamp.toDate(); }
    public long getLikesCount() { return likesCount; }
    public long getCommentsCount() { return commentsCount; }

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

    // You might want to add a convenience method to get java.util.Date if needed elsewhere
    public java.util.Date getTimestampAsDate() {
        return timestamp != null ? timestamp.toDate() : null;
    }
}