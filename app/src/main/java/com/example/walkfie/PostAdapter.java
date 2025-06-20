package com.example.walkfie;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList; // Correctly named 'postList'
    private OnPostInteractionListener onPostInteractionListener;

    public interface OnPostInteractionListener {
        void onProfileClick(Post post);
        void onLikeClick(Post post);
        void onCommentClick(Post post);
        void onShareClick(Post post);
        void onSaveClick(Post post);
        void onViewCommentsClick(Post post);
        void onPostOptionsClick(Post post);
    }

    public PostAdapter(List<Post> postList, OnPostInteractionListener onPostInteractionListener) {
        this.postList = postList;
        this.onPostInteractionListener = onPostInteractionListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_ulit, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // --- Bind data from Post (now from Firestore) ---
        holder.tvPostUsername.setText(post.getUsername());
        holder.tvLikesCount.setText(post.getLikesCount() + " likes");
        holder.tvPostDescription.setText(post.getCaption());
        // For commentsCount, ensure Post.java has getCommentsCount() getter for the 'commentsCount' field.
        // It seems you've added commentsCount to Post.java, so this line should be fine now.
        holder.tvViewAllComments.setText("View all " + post.getCommentsCount() + " comments");
        holder.tvPostTime.setText(getFormattedTime(post.getTimestamp()));

        // --- Load images using Glide ---
        // Load Profile Picture
        if (post.getUserProfilePicUrl() != null && !post.getUserProfilePicUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getUserProfilePicUrl())
                    .placeholder(R.drawable.ic_default_profile_placeholder)
                    .error(R.drawable.ic_default_profile_placeholder)
                    .into(holder.ivPostProfilePic);
        } else {
            holder.ivPostProfilePic.setImageResource(R.drawable.ic_default_profile_placeholder);
        }

        // Load Post Content Image
        if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getMediaUrl())
                    .placeholder(R.drawable.sample_story_placeholder)
                    .error(R.drawable.ic_close) // Use a more appropriate error icon, e.g., ic_error
                    .into(holder.ivPostContent);
        } else {
            holder.ivPostContent.setImageResource(R.drawable.sample_story_placeholder); // Consider a dedicated post placeholder
        }

        // --- Set click listeners ---
        holder.ivPostProfilePic.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onProfileClick(post);
        });
        holder.tvPostUsername.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onProfileClick(post);
        });
        holder.ivLike.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onLikeClick(post);
        });
        holder.ivComment.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onCommentClick(post);
        });
        holder.ivShare.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onShareClick(post);
        });
        holder.ivSave.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onSaveClick(post);
        });
        holder.tvViewAllComments.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onViewCommentsClick(post);
        });
        holder.ivPostOptions.setOnClickListener(v -> {
            if (onPostInteractionListener != null) onPostInteractionListener.onPostOptionsClick(post);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void updatePosts(List<Post> newPosts) {
        Log.d("PostAdapter", "updatePosts called. Incoming list size: " + (newPosts == null ? "null" : newPosts.size()));
        if (newPosts != null) {
            for (int i = 0; i < newPosts.size(); i++) {
                Post p = newPosts.get(i);
                Log.d("PostAdapter", "newPosts[" + i + "]: id=" + p.getId() + ", caption=" + p.getCaption());
            }
        }
        if (this.postList == null) {
            Log.e("PostAdapter", "ERROR: adapter's postList is null in updatePosts! Initializing new ArrayList.");
            this.postList = new ArrayList<>();
        }
        if (newPosts == null) {
            Log.w("PostAdapter", "WARNING: newPosts list is null in updatePosts. Clearing postList.");
            Log.d("PostAdapter", Log.getStackTraceString(new Exception("updatePosts called with null list")));
            this.postList.clear();
            notifyDataSetChanged();
            return;
        }
        if (newPosts.isEmpty()) {
            Log.w("PostAdapter", "WARNING: newPosts list is EMPTY in updatePosts. Stack trace:");
            Log.d("PostAdapter", Log.getStackTraceString(new Exception("updatePosts called with empty list")));
        }
        Log.d("PostAdapter", "postList size BEFORE clear (in adapter): " + this.postList.size());
        this.postList.clear();
        Log.d("PostAdapter", "postList size AFTER clear (in adapter): " + this.postList.size());
        this.postList.addAll(newPosts);
        Log.d("PostAdapter", "updatePosts finished. Internal list size: " + this.postList.size());
        notifyDataSetChanged();
    }

    // Helper method for formatting timestamp
    public String getFormattedTime(Date timestamp) {
        if (timestamp == null) {
            return "";
        }

        long currentTime = System.currentTimeMillis();
        long postTimeMillis = timestamp.getTime();
        long diff = currentTime - postTimeMillis;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toMinutes(diff) + "m ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toHours(diff) + "h ago";
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault());
            return sdf.format(timestamp);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostProfilePic, ivPostContent, ivLike, ivComment, ivShare, ivSave, ivPostOptions;
        TextView tvPostUsername, tvLikesCount, tvPostDescription, tvViewAllComments, tvPostTime;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostProfilePic = itemView.findViewById(R.id.ivPostProfilePic);
            ivPostContent = itemView.findViewById(R.id.ivPostContent);
            ivLike = itemView.findViewById(R.id.ivLike);
            ivComment = itemView.findViewById(R.id.ivComment);
            ivShare = itemView.findViewById(R.id.ivShare);
            ivSave = itemView.findViewById(R.id.ivSave);
            ivPostOptions = itemView.findViewById(R.id.ivPostOptions);

            tvPostUsername = itemView.findViewById(R.id.tvPostUsername);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
            tvPostDescription = itemView.findViewById(R.id.tvPostDescription);
            tvViewAllComments = itemView.findViewById(R.id.tvViewAllComments);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
        }
    }
}