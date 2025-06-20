package com.example.walkfie;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity; // Needed if adapter constructor uses activity
import androidx.viewpager2.adapter.FragmentStateAdapter; // Import this
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StoryViewerFragment extends Fragment {

    private static final String ARG_STORIES_LIST = "stories_list";
    private static final String ARG_START_INDEX = "start_index";

    private List<Story> stories;
    private int startIndex;

    private ViewPager2 storyViewPager;
    private StoryPagerAdapter storyPagerAdapter; // Declare it here
    private ImageButton btnClose;
    private TextView tvNoStories; // Reference to your no stories text view

    public interface StoryViewerCallback {
        void onStoryViewerClosed();
    }
    private StoryViewerCallback callback;


    public StoryViewerFragment() {
        // Required empty public constructor
    }

    public static StoryViewerFragment newInstance(ArrayList<Story> stories, int startIndex) {
        Log.d("StoryViewerFragment", "DEBUG_FLOW: newInstance called. Stories to pass: " + (stories != null ? stories.size() : "null"));
        StoryViewerFragment fragment = new StoryViewerFragment();
        Bundle args = new Bundle();
        // Ensure stories list is not null, even if empty
        args.putParcelableArrayList(ARG_STORIES_LIST, stories != null ? stories : new ArrayList<>());
        args.putInt(ARG_START_INDEX, startIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d("StoryViewerFragment", "DEBUG_FLOW: onAttach called.");
        if (context instanceof StoryViewerCallback) {
            callback = (StoryViewerCallback) context;
            Log.d("StoryViewerFragment", "DEBUG_FLOW: StoryViewerCallback set successfully.");
        } else {
            Log.w("StoryViewerFragment", "DEBUG_FLOW: Host Activity does not implement StoryViewerCallback. This is a warning.");
            // Don't throw a RuntimeException if the callback is optional for this fragment
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("StoryViewerFragment", "DEBUG_FLOW: onCreate called!");

        if (getArguments() != null) {
            stories = getArguments().getParcelableArrayList(ARG_STORIES_LIST);
            startIndex = getArguments().getInt(ARG_START_INDEX, 0);
            Log.d("StoryViewerFragment", "DEBUG_FLOW: Received " + (stories != null ? stories.size() : 0) + " stories and startIndex: " + startIndex);
        } else {
            Log.e("StoryViewerFragment", "DEBUG_FLOW: No arguments provided for StoryViewerFragment! Initializing empty stories.");
            stories = new ArrayList<>();
            startIndex = 0;
        }

        if (stories == null || stories.isEmpty()) {
            Log.w("StoryViewerFragment", "DEBUG_FLOW: Stories list is null or empty in onCreate. Setting up to show 'No stories'.");
            // No need for Toast here, as onCreateView will handle the UI for no stories.
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("StoryViewerFragment", "DEBUG_FLOW: onCreateView called!");
        View view = inflater.inflate(R.layout.fragment_story_viewer, container, false);

        storyViewPager = view.findViewById(R.id.storyViewPager);
        btnClose = view.findViewById(R.id.btnCloseStoryViewer);
        tvNoStories = view.findViewById(R.id.tvNoStories); // Initialize tvNoStories

        if (stories != null && !stories.isEmpty()) {
            // Instantiate the StoryPagerAdapter with 'this' fragment and its lifecycle
            storyPagerAdapter = new StoryPagerAdapter(this, stories);
            storyViewPager.setAdapter(storyPagerAdapter);
            storyViewPager.setCurrentItem(startIndex, false); // Set initial story without smooth scroll

            storyViewPager.setVisibility(View.VISIBLE);
            tvNoStories.setVisibility(View.GONE); // Hide no stories text
            Log.d("StoryViewerFragment", "DEBUG_FLOW: Story Pager Adapter set with " + stories.size() + " stories.");
        } else {
            storyViewPager.setVisibility(View.GONE);
            tvNoStories.setVisibility(View.VISIBLE); // Show no stories text
            tvNoStories.setText("No stories to show.");
            Log.w("StoryViewerFragment", "DEBUG_FLOW: Stories list is empty in onCreateView. Hiding ViewPager and showing 'No stories' message.");
        }

        btnClose.setOnClickListener(v -> {
            Log.d("StoryViewerFragment", "DEBUG_FLOW: Close button clicked.");
            if (callback != null) {
                callback.onStoryViewerClosed();
            } else {
                // If no callback, try to pop the fragment directly
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                } else {
                    Log.e("StoryViewerFragment", "Cannot pop back stack, getParentFragmentManager is null.");
                }
            }
        });

        Log.d("StoryViewerFragment", "DEBUG_FLOW: onCreateView finished.");
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null; // Clear the callback to prevent memory leaks
        Log.d("StoryViewerFragment", "DEBUG_FLOW: onDetach called. Callback cleared.");
    }

    /**
     * Inner class for ViewPager2 Adapter.
     */
    private static class StoryPagerAdapter extends FragmentStateAdapter {
        private final List<Story> stories;

        // Constructor for FragmentStateAdapter used within a Fragment
        public StoryPagerAdapter(@NonNull Fragment fragment, List<Story> stories) {
            super(fragment); // Pass the Fragment to the super constructor
            this.stories = stories;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return a new instance of StoryPageFragment for each position
            return StoryPageFragment.newInstance(stories.get(position));
        }

        @Override
        public int getItemCount() {
            // Return the total number of stories
            return stories.size();
        }
    }
}