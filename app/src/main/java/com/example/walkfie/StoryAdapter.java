// com.example.walkfie/StoryAdapter.java
package com.example.walkfie;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private static final String TAG = "StoryAdapter";
    private Context context;
    private List<StoryItem> storiesData; // Changed to StoryItem
    private OnStoryClickListener listener;

    public interface OnStoryClickListener {
        void onYourStoryClick(); // For adding new story or viewing your own
        void onFriendStoryClick(String userId); // This might be redundant if you always use onStoryClick
        void onStoryClick(List<Story> storiesToView, int startIndex); // Unified click for viewing
        void onUserStoryClickToAdd(); // Specifically for clicking the '+' icon on 'Your Story' circle
    }

    public StoryAdapter(Context context, List<StoryItem> storiesData, OnStoryClickListener listener) {
        this.context = context;
        this.storiesData = storiesData;
        this.listener = listener;
    }

    public void updateStories(List<StoryItem> newStoriesData) {
        this.storiesData.clear();
        if (newStoriesData != null) {
            this.storiesData.addAll(newStoriesData);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return storiesData.get(position).getType().ordinal(); // Use ordinal for view type
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == StoryType.YOUR_STORY.ordinal()) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_your_story_circle, parent, false);
        } else { // Assuming viewType == StoryType.FRIEND_STORY.ordinal()
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_story_circle, parent, false);
        }
        return new StoryViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        StoryItem storyItem = storiesData.get(position);
        holder.bind(storyItem);

        // Set click listeners based on story type
        if (storyItem.getType() == StoryType.YOUR_STORY) {
            if (storyItem.getUserStories().isEmpty()) {
                // If user has no stories, clicking the circle acts as "add story"
                // The ivAddStoryIcon handles this specific action
                holder.ivAddStoryIcon.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUserStoryClickToAdd();
                    }
                });
                // The main circle click (ivStoryProfilePic) could also go to add, or show empty state if clicked elsewhere
                holder.ivStoryProfilePic.setOnClickListener(v -> {
                    if (listener != null) {
                        // If no stories, it should lead to adding a story
                        listener.onUserStoryClickToAdd();
                    }
                });
            } else {
                // If user has stories, clicking the circle views their stories
                holder.itemView.setOnClickListener(v -> { // Or ivStoryProfilePic click listener
                    if (listener != null) {
                        listener.onStoryClick(storyItem.getUserStories(), 0); // View their own stories
                    }
                });
                // Hide add icon if stories exist and clicking circle views them
                if (holder.ivAddStoryIcon != null) {
                    holder.ivAddStoryIcon.setVisibility(View.GONE);
                }
            }

        } else if (storyItem.getType() == StoryType.FRIEND_STORY) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFriendStoryClick(storyItem.getUserId()); // Or directly pass stories:
                    listener.onStoryClick(storyItem.getUserStories(), 0);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return storiesData.size();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoryProfilePic;
        TextView tvStoryUsername;
        ImageView ivAddStoryIcon; // Only present in item_your_story_circle

        public StoryViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            ivStoryProfilePic = itemView.findViewById(R.id.ivStoryProfilePic);
            tvStoryUsername = itemView.findViewById(R.id.tvStoryUsername);

            if (viewType == StoryType.YOUR_STORY.ordinal()) {
                ivAddStoryIcon = itemView.findViewById(R.id.ivAddStoryIcon);
                // Optional: set a default image for your own story if none is loaded yet
                ivStoryProfilePic.setImageResource(R.drawable.ic_default_profile_placeholder);
            }
            // Add any other specific view initializations based on viewType if needed
        }

        public void bind(StoryItem item) {
            tvStoryUsername.setText(item.getUsername());

            // Load profile picture
            if (item.getProfilePicUrl() != null && !item.getProfilePicUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getProfilePicUrl())
                        .apply(RequestOptions.circleCropTransform()
                                .placeholder(R.drawable.ic_default_profile_placeholder)
                                .error(R.drawable.ic_default_profile_placeholder))
                        .into(ivStoryProfilePic);
            } else {
                ivStoryProfilePic.setImageResource(R.drawable.ic_default_profile_placeholder);
            }

            // Specific logic for 'Your Story' add icon
            if (ivAddStoryIcon != null) { // This ViewHolder corresponds to Your Story circle
                if (item.getUserStories() == null || item.getUserStories().isEmpty()) {
                    ivAddStoryIcon.setVisibility(View.VISIBLE);
                } else {
                    ivAddStoryIcon.setVisibility(View.GONE); // Hide '+' if stories exist (user views their own story)
                }
            }
        }
    }

    // --- StoryItem Class (INNER CLASS, keep it inside StoryAdapter.java) ---
    // This is crucial for handling different types of story circles in one adapter
    public static class StoryItem implements Serializable {
        private StoryType type;
        private String userId;
        private String username;
        private String profilePicUrl;
        private List<Story> userStories; // Stories for this specific user/circle

        private StoryItem(StoryType type, String userId, String username, String profilePicUrl) {
            this.type = type;
            this.userId = userId;
            this.username = username;
            this.profilePicUrl = profilePicUrl;
            this.userStories = new ArrayList<>(); // Initialize to prevent null pointer
        }

        public static StoryItem createYourStoryItem(String userId, String username, String profilePicUrl) {
            return new StoryItem(StoryType.YOUR_STORY, userId, username, profilePicUrl);
        }

        public static StoryItem createFriendStoryItem(String userId, String username, String profilePicUrl) {
            return new StoryItem(StoryType.FRIEND_STORY, userId, username, profilePicUrl);
        }

        // Getters
        public StoryType getType() { return type; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getProfilePicUrl() { return profilePicUrl; }
        public List<Story> getUserStories() { return userStories; }

        // Setters (if needed for dynamic updates)
        public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
        public void addStory(Story story) {
            if (story != null) {
                this.userStories.add(story);
            }
        }

        @Override
        public String toString() {
            return "StoryItem{" +
                    "type=" + type +
                    ", userId='" + userId + '\'' +
                    ", username='" + username + '\'' +
                    ", profilePicUrl='" + profilePicUrl + '\'' +
                    ", userStoriesCount=" + (userStories != null ? userStories.size() : 0) +
                    '}';
        }
    }
}