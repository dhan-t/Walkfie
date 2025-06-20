package com.example.walkfie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProfilePostsAdapter extends RecyclerView.Adapter<ProfilePostsAdapter.PostThumbnailViewHolder> {

    // Reusing PostAdapter.PostItem from HomeFragment for consistency
    private List<PostAdapter.PostItem> postThumbnails;
    private OnPostThumbnailClickListener listener;

    public interface OnPostThumbnailClickListener {
        void onPostThumbnailClick(PostAdapter.PostItem postItem);
    }

    public ProfilePostsAdapter(List<PostAdapter.PostItem> postThumbnails, OnPostThumbnailClickListener listener) {
        this.postThumbnails = postThumbnails;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This layout should be a simple ImageView to show the post thumbnail
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_post_thumbnail, parent, false);
        return new PostThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostThumbnailViewHolder holder, int position) {
        PostAdapter.PostItem post = postThumbnails.get(position);

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.sample_story_placeholder) // Placeholder for post content
                    .error(R.drawable.sample_story_placeholder)
                    .centerCrop()
                    .into(holder.ivPostThumbnail);
        } else {
            holder.ivPostThumbnail.setImageResource(R.drawable.sample_story_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostThumbnailClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postThumbnails.size();
    }

    public void updatePosts(List<PostAdapter.PostItem> newPostThumbnails) {
        this.postThumbnails.clear();
        this.postThumbnails.addAll(newPostThumbnails);
        notifyDataSetChanged();
    }

    static class PostThumbnailViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostThumbnail;

        public PostThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostThumbnail = itemView.findViewById(R.id.ivPostThumbnail);
        }
    }
}