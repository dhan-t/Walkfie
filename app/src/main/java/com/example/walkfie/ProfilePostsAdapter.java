package com.example.walkfie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast; // Keep Toast import for debugging if needed

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProfilePostsAdapter extends RecyclerView.Adapter<ProfilePostsAdapter.PostThumbnailViewHolder> {

    // FIX: Change from PostAdapter.PostItem to Post
    private List<Post> postThumbnails; // Now correctly holds List<Post>
    private OnPostThumbnailClickListener listener;

    public interface OnPostThumbnailClickListener {
        void onPostThumbnailClick(Post post); // <-- FIX: Callback now passes Post object
    }

    // FIX: Constructor now accepts List<Post>
    public ProfilePostsAdapter(List<Post> postThumbnails, OnPostThumbnailClickListener listener) {
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
        Post post = postThumbnails.get(position); // <-- FIX: Get a Post object

        // FIX: Use post.getMediaUrl() as per our Post model
        // Assuming mediaUrl holds the URL for the main media (image/video thumbnail)
        if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getMediaUrl())
                    .placeholder(R.drawable.sample_story_placeholder) // Placeholder for post content
                    .error(R.drawable.ic_close) // Use a proper error image
                    .centerCrop()
                    .into(holder.ivPostThumbnail);
        } else {
            holder.ivPostThumbnail.setImageResource(R.drawable.sample_story_placeholder); // Default if no mediaUrl
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostThumbnailClick(post); // <-- FIX: Pass the Post object
            }
        });
    }

    @Override
    public int getItemCount() {
        return postThumbnails.size();
    }

    // FIX: Method now accepts List<Post>
    public void updatePosts(List<Post> newPostThumbnails) {
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