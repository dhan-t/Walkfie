// com.example.walkfie/Post.java
package com.example.walkfie;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date; // For timestamp

public class Post implements Serializable {
    private String id; // Document ID from Firestore
    private String userId;
    private String username;
    private String userProfilePicUrl;
    private String mediaUrl; // Ensure this matches your Firestore field name for post image/video
    private String caption; // <--- This field and its getter/setter must exist
    private Date timestamp; // <--- This field and its getter/setter must exist
    private long likesCount; // <--- This field and its getter/setter must exist

    public Post() {
        // Required public no-argument constructor for Firestore deserialization
    }

    // You might have a constructor for creating new posts, update it if necessary
    public Post(String userId, String username, String userProfilePicUrl, String mediaUrl, String caption, Date timestamp, long likesCount) {
        this.userId = userId;
        this.username = username;
        this.userProfilePicUrl = userProfilePicUrl;
        this.mediaUrl = mediaUrl;
        this.caption = caption;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUserProfilePicUrl() { return userProfilePicUrl; }
    public String getMediaUrl() { return mediaUrl; } // Ensure this matches your Firestore field name
    public String getCaption() { return caption; } // Make sure this getter exists
    @ServerTimestamp // This annotation is crucial if Firestore automatically sets this field
    public Date getTimestamp() { return timestamp; } // Make sure this getter exists
    public long getLikesCount() { return likesCount; } // Make sure this getter exists

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setUserProfilePicUrl(String userProfilePicUrl) { this.userProfilePicUrl = userProfilePicUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setCaption(String caption) { this.caption = caption; } // Make sure this setter exists
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; } // Make sure this setter exists
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; } // Make sure this setter exists
}