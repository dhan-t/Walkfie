// StoryPagerAdapter.java
package com.example.walkfie;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class StoryPagerAdapter extends FragmentStateAdapter {

    private final List<Story> stories;

    public StoryPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, List<Story> stories) {
        super(fragmentManager, lifecycle); // ✅ this constructor
        this.stories = stories;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return StoryPageFragment.newInstance(stories.get(position));
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }
}
