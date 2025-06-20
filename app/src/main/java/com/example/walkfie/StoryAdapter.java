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
import androidx.core.content.ContextCompat; // Import for ContextCompat
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
        // onYourStoryClick and onFriendStoryClick are mostly redundant now if onStoryClick is used for viewing
        // and onUserStoryClickToAdd is for creation. You might keep them if your Activity
        // needs specific triggers, but the core logic below uses onStoryClick/onUserStoryClickToAdd.
        void onYourStoryClick(); // Potentially deprecated or used for empty state general click
        void onFriendStoryClick(String userId); // Potentially deprecated

        void onStoryClick(List<Story> storiesToView, int startIndex); // Unified click for viewing stories
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
        return storiesData.get(position).getType().ordinal();
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == StoryType.YOUR_STORY.ordinal()) { // This is correct if StoryType is an inner class OR imported
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_your_story_circle, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_story_circle, parent, false);
        }
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        StoryItem storyItem = storiesData.get(position);
        holder.bind(storyItem);

        // Clear previous listeners
        holder.itemView.setOnClickListener(null);
        if (holder.ivAddStoryButton != null) {
            holder.ivAddStoryButton.setOnClickListener(null);
        }
        holder.ivStoryProfilePic.setOnClickListener(null);

        if (storyItem.getType() == StoryType.YOUR_STORY) {
            if (storyItem.getUserStories() == null || storyItem.getUserStories().isEmpty()) {
                // No stories: show add icon, make whole circle and add icon clickable to add
                holder.ivAddStoryButton.setVisibility(View.VISIBLE);
                View.OnClickListener addStoryListener = v -> {
                    if (listener != null) listener.onUserStoryClickToAdd();
                };
                holder.itemView.setOnClickListener(addStoryListener);
                holder.ivAddStoryButton.setOnClickListener(addStoryListener);
                holder.ivStoryProfilePic.setOnClickListener(addStoryListener);
                holder.ivStoryProfilePic.setBackgroundResource(R.drawable.circle_add_story_border);
            } else {
                // Has stories: hide add icon, make whole circle clickable to view
                holder.ivAddStoryButton.setVisibility(View.GONE);
                View.OnClickListener viewStoryListener = v -> {
                    if (listener != null && !storyItem.getUserStories().isEmpty()) {
                        listener.onStoryClick(storyItem.getUserStories(), 0);
                    }
                };
                holder.itemView.setOnClickListener(viewStoryListener);
                holder.ivStoryProfilePic.setOnClickListener(viewStoryListener);
                holder.ivStoryProfilePic.setBackgroundResource(R.drawable.circle_add_story_border);
            }
            holder.tvStoryUsername.setText("Your Story");
        } else if (storyItem.getType() == StoryType.FRIEND_STORY) {
            if (holder.ivAddStoryButton != null) holder.ivAddStoryButton.setVisibility(View.GONE);
            holder.tvStoryUsername.setText(storyItem.getUsername());
            View.OnClickListener viewFriendStoryListener = v -> {
                if (listener != null && !storyItem.getUserStories().isEmpty()) {
                    listener.onStoryClick(storyItem.getUserStories(), 0);
                }
            };
            holder.itemView.setOnClickListener(viewFriendStoryListener);
            holder.ivStoryProfilePic.setOnClickListener(viewFriendStoryListener);
            holder.ivStoryProfilePic.setBackgroundResource(R.drawable.circle_add_story_border);
        }
    }

    @Override
    public int getItemCount() {
        return storiesData.size();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        de.hdodenhof.circleimageview.CircleImageView ivStoryProfilePic; // Changed to CircleImageView
        TextView tvStoryUsername;
        ImageView ivAddStoryButton; // Only present in item_your_story_circle

        public StoryViewHolder(@NonNull View itemView) { // Simplified constructor
            super(itemView);
            ivStoryProfilePic = itemView.findViewById(R.id.ivStoryProfilePic);
            tvStoryUsername = itemView.findViewById(R.id.tvStoryUsername);

            // This only finds the button if it exists in the inflated layout
            ivAddStoryButton = itemView.findViewById(R.id.ivAddStoryButton);
            // No longer need to check viewType here, as findViewById returns null if ID not found
        }

        public void bind(StoryItem item) {
            tvStoryUsername.setText(item.getUsername());

            // Load profile picture for ALL story types
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

            // The visibility of ivAddStoryButton and click listeners are handled in onBindViewHolder now
            // Border styling should also be handled in onBindViewHolder based on story status (seen/unseen)
        }
    }

    // --- StoryItem Class (INNER CLASS, keep it inside StoryAdapter.java) ---
    // No changes needed here, it looks good.
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

    // --- StoryType Enum (add this if it's not already a separate file) ---
    public enum StoryType {
        YOUR_STORY,
        FRIEND_STORY
    }
}