// com.example.walkfie/YourStoryCircleItem.java
package com.example.walkfie;

import java.util.List;

public class YourStoryCircleItem implements StoryCircleItem {
    private String username;
    private String profilePicUrl;
    private List<Story> stories; // Could be empty if no stories yet

    public YourStoryCircleItem(String username, String profilePicUrl, List<Story> stories) {
        this.username = username;
        this.profilePicUrl = profilePicUrl;
        this.stories = stories;
    }

    @Override
    public int getType() {
        return StoryType.YOUR_STORY.ordinal(); // Using enum ordinal as type
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
}