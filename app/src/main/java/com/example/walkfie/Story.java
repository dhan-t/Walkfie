package com.example.walkfie;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;


public class Story implements Parcelable {
    private String id;
    private String text;
    private String mediaUrl;
    private String mediaType; // "image" or "video"
    private String userId;
    private String username;
    private String userProfilePicUrl;
    private Timestamp timestamp;

    // Required empty constructor for Firestore
    public Story() {}

    // Full constructor
    public Story(String id, String text, String mediaUrl, String mediaType, String userId, String username, String userProfilePicUrl, Timestamp timestamp) {
        this.id = id;
        this.text = text;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.userId = userId;
        this.username = username;
        this.userProfilePicUrl = userProfilePicUrl;
        this.timestamp = timestamp;
    }

    // Parcelable constructor
    protected Story(Parcel in) {
        id = in.readString();
        text = in.readString();
        mediaUrl = in.readString();
        mediaType = in.readString();
        userId = in.readString();
        username = in.readString();
        userProfilePicUrl = in.readString();
        long seconds = in.readLong();
        this.timestamp = new Timestamp(seconds, 0); // nanoseconds = 0
    }

    public static final Creator<Story> CREATOR = new Creator<Story>() {
        @Override
        public Story createFromParcel(Parcel in) {
            return new Story(in);
        }

        @Override
        public Story[] newArray(int size) {
            return new Story[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(text);
        dest.writeString(mediaUrl);
        dest.writeString(mediaType);
        dest.writeString(userId);
        dest.writeString(username);
        dest.writeString(userProfilePicUrl);
        dest.writeLong(timestamp != null ? timestamp.getSeconds() : 0L);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getText() { return text; }
    public String getMediaUrl() { return mediaUrl; }
    public String getMediaType() { return mediaType; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUserProfilePicUrl() { return userProfilePicUrl; }
    public Timestamp getTimestamp() { return timestamp; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setText(String text) { this.text = text; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setUserProfilePicUrl(String userProfilePicUrl) { this.userProfilePicUrl = userProfilePicUrl; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Story{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", userProfilePicUrl='" + userProfilePicUrl + '\'' +
                ", timestamp=" + (timestamp != null ? timestamp.toDate() : "null") +
                '}';
    }
}
