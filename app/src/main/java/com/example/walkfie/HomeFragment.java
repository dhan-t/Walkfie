package com.example.walkfie;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.Handler; // Import Handler
import android.os.Looper; // Import Looper

public class HomeFragment extends Fragment implements PostAdapter.OnPostInteractionListener, StoryAdapter.OnStoryClickListener {

    private static final String TAG = "HomeFragment";

    public interface HomeFragmentCallback {
        // Keeping old ones for compatibility, but focusing on new ones for clarity
        void navigateToPostCreation();
        void navigateToFriendSearch();
        void navigateToStoryCreation();

        void navigateToProfileFromHome(String userId);
        void openPostDetails(Post post);
        void openStoryViewer(List<Story> storiesToView, int startIndex);

        // Specific HomeFragment-related callbacks
        void navigateToStoryCreationFromHome(); // For adding a new story
        void navigateToPostCreationFromHome();
        void navigateToFriendSearchFromHome();
    }

    private HomeFragmentCallback callback;

    // UI Components
    private RecyclerView rvStories;
    private RecyclerView rvPosts;
    private ProgressBar postsProgressBar; // Declared
    private ImageView ivNewPost; // For the '+' icon to create a post
    private ImageView ivProfileIcon; // For the user's own profile icon in the toolbar
    private TextView tvHomeTitle; // The "Home" title text
    private TextView tvNoPostsMessage; // Added to show when no posts are available
    // REMOVED: private ImageView ivYourStoryProfilePic; // This is now handled by StoryAdapter

    // Adapters
    private PostAdapter postAdapter;
    private List<Post> postList;

    private StoryAdapter storyAdapter;
    private List<StoryAdapter.StoryItem> storyItemList;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration postsListenerRegistration;
    private ListenerRegistration storiesListenerRegistration;
    private ListenerRegistration currentUserProfileListenerRegistration; // For current user's top-right profile pic

    // Handler for delayed UI updates (for demonstration purposes)
    private final Handler handler = new Handler(Looper.getMainLooper());

    public HomeFragment() {
        // Required empty public constructor
    }

    public void setHomeFragmentCallback(HomeFragmentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeFragmentCallback) {
            callback = (HomeFragmentCallback) context;
        } else {
            Log.w(TAG, "Host Activity does not implement HomeFragmentCallback.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        postList = new ArrayList<>();
        storyItemList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Bind UI components
        rvStories = view.findViewById(R.id.rvStories);
        rvPosts = view.findViewById(R.id.rvPosts);
        postsProgressBar = view.findViewById(R.id.postsProgressBar); // Initialized
        ivNewPost = view.findViewById(R.id.ivNewPost);
        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        tvHomeTitle = view.findViewById(R.id.tvHomeTitle);
        tvNoPostsMessage = view.findViewById(R.id.tvNoPostsMessage); // Now exists in XML

        // REMOVED: ivYourStoryProfilePic binding, it's no longer a direct element in HomeFragment's XML

        // Setup Posts RecyclerView
        postAdapter = new PostAdapter(postList, this);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPosts.setAdapter(postAdapter);

        // Setup Stories RecyclerView
        storyAdapter = new StoryAdapter(getContext(), storyItemList, this);
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvStories.setAdapter(storyAdapter);

        // Set listeners for toolbar buttons
        ivNewPost.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToPostCreationFromHome();
            }
        });

        // Set listener for profile icon (top right)
        ivProfileIcon.setOnClickListener(v -> {
            if (callback != null) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    callback.navigateToProfileFromHome(currentUser.getUid());
                } else {
                    Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // REMOVED: Click listener for ivYourStoryProfilePic as it's no longer a standalone ImageView here.
        // The StoryAdapter will handle clicks for "Your Story" item within rvStories.

        // Load current user's profile picture for *only* ivProfileIcon (top right)
        loadCurrentUserProfilePicForToolbar(); // **RENAMED** and updated

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshPosts(); // Will now show spinner
        refreshStories();
        loadCurrentUserProfilePicForToolbar(); // Ensure toolbar profile pic is loaded
    }

    @Override
    public void onStop() {
        super.onStop();
        // Crucial: Remove any pending callbacks when the fragment stops
        handler.removeCallbacksAndMessages(null);

        if (postsListenerRegistration != null) {
            postsListenerRegistration.remove();
            postsListenerRegistration = null;
            Log.d(TAG, "Firestore posts listener removed.");
        }
        if (storiesListenerRegistration != null) {
            storiesListenerRegistration.remove();
            storiesListenerRegistration = null;
            Log.d(TAG, "Firestore stories listener removed.");
        }
        if (currentUserProfileListenerRegistration != null) {
            currentUserProfileListenerRegistration.remove();
            currentUserProfileListenerRegistration = null;
            Log.d(TAG, "Firestore current user profile listener removed.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Crucial: Remove any pending callbacks when the fragment is detached
        handler.removeCallbacksAndMessages(null);
        callback = null;
    }

    /**
     * Loads the current user's profile picture into the top-right profile icon only.
     * The "Your Story" profile picture is handled by the StoryAdapter.
     */
    private void loadCurrentUserProfilePicForToolbar() { // **RENAMED** and updated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUserProfileListenerRegistration != null) {
                currentUserProfileListenerRegistration.remove();
            }

            currentUserProfileListenerRegistration = db.collection("users").document(currentUser.getUid())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Error listening for current user profile pic (toolbar): " + e.getMessage());
                            ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String profilePicUrl = documentSnapshot.getString("profilePicUrl"); // Assuming this is correct
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePicUrl)
                                        .apply(new RequestOptions().placeholder(R.drawable.ic_default_profile_placeholder).error(R.drawable.ic_default_profile_placeholder))
                                        .into(ivProfileIcon);
                                Log.d(TAG, "HomeFragment: Successfully loaded current user profile pic for toolbar from " + profilePicUrl);
                            } else {
                                Log.d(TAG, "HomeFragment: Current user profile pic URL is empty or null (toolbar), using default.");
                                ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
                            }
                        } else {
                            Log.d(TAG, "HomeFragment: Current user profile document does not exist (toolbar), using default placeholder.");
                            ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
                        }
                    });
        } else {
            Log.d(TAG, "HomeFragment: No current user, displaying default profile placeholder for toolbar.");
            ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
        }
    }


    public void refreshPosts() {
        Log.d(TAG, "Refreshing posts...");

        // --- START OF SPINNER ANIMATION LOGIC (FORCED DELAY FOR DEMO) ---
        // Show ProgressBar, hide RecyclerView and No Posts message
        postsProgressBar.setVisibility(View.VISIBLE);
        rvPosts.setVisibility(View.GONE);
        tvNoPostsMessage.setVisibility(View.GONE);
        // --- END OF SPINNER ANIMATION LOGIC ---

        if (postsListenerRegistration != null) {
            postsListenerRegistration.remove();
        }

        postsListenerRegistration = db.collection("posts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    // --- IMPORTANT CHANGE FOR FORCED DELAY ---
                    // Wrap the post-loading UI update in a delayed runnable
                    handler.postDelayed(() -> {
                        // This block will execute AFTER the delay

                        // Hide ProgressBar when fetching is complete, regardless of success or failure
                        postsProgressBar.setVisibility(View.GONE);

                        if (error != null) {
                            Log.e(TAG, "Listen failed for posts.", error);
                            Toast.makeText(getContext(), "Error loading posts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            rvPosts.setVisibility(View.GONE);
                            tvNoPostsMessage.setText("Error loading posts.");
                            tvNoPostsMessage.setVisibility(View.VISIBLE);
                            return;
                        }

                        if (value != null) {
                            Log.d(TAG, "postList size BEFORE clear (HomeFragment): " + postList.size());
                            List<Post> tempPostList = new ArrayList<>();

                            Log.d(TAG, "Received " + value.size() + " documents from Firestore.");
                            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                                Log.d(TAG, "Processing document ID: " + doc.getId());
                                Log.d(TAG, "Document data: " + doc.getData());

                                try {
                                    Post post = doc.toObject(Post.class);
                                    if (post != null) {
                                        post.setId(doc.getId());
                                        tempPostList.add(post);
                                        Log.d(TAG, "Successfully deserialized Post: " + post.getCaption() + " (ID: " + post.getId() + ")");
                                    } else {
                                        Log.w(TAG, "Failed to deserialize document ID: " + doc.getId() + " to Post object. toObject returned null.");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error deserializing document ID: " + doc.getId() + " to Post object.", e);
                                }
                            }

                            postList.clear();
                            postList.addAll(tempPostList);


                            postAdapter.updatePosts(new ArrayList<>(postList));
                            Log.d(TAG, "Posts updated. Total posts: " + postList.size());

                            // --- START OF SPINNER ANIMATION LOGIC ---
                            // Update UI based on fetched data
                            if (postList.isEmpty()) {
                                rvPosts.setVisibility(View.GONE);
                                tvNoPostsMessage.setText("No posts yet. Be the first to share!");
                                tvNoPostsMessage.setVisibility(View.VISIBLE);
                            } else {
                                rvPosts.setVisibility(View.VISIBLE);
                                tvNoPostsMessage.setVisibility(View.GONE);
                            }
                            // --- END OF SPINNER ANIMATION LOGIC ---
                        } else {
                            Log.d(TAG, "Current data snapshot is null.");
                            // --- START OF SPINNER ANIMATION LOGIC ---
                            rvPosts.setVisibility(View.GONE);
                            tvNoPostsMessage.setText("No posts available.");
                            tvNoPostsMessage.setVisibility(View.VISIBLE);
                            // --- END OF SPINNER ANIMATION LOGIC ---
                        }
                    }, 2500); // <-- 2500 milliseconds = 2.5 seconds delay (Adjust this value as needed)
                });
    }

    /**
     * Refreshes stories. Fetches stories from Firestore and updates the RecyclerView.
     * This method now also passes the current user's profile pic URL to the StoryItem
     * for "Your Story" directly.
     */
    public void refreshStories() {
        Log.d(TAG, "Refreshing stories...");

        if (storiesListenerRegistration != null) {
            storiesListenerRegistration.remove();
        }

        storiesListenerRegistration = db.collection("stories")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed for stories.", error);
                        Toast.makeText(getContext(), "Error loading stories: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        Log.d(TAG, "Received " + value.size() + " story documents from Firestore.");
                        List<Story> allFetchedStories = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                Story story = doc.toObject(Story.class);
                                if (story != null) {
                                    story.setId(doc.getId());
                                    allFetchedStories.add(story);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error deserializing story document: " + doc.getId(), e);
                            }
                        }

                        List<StoryAdapter.StoryItem> tempStoryItemList = new ArrayList<>();
                        FirebaseUser currentUser = mAuth.getCurrentUser();

                        // Add "Your Story" first (if current user exists)
                        if (currentUser != null) {
                            // Fetch current user's profile pic URL for their story item
                            db.collection("users").document(currentUser.getUid()).get()
                                    .addOnSuccessListener(userDoc -> {
                                        String yourProfilePicUrl = null;
                                        if (userDoc.exists()) {
                                            yourProfilePicUrl = userDoc.getString("profilePicUrl"); // Assuming this is correct
                                        }

                                        StoryAdapter.StoryItem yourStoryItem = StoryAdapter.StoryItem.createYourStoryItem(
                                                currentUser.getUid(),
                                                "Your Story", // Default username for your story
                                                yourProfilePicUrl // Pass the actual profile pic URL
                                        );
                                        for (Story story : allFetchedStories) {
                                            if (story.getUserId().equals(currentUser.getUid())) {
                                                yourStoryItem.addStory(story);
                                            }
                                        }
                                        Collections.sort(yourStoryItem.getUserStories(), (s1, s2) -> s2.getTimestamp().compareTo(s1.getTimestamp()));
                                        tempStoryItemList.add(0, yourStoryItem); // Add at the beginning

                                        // Now add friend stories and update the adapter
                                        addFriendStoriesAndSort(tempStoryItemList, allFetchedStories, currentUser);
                                        storyAdapter.updateStories(tempStoryItemList);
                                        Log.d(TAG, "Stories updated (with current user profile pic). Total story circles: " + tempStoryItemList.size());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to get current user profile for story item: " + e.getMessage());
                                        // Proceed without user profile pic for your story if fetch fails
                                        StoryAdapter.StoryItem yourStoryItem = StoryAdapter.StoryItem.createYourStoryItem(
                                                currentUser.getUid(), "Your Story", null);
                                        for (Story story : allFetchedStories) {
                                            if (story.getUserId().equals(currentUser.getUid())) {
                                                yourStoryItem.addStory(story);
                                            }
                                        }
                                        Collections.sort(yourStoryItem.getUserStories(), (s1, s2) -> s2.getTimestamp().compareTo(s1.getTimestamp()));
                                        tempStoryItemList.add(0, yourStoryItem);

                                        addFriendStoriesAndSort(tempStoryItemList, allFetchedStories, currentUser);
                                        storyAdapter.updateStories(tempStoryItemList);
                                        Log.d(TAG, "Stories updated (without current user profile pic). Total story circles: " + tempStoryItemList.size());
                                    });
                        } else {
                            // If no current user, just add friend stories
                            addFriendStoriesAndSort(tempStoryItemList, allFetchedStories, currentUser);
                            storyAdapter.updateStories(tempStoryItemList);
                            Log.d(TAG, "Stories updated (no current user). Total story circles: " + tempStoryItemList.size());
                        }

                    } else {
                        Log.d(TAG, "Current story data snapshot is null.");
                    }
                });
    }

    // Helper method to consolidate friend story logic and sorting
    private void addFriendStoriesAndSort(List<StoryAdapter.StoryItem> tempStoryItemList, List<Story> allFetchedStories, FirebaseUser currentUser) {
        for (Story story : allFetchedStories) {
            if (currentUser == null || !story.getUserId().equals(currentUser.getUid())) {
                boolean found = false;
                for (StoryAdapter.StoryItem item : tempStoryItemList) {
                    if (item.getUserId().equals(story.getUserId())) {
                        item.addStory(story);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    StoryAdapter.StoryItem friendStoryItem = StoryAdapter.StoryItem.createFriendStoryItem(
                            story.getUserId(),
                            story.getUsername(),
                            story.getUserProfilePicUrl()
                    );
                    friendStoryItem.addStory(story);
                    tempStoryItemList.add(friendStoryItem);
                }
            }
        }

        // Sort friend stories (stories added by timestamp within each user's item,
        // here we sort the StoryItem list itself, keeping "Your Story" first)
        Collections.sort(tempStoryItemList, (item1, item2) -> {
            // Ensure "Your Story" always comes first
            if (item1.getType() == StoryAdapter.StoryType.YOUR_STORY && item2.getType() != StoryAdapter.StoryType.YOUR_STORY) return -1;
            if (item2.getType() == StoryAdapter.StoryType.YOUR_STORY && item1.getType() != StoryAdapter.StoryType.YOUR_STORY) return 1;

            // For other stories, sort by the timestamp of their most recent story
            if (item1.getUserStories() != null && !item1.getUserStories().isEmpty() &&
                    item2.getUserStories() != null && !item2.getUserStories().isEmpty()) {
                // Assuming getTimestamp() returns com.google.firebase.Timestamp, which has compareTo
                return item2.getUserStories().get(0).getTimestamp().compareTo(item1.getUserStories().get(0).getTimestamp());
            }
            return 0;
        });
    }


    // --- PostAdapter.OnPostInteractionListener Implementations ---
    @Override
    public void onProfileClick(Post post) {
        Toast.makeText(getContext(), "Clicked profile: " + post.getUsername(), Toast.LENGTH_SHORT).show();
        if (callback != null) {
            callback.navigateToProfileFromHome(post.getUserId());
        }
    }

    @Override
    public void onLikeClick(Post post) {
        Toast.makeText(getContext(), "Liked post: " + post.getId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCommentClick(Post post) {
        Toast.makeText(getContext(), "Clicked comment on post: " + post.getId(), Toast.LENGTH_SHORT).show();
        if (callback != null) {
            callback.openPostDetails(post);
        }
    }

    @Override
    public void onShareClick(Post post) {
        Toast.makeText(getContext(), "Shared post: " + post.getId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveClick(Post post) {
        Toast.makeText(getContext(), "Saved post: " + post.getId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewCommentsClick(Post post) {
        Toast.makeText(getContext(), "View all comments for post: " + post.getId(), Toast.LENGTH_SHORT).show();
        if (callback != null) {
            callback.openPostDetails(post);
        }
    }

    @Override
    public void onPostOptionsClick(Post post) {
        Toast.makeText(getContext(), "Clicked options for post: " + post.getId(), Toast.LENGTH_SHORT).show();
    }

    // --- StoryAdapter.OnStoryClickListener Implementations ---
    // This is called when a user clicks on the "Your Story" circle in the story RecyclerView
    // This should now correctly navigate to *creation* if it's the "add" button, or *viewing* if it's an existing story.
    @Override
    public void onYourStoryClick() {
        Log.d(TAG, "onYourStoryClick: User wants to view/add their story.");
        if (callback != null) {
            // This method is primarily triggered when the 'Your Story' item is clicked
            // and no stories exist, or the explicit "add" area within it is tapped.
            // The StoryAdapter should handle distinguishing between viewing existing stories
            // and navigating to creation. For creating, onUserStoryClickToAdd is more specific.
            // If this is the only entry point from a truly empty 'Your Story' state, then:
            // callback.navigateToStoryCreationFromHome();
        }
    }

    // This is called when a user clicks on a friend's story circle in the story RecyclerView
    @Override
    public void onFriendStoryClick(String userId) {
        Log.d(TAG, "onFriendStoryClick: " + userId + ". This is usually for opening story viewer.");
        // This callback is likely redundant if onStoryClick handles actual viewing.
        // The StoryAdapter should pass the list of stories for this user directly to onStoryClick.
    }

    // This is the primary callback for viewing stories, triggered by both "Your Story" and friend stories.
    @Override
    public void onStoryClick(List<Story> storiesToView, int startIndex) {
        Log.d(TAG, "HomeFragment: onStoryClick triggered. Stories to view: " + storiesToView.size() + ", Start index: " + startIndex);
        if (callback != null) {
            callback.openStoryViewer(storiesToView, startIndex);
        } else {
            Log.e(TAG, "HomeFragment: Callback is null. Cannot open story viewer.");
            Toast.makeText(getContext(), "Error: Story viewer not available.", Toast.LENGTH_SHORT).show();
        }
    }

    // This is specifically for clicking on the "+" icon or empty "Your Story" circle to add a new story.
    // This is the correct entry point for story creation from the story reel.
    @Override
    public void onUserStoryClickToAdd() {
        Log.d(TAG, "onUserStoryClickToAdd: Add new story for current user (from adapter).");
        if (callback != null) {
            callback.navigateToStoryCreationFromHome();
        }
    }
}