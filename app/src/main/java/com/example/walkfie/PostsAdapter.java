package com.example.walkfie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private List<String> posts; // Using String for dummy data

    public PostsAdapter(List<String> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        // For actual data, load images, set real text and dates
        holder.ivPostUserProfilePic.setImageResource(R.drawable.ic_default_profile_placeholder);
        holder.tvPostUserName.setText("John Doe");
        holder.tvPostDate.setText("27/05/2025");
        holder.ivPostMapThumbnail.setImageResource(R.drawable.sample_map_thumbnail); // Use a placeholder map image
        holder.tvPostText.setText("This is a sample post content related to a walk. Location: Dummy Street. Exploring the city!");
        holder.tvLikeCount.setText("Like"); // In a real app, this would be a number
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostUserProfilePic;
        TextView tvPostUserName;
        TextView tvPostDate;
        ImageView ivPostMapThumbnail;
        TextView tvPostText;
        TextView tvLikeCount;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostUserProfilePic = itemView.findViewById(R.id.ivPostUserProfilePic);
            tvPostUserName = itemView.findViewById(R.id.tvPostUserName);
            tvPostDate = itemView.findViewById(R.id.tvPostDate);
            ivPostMapThumbnail = itemView.findViewById(R.id.ivPostMapThumbnail);
            tvPostText = itemView.findViewById(R.id.tvPostText);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
        }
    }
}