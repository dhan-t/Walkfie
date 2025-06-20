package com.example.walkfie;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class StoryPageFragment extends Fragment {

    private static final String ARG_STORY = "story_arg";
    private Story story;

    public static StoryPageFragment newInstance(Story story) {
        StoryPageFragment fragment = new StoryPageFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_STORY, story);// Story must implement Serializable
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            story = getArguments().getParcelable(ARG_STORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_story_page, container, false);

        ImageView ivStoryImage = view.findViewById(R.id.ivStoryImage);
        ImageView ivUserProfilePic = view.findViewById(R.id.ivUserProfilePic);
        TextView tvUsername = view.findViewById(R.id.tvUsername);
        TextView tvTimestamp = view.findViewById(R.id.tvStoryTimestamp);
        TextView tvCaption = view.findViewById(R.id.tvStoryCaption);


        Log.d("StoryPageFragment", "Inflating for story: " + (story != null ? story.getId() : "null"));
        Log.d("StoryPageFragment", "Media URL: " + (story != null ? story.getMediaUrl() : "null"));
        Log.d("StoryPageFragment", "Story loaded: " + story);


        if (story != null) {
            // Load image or video thumbnail for now (full video handling is a future step)
            if ("image".equalsIgnoreCase(story.getMediaType())) {
                Glide.with(this)
                        .load(story.getMediaUrl())
                        .placeholder(R.drawable.sample_story_placeholder)
                        .into(ivStoryImage);
            } else {
                // If video, you can load thumbnail (for now)
                Glide.with(this)
                        .load(story.getMediaUrl())
                        .placeholder(R.drawable.sample_story_placeholder)
                        .into(ivStoryImage);
                // You can replace with a VideoView or ExoPlayer if needed
            }

            Glide.with(this)
                    .load(story.getUserProfilePicUrl())
                    .placeholder(R.drawable.ic_default_profile_placeholder)
                    .into(ivUserProfilePic);

            tvUsername.setText(story.getUsername());
            tvCaption.setText(story.getCaption());

            if (story.getTimestamp() != null) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        story.getTimestamp().toDate().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                tvTimestamp.setText(relativeTime);
            } else {
                tvTimestamp.setText("Just now");
            }
        } else {
            Log.e("StoryPageFragment", "Story is null");
        }

        return view;
    }
}
