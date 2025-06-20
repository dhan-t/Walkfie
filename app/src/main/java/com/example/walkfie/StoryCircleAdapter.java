package com.example.walkfie;

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

import java.util.ArrayList;
import java.util.List;

public class StoryCircleAdapter extends RecyclerView.Adapter<StoryCircleAdapter.StoryCircleViewHolder> {

    public interface OnStoryCircleClickListener {
        void onYourStoryClick();
        void onFriendStoryClick(List<Story> stories, int startIndex);
    }

    private List<StoryCircleItem> storyCircleItems = new ArrayList<>();
    private OnStoryCircleClickListener listener;

    public StoryCircleAdapter(OnStoryCircleClickListener listener) {
        this.listener = listener;
    }

    public void setStoryCircleItems(List<StoryCircleItem> newItems) {
        this.storyCircleItems.clear();
        if (newItems != null) {
            this.storyCircleItems.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void clearStories() {
        storyCircleItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return storyCircleItems.get(position).getType();
    }

    @NonNull
    @Override
    public StoryCircleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == StoryType.YOUR_STORY.ordinal() ?
                        R.layout.item_your_story_circle :
                        R.layout.item_friend_story_circle,
                parent,
                false
        );
        return new StoryCircleViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryCircleViewHolder holder, int position) {
        StoryCircleItem item = storyCircleItems.get(position);
        holder.bind(item);

        holder.itemView.setOnClickListener(v -> {
            Log.d("StoryCircle", "Clicked: " + item.getUsername());
            if (listener != null) {
                if (item.getType() == StoryType.YOUR_STORY.ordinal()) {
                    Log.d("StoryCircle", "YourStory clicked!");
                    listener.onYourStoryClick();
                } else {
                    Log.d("StoryCircle", "FriendStory clicked!");
                    listener.onFriendStoryClick(item.getStories(), 0);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return storyCircleItems.size();
    }

    static class StoryCircleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoryProfilePic;
        TextView tvStoryUsername;
        ImageView ivAddStoryIcon;

        public StoryCircleViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            ivStoryProfilePic = itemView.findViewById(R.id.ivStoryProfilePic);
            tvStoryUsername = itemView.findViewById(R.id.tvStoryUsername);
            if (viewType == StoryType.YOUR_STORY.ordinal()) {
                ivAddStoryIcon = itemView.findViewById(R.id.ivAddStoryIcon);
            }
        }

        public void bind(StoryCircleItem item) {
            tvStoryUsername.setText(item.getUsername());
            Glide.with(itemView.getContext())
                    .load(item.getProfilePicUrl())
                    .apply(RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.ic_default_profile_placeholder)
                            .error(R.drawable.ic_default_profile_placeholder))
                    .into(ivStoryProfilePic);

            if (ivAddStoryIcon != null) {
                ivAddStoryIcon.setVisibility(View.VISIBLE);
            }
        }
    }
}
