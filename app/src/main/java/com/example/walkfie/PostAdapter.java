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

import de.hdodenhof.circleimageview.CircleImageView;

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

        // Show edited tag if post has been edited
        boolean isEdited = false;
        Date editedAt = null;
        if (post instanceof Post) {
            try {
                // If you add edited and editedAt fields to Post, use getters here
                java.lang.reflect.Method getEdited = post.getClass().getMethod("getEdited");
                Object editedValue = getEdited.invoke(post);
                if (editedValue instanceof Boolean) {
                    isEdited = (Boolean) editedValue;
                }
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Method getEditedAt = post.getClass().getMethod("getEditedAt");
                Object editedAtValue = getEditedAt.invoke(post);
                if (editedAtValue instanceof Date) {
                    editedAt = (Date) editedAtValue;
                } else if (editedAtValue instanceof com.google.firebase.Timestamp) {
                    editedAt = ((com.google.firebase.Timestamp) editedAtValue).toDate();
                }
            } catch (Exception ignored) {}
        }
        if (isEdited && editedAt != null) {
            holder.tvPostTime.setText("Edited the post " + getFormattedTime(editedAt));
        } else {
            holder.tvPostTime.setText(getFormattedTime(post.getTimestamp()));
        }

        // --- Load images using Glide ---
        // Load Profile Picture (always use userProfilePicUrl)
        String profilePicUrl = post.getUserProfilePicUrl();
        if (profilePicUrl == null || profilePicUrl.isEmpty()) {
            profilePicUrl = post.getProfilePicUrl(); // fallback if needed
        }
        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_default_profile_placeholder)
                    .error(R.drawable.ic_default_profile_placeholder)
                    .into(holder.ivPostProfilePic);
        } else {
            holder.ivPostProfilePic.setImageResource(R.drawable.ic_default_profile_placeholder);
        }

        // Show text body if text-only, otherwise show image
        if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
            holder.ivPostContent.setVisibility(View.VISIBLE);
            holder.tvPostTextBody.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(post.getMediaUrl())
                    .placeholder(R.drawable.sample_story_placeholder)
                    .error(R.drawable.ic_close)
                    .into(holder.ivPostContent);
            // Show caption or text in description
            if (post.getCaption() != null && !post.getCaption().isEmpty()) {
                holder.tvPostDescription.setText(post.getCaption());
            } else if (post.getText() != null && !post.getText().isEmpty()) {
                holder.tvPostDescription.setText(post.getText());
            } else {
                holder.tvPostDescription.setText("");
            }
        } else if (post.getText() != null && !post.getText().isEmpty()) {
            holder.ivPostContent.setVisibility(View.GONE);
            holder.tvPostTextBody.setVisibility(View.VISIBLE);
            holder.tvPostTextBody.setText(post.getText());
            holder.tvPostTextBody.bringToFront();
            holder.tvPostDescription.setText(post.getText());
            Log.d("PostAdapter", "Showing text-only post: " + post.getText());
        } else {
            holder.ivPostContent.setVisibility(View.GONE);
            holder.tvPostTextBody.setVisibility(View.GONE);
            holder.tvPostDescription.setText("");
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

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivPostProfilePic;
        ImageView ivPostContent, ivLike, ivComment, ivShare, ivSave, ivPostOptions;
        TextView tvPostUsername, tvLikesCount, tvPostDescription, tvViewAllComments, tvPostTime, tvPostTextBody;

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
            tvPostTextBody = itemView.findViewById(R.id.tvPostTextBody);
        }
    }
}