package com.example.walkfie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    // For now, let's keep it simple with just username and profile pic URL.
    // In a real app, this could be a 'User' object or a dedicated 'Friend' object.
    public static class FriendItem {
        private String userId;
        private String username;
        private String profilePicUrl;

        public FriendItem() {} // For Firestore if you fetch friends this way

        public FriendItem(String userId, String username, String profilePicUrl) {
            this.userId = userId;
            this.username = username;
            this.profilePicUrl = profilePicUrl;
        }

        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getProfilePicUrl() { return profilePicUrl; }

        public void setUserId(String userId) { this.userId = userId; }
        public void setUsername(String username) { this.username = username; }
        public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
    }


    private List<FriendItem> friendsList;
    private OnFriendClickListener onFriendClickListener;

    public interface OnFriendClickListener {
        void onFriendClick(FriendItem friendItem);
    }

    public FriendsAdapter(List<FriendItem> friendsList, OnFriendClickListener listener) {
        this.friendsList = friendsList;
        this.onFriendClickListener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_thumbnail, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendItem friend = friendsList.get(position);
        holder.tvFriendUsername.setText(friend.getUsername());

        if (friend.getProfilePicUrl() != null && !friend.getProfilePicUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(friend.getProfilePicUrl())
                    .placeholder(R.drawable.ic_default_profile_placeholder)
                    .error(R.drawable.ic_default_profile_placeholder)
                    .into(holder.ivFriendProfilePic);
        } else {
            holder.ivFriendProfilePic.setImageResource(R.drawable.ic_default_profile_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onFriendClickListener != null) {
                onFriendClickListener.onFriendClick(friend);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public void updateFriends(List<FriendItem> newFriendsList) {
        this.friendsList.clear();
        this.friendsList.addAll(newFriendsList);
        notifyDataSetChanged();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFriendProfilePic;
        TextView tvFriendUsername;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFriendProfilePic = itemView.findViewById(R.id.ivFriendProfilePic);
            tvFriendUsername = itemView.findViewById(R.id.tvFriendUsername);
        }
    }
}