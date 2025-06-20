package com.example.walkfie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.StoryViewHolder> {

    private List<String> stories; // Using String for dummy data

    public StoriesAdapter(List<String> stories) {
        this.stories = stories;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_thumbnail, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        // For actual data, you'd load image from URL/path and set date
        holder.ivStoryThumbnail.setImageResource(R.drawable.sample_story_placeholder); // Use a placeholder image
        holder.tvStoryDate.setText("05/27/2025"); // Dummy date
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoryThumbnail;
        TextView tvStoryDate;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryThumbnail = itemView.findViewById(R.id.ivStoryImage);
            tvStoryDate = itemView.findViewById(R.id.tvStoryDate);
        }
    }
}