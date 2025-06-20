// com.example.walkfie/FriendStoryCircleItem.java
package com.example.walkfie;

import java.util.List;

public class FriendStoryCircleItem implements StoryCircleItem {
    private String username;
    private String profilePicUrl;
    private String userId; // Useful for fetching stories if not pre-loaded
    private List<Story> stories; // List of actual story objects for this friend

    public FriendStoryCircleItem(String username, String profilePicUrl, String userId, List<Story> stories) {
        this.username = username;
        this.profilePicUrl = profilePicUrl;
        this.userId = userId;
        this.stories = stories;
    }

    @Override
    public int getType() {
        return StoryType.FRIEND_STORY.ordinal(); // Using enum ordinal as type
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    @Override
    public List<Story> getStories() {
        return stories;
    }

    public String getUserId() {
        return userId;
    }
}