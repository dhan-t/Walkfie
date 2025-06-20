// com.example.walkfie/ProfileStoriesAdapter.java
package com.example.walkfie;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView; // If you want to show timestamp/caption

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ProfileStoriesAdapter extends RecyclerView.Adapter<ProfileStoriesAdapter.ProfileStoryViewHolder> {

    private List<Story> storiesList; // This adapter works directly with the Story model
    private OnProfileStoryClickListener onProfileStoryClickListener;
    private Context context;

    public interface OnProfileStoryClickListener {
        // When a story thumbnail is clicked, pass the list of all stories and the index of the clicked one
        void onProfileStoryClick(List<Story> stories, int clickedPosition);
    }

    public ProfileStoriesAdapter(List<Story> storiesList, OnProfileStoryClickListener listener) {
        this.storiesList = storiesList;
        this.onProfileStoryClickListener = listener;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ProfileStoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile_story_thumbnail, parent, false);
        return new ProfileStoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileStoryViewHolder holder, int position) {
        Story story = storiesList.get(position);

        // Load the story image (which is storyImageUrl in your Story model)
        if (context != null && story.getMediaUrl() != null && !story.getMediaUrl().isEmpty()) {
            Glide.with(context)
                    .load(story.getMediaUrl())
                    // Apply transformations for a nice thumbnail look
                    .apply(new RequestOptions()
                            .transform(new CenterCrop(), new RoundedCorners(16)) // Adjust radius as needed
                            .placeholder(R.drawable.sample_story_placeholder) // A general placeholder for images
                            .error(R.drawable.ic_close)) // An error image
                    .into(holder.ivStoryThumbnail);
        } else if (context != null) {
            holder.ivStoryThumbnail.setImageResource(R.drawable.sample_story_placeholder); // Default if no URL
        }

        // Optional: Display timestamp or caption if needed
        // holder.tvStoryTimestamp.setText(story.getFormattedTime());
        // holder.tvStoryCaption.setText(story.getCaption());

        holder.itemView.setOnClickListener(v -> {
            if (onProfileStoryClickListener != null) {
                // Pass the entire list of stories and the position of the clicked story
                // This allows the StoryViewer to start from the correct story.
                onProfileStoryClickListener.onProfileStoryClick(storiesList, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return storiesList.size();
    }

    public void updateStories(List<Story> newStoriesList) { // Now accepts List<Story>
        this.storiesList.clear();
        this.storiesList.addAll(newStoriesList);
        notifyDataSetChanged();
    }

    static class ProfileStoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoryThumbnail;
        // TextView tvStoryTimestamp; // Uncomment if you add this to the layout

        public ProfileStoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryThumbnail = itemView.findViewById(R.id.ivProfileStoryThumbnail);
            // tvStoryTimestamp = itemView.findViewById(R.id.tvStoryTimestamp); // Uncomment
        }
    }
}