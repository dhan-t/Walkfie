// com.example.walkfie/StoryCircleItem.java
package com.example.walkfie;

import java.io.Serializable;
import java.util.List;

// Base interface for items in the story circles RecyclerView
public interface StoryCircleItem extends Serializable {
    int getType(); // Returns the view type (e.g., YOUR_STORY, FRIEND_STORY)
    String getUsername();
    String getProfilePicUrl();
    List<Story> getStories(); // List of actual story objects for this user/circle
}