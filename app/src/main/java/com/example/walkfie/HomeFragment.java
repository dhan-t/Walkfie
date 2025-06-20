package com.example.walkfie;

import static android.content.ContentValues.TAG; // Keep this if you use TAG within this file for logs

import android.content.Context; // IMPORTANT: Add this import for onAttach context
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

// Interface remains the same, it's correctly defined
interface HomeFragmentCallback {
    void navigateToProfileFromHome();
    void navigateToFriendSearchFromHome();
    void navigateToPostCreationFromHome();
    void navigateToStoryCreationFromHome();
    void openPostDetails(PostAdapter.PostItem post);
    void openStoryViewer(List<Story> storiesToView, int startIndex);
}

public class HomeFragment extends Fragment {

    private ImageView ivProfileIcon;
    private RecyclerView rvStories;
    private TextView tvEmptyStoryFeed;

    private HomeFragmentCallback callback; // Reference to HomeActivity

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference storiesRef;
    private CollectionReference usersRef;

    private StoryCircleAdapter storyCircleAdapter;
    private ListenerRegistration storiesListenerRegistration;
    private ListenerRegistration userProfileListenerRegistration;

    private List<StoryCircleItem> storyCircleItemList = new ArrayList<>(); // Holds all story circles

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) { // Changed to @NonNull Context
        super.onAttach(context);
        if (context instanceof HomeFragmentCallback) {
            callback = (HomeFragmentCallback) context;
            Log.d("HomeFragment", "DEBUG_FLOW: HomeFragment attached to Activity. Callback set successfully."); // <-- NEW LOG
        } else {
            Log.e("HomeFragment", "DEBUG_FLOW: Host Activity must implement HomeFragmentCallback"); // <-- NEW LOG
            throw new RuntimeException(context + " must implement HomeFragmentCallback");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storiesRef = db.collection("stories");
        usersRef = db.collection("users");
        Log.d("HomeFragment", "DEBUG_FLOW: HomeFragment onCreate called."); // <-- NEW LOG
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        rvStories = view.findViewById(R.id.rvStories);
        tvEmptyStoryFeed = view.findViewById(R.id.tvEmptyStoryFeed);

        storyCircleAdapter = new StoryCircleAdapter(new StoryCircleAdapter.OnStoryCircleClickListener() {
            @Override
            public void onYourStoryClick() {
                Log.d("HomeFragment", "DEBUG_FLOW: onYourStoryClick triggered in HomeFragment's listener."); // <-- NEW LOG
                boolean yourStoryFound = false;
                for (StoryCircleItem item : storyCircleItemList) {
                    if (item instanceof YourStoryCircleItem) {
                        yourStoryFound = true;
                        List<Story> stories = ((YourStoryCircleItem) item).getStories();
                        Log.d("HomeFragment", "DEBUG_FLOW: YourStoryCircleItem found. Stories count: " + (stories != null ? stories.size() : "null")); // <-- NEW LOG
                        if (callback != null) { // Always check callback before using
                            if (stories != null && !stories.isEmpty()) {
                                Log.d("HomeFragment", "DEBUG_FLOW: Calling openStoryViewer with " + stories.size() + " stories."); // <-- NEW LOG
                                callback.openStoryViewer(stories, 0);
                            } else {
                                Log.d("HomeFragment", "DEBUG_FLOW: No stories found for Your Story. Calling navigateToStoryCreationFromHome."); // <-- NEW LOG
                                callback.navigateToStoryCreationFromHome();
                            }
                        } else {
                            Log.e("HomeFragment", "DEBUG_FLOW: Callback is NULL when trying to open story viewer/creator."); // <-- NEW LOG
                            Toast.makeText(getContext(), "Error: HomeActivity link missing.", Toast.LENGTH_SHORT).show();
                        }
                        break; // Important: Exit loop once "Your Story" is found and handled
                    }
                }
                if (!yourStoryFound) {
                    Log.w("HomeFragment", "DEBUG_FLOW: YourStoryCircleItem not found in list. This might be an issue with data loading."); // <-- NEW LOG
                    if (callback != null) {
                        // Fallback: If YourStoryCircleItem wasn't found at all, navigate to creation.
                        callback.navigateToStoryCreationFromHome();
                    }
                }
            }

            @Override
            public void onFriendStoryClick(List<Story> stories, int startIndex) {
                Log.d("HomeFragment", "DEBUG_FLOW: onFriendStoryClick triggered. Stories count: " + (stories != null ? stories.size() : "null")); // <-- NEW LOG
                if (callback != null) { // Always check callback before using
                    if (stories != null && !stories.isEmpty()) {
                        Log.d("HomeFragment", "DEBUG_FLOW: Calling openStoryViewer for friend stories."); // <-- NEW LOG
                        callback.openStoryViewer(stories, startIndex);
                    } else {
                        Log.w("HomeFragment", "DEBUG_FLOW: No stories provided for friend story click."); // <-- NEW LOG
                        Toast.makeText(getContext(), "No stories available for this friend.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("HomeFragment", "DEBUG_FLOW: Callback is NULL when trying to open friend story viewer."); // <-- NEW LOG
                    Toast.makeText(getContext(), "Error: HomeActivity link missing.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvStories.setAdapter(storyCircleAdapter);

        ivProfileIcon.setOnClickListener(v -> {
            Log.d("HomeFragment", "DEBUG_FLOW: Profile icon clicked."); // <-- NEW LOG
            if (callback != null) {
                callback.navigateToProfileFromHome();
            } else {
                Log.e("HomeFragment", "DEBUG_FLOW: Callback is NULL when navigating to profile."); // <-- NEW LOG
            }
        });

        Log.d("HomeFragment", "DEBUG_FLOW: HomeFragment onCreateView finished."); // <-- NEW LOG
        return view;
    }

    public void refreshStories() {
        Log.d("HomeFragment", "DEBUG_FLOW: refreshStories called."); // <-- NEW LOG
        if (storyCircleAdapter == null) {
            Log.w("HomeFragment", "DEBUG_FLOW: refreshStories called but storyCircleAdapter is null.");
            return;
        }
        loadStoryCircles(); // Clears and reloads adapter, but doesn't refetch
        listenForStories(); // This is what fetches new data
    }

    public void clearStories() {
        Log.d("HomeFragment", "DEBUG_FLOW: clearStories called."); // <-- NEW LOG
        storyCircleItemList.clear();
        if (storyCircleAdapter != null) {
            storyCircleAdapter.clearStories();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("HomeFragment", "DEBUG_FLOW: HomeFragment onStart called. Setting up listeners."); // <-- NEW LOG
        listenForUserProfilePicture();
        listenForStories();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("HomeFragment", "DEBUG_FLOW: HomeFragment onStop called. Removing listeners."); // <-- NEW LOG
        if (storiesListenerRegistration != null) storiesListenerRegistration.remove();
        if (userProfileListenerRegistration != null) userProfileListenerRegistration.remove();
    }

    private void loadStoryCircles() {
        Log.d("HomeFragment", "DEBUG_FLOW: loadStoryCircles called. Clearing list."); // <-- NEW LOG
        storyCircleItemList.clear();
        storyCircleAdapter.setStoryCircleItems(storyCircleItemList); // This will update the adapter with an empty list initially
        storyCircleAdapter.notifyDataSetChanged();
    }

    private void listenForUserProfilePicture() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
            Log.w("HomeFragment", "DEBUG_FLOW: No current user for profile picture listener."); // <-- NEW LOG
            return;
        }

        userProfileListenerRegistration = usersRef.document(user.getUid())
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) {
                        ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
                        Log.e("HomeFragment", "DEBUG_FLOW: Error or no user profile doc: " + (e != null ? e.getMessage() : "null/empty doc")); // <-- NEW LOG
                        return;
                    }

                    User profile = doc.toObject(User.class);
                    if (profile != null && profile.getProfilePicUrl() != null) {
                        Glide.with(this)
                                .load(profile.getProfilePicUrl())
                                .apply(RequestOptions.circleCropTransform())
                                .into(ivProfileIcon);
                        Log.d("HomeFragment", "DEBUG_FLOW: Profile picture loaded."); // <-- NEW LOG
                    } else {
                        ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
                        Log.w("HomeFragment", "DEBUG_FLOW: Profile pic URL is null or user object is null."); // <-- NEW LOG
                    }
                });
    }

    private void listenForStories() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w("HomeFragment", "DEBUG_FLOW: No current user for stories listener."); // <-- NEW LOG
            return;
        }

        long twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        Timestamp cutoff = new Timestamp(new Date(twentyFourHoursAgo));

        storiesListenerRegistration = storiesRef
                .whereGreaterThanOrEqualTo("timestamp", cutoff)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e(TAG, "DEBUG_FLOW: Error loading stories: " + (e != null ? e.getMessage() : "null snapshot")); // <-- MODIFIED LOG
                        showOnlyYourStory(currentUser.getUid()); // Fallback
                        return;
                    }

                    List<Story> fetchedStories = new ArrayList<>();
                    Log.d(TAG, "DEBUG_FLOW: Fetched stories count from Firestore: " + snapshots.size()); // <-- MODIFIED LOG
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Log.d(TAG, "DEBUG_FLOW: Story doc: " + doc.getId() + " => " + doc.getData()); // <-- MODIFIED LOG
                        Story story = doc.toObject(Story.class);
                        story.setId(doc.getId());
                        if (story.getMediaUrl() != null && !story.getMediaUrl().isEmpty()) {
                            fetchedStories.add(story);
                        }
                    }

                    organizeAndDisplayStories(fetchedStories, currentUser.getUid());
                });
    }

    private void showOnlyYourStory(String userId) {
        Log.d("HomeFragment", "DEBUG_FLOW: showOnlyYourStory called."); // <-- NEW LOG
        usersRef.document(userId).get().addOnSuccessListener(doc -> {
            String profilePicUrl = "";
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null) profilePicUrl = user.getProfilePicUrl();
                Log.d("HomeFragment", "DEBUG_FLOW: User profile found for showOnlyYourStory."); // <-- NEW LOG
            } else {
                Log.w("HomeFragment", "DEBUG_FLOW: User profile not found for showOnlyYourStory."); // <-- NEW LOG
            }

            YourStoryCircleItem yourStory = new YourStoryCircleItem("Your Story", profilePicUrl, new ArrayList<>());
            storyCircleAdapter.setStoryCircleItems(Collections.singletonList(yourStory));
            updateStoryVisibility(1);
        }).addOnFailureListener(e -> {
            Log.e("HomeFragment", "DEBUG_FLOW: Error fetching user for showOnlyYourStory: " + e.getMessage()); // <-- NEW LOG
            // Handle error, e.g., show a placeholder
            YourStoryCircleItem yourStory = new YourStoryCircleItem("Your Story", "", new ArrayList<>());
            storyCircleAdapter.setStoryCircleItems(Collections.singletonList(yourStory));
            updateStoryVisibility(1);
        });
    }


    private void organizeAndDisplayStories(List<Story> allStories, String currentUserId) {
        Log.d("HomeFragment", "DEBUG_FLOW: organizeAndDisplayStories called with " + allStories.size() + " stories."); // <-- NEW LOG
        Map<String, List<Story>> grouped = new HashMap<>();
        for (Story story : allStories) {
            grouped.computeIfAbsent(story.getUserId(), k -> new ArrayList<>()).add(story);
        }

        usersRef.get().addOnSuccessListener(snapshot -> {
            List<StoryCircleItem> finalItems = new ArrayList<>();

            for (DocumentSnapshot doc : snapshot) {
                String uid = doc.getId();
                User user = doc.toObject(User.class);
                if (user == null) continue;

                String name = uid.equals(currentUserId) ? "Your Story" : user.getUsername();
                String pic = user.getProfilePicUrl();
                List<Story> stories = grouped.getOrDefault(uid, new ArrayList<>());

                if (uid.equals(currentUserId)) {
                    finalItems.add(new YourStoryCircleItem(name, pic, stories));
                } else {
                    assert stories != null; // Added assert, though check for isEmpty is typically enough
                    if (!stories.isEmpty()) {
                        finalItems.add(new FriendStoryCircleItem(name, pic, uid, stories));
                    }
                }
            }

            for (StoryCircleItem item : finalItems) {
                Log.d(TAG, "DEBUG_FLOW: Final StoryCircleItem: " + item.getUsername() + " - type: " + item.getType() + " - stories: " + item.getStories().size()); // <-- MODIFIED LOG
            }

            storyCircleItemList.clear(); // Clear before adding to avoid duplicates
            storyCircleItemList.addAll(finalItems); // Update the fragment's member list
            storyCircleAdapter.setStoryCircleItems(storyCircleItemList); // Pass the updated list to the adapter
            updateStoryVisibility(storyCircleItemList.size()); // Use the size of the updated list
            Log.d("HomeFragment", "DEBUG_FLOW: Stories organized and adapter updated. Total items: " + storyCircleItemList.size()); // <-- NEW LOG

        }).addOnFailureListener(e -> {
            Log.e("HomeFragment", "DEBUG_FLOW: Error fetching users for organizing stories: " + e.getMessage()); // <-- NEW LOG
            // Handle error, maybe just display current user's story or nothing
            showOnlyYourStory(currentUserId);
        });
    }

    private void updateStoryVisibility(int count) {
        Log.d("HomeFragment", "DEBUG_FLOW: updateStoryVisibility called with count: " + count); // <-- NEW LOG
        if (count == 0 || (count == 1 && (storyCircleItemList.get(0) instanceof YourStoryCircleItem) && storyCircleItemList.get(0).getStories().isEmpty())) {
            // Only 'Your Story' circle exists and it has no stories
            tvEmptyStoryFeed.setVisibility(View.VISIBLE);
            rvStories.setVisibility(View.GONE);
        } else {
            tvEmptyStoryFeed.setVisibility(View.GONE);
            rvStories.setVisibility(View.VISIBLE);
        }
    }

    // This method is already called in onAttach, so it's technically redundant to call it explicitly
    // from outside the fragment for setup, but might be useful if you change the callback later.
    public void setHomeFragmentCallback(HomeFragmentCallback callback) {
        this.callback = callback;
        Log.d("HomeFragment", "DEBUG_FLOW: setHomeFragmentCallback called externally. Callback set."); // <-- NEW LOG
    }
}