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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.Handler; // Import Handler
import android.os.Looper; // Import Looper
import android.app.AlertDialog;

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

    private FloatingActionButton fabQuickCreate;

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
        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        tvHomeTitle = view.findViewById(R.id.tvHomeTitle);
        tvNoPostsMessage = view.findViewById(R.id.tvNoPostsMessage); // Now exists in XML
        fabQuickCreate = view.findViewById(R.id.fabQuickCreate);

        // REMOVED: ivNewPost binding and related logic, as it's no longer needed

        // Setup Posts RecyclerView
        postAdapter = new PostAdapter(postList, this);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPosts.setAdapter(postAdapter);

        // Setup Stories RecyclerView
        storyAdapter = new StoryAdapter(getContext(), storyItemList, this);
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvStories.setAdapter(storyAdapter);

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

        fabQuickCreate.setOnClickListener(v -> showQuickCreateDialog());

        // Load current user's profile picture for *only* ivProfileIcon (top right)
        loadCurrentUserProfilePicForToolbar(); // **RENAMED** and updated

        return view;
    }

    private void showQuickCreateDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_quick_create, null);
        Button btnCreatePost = dialogView.findViewById(R.id.btnCreatePost);
        Button btnAddToStory = dialogView.findViewById(R.id.btnAddToStory);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        btnCreatePost.setOnClickListener(v -> {
            dialog.dismiss();
            // Show dialog for text-only post
            showCreateTextContentDialog(true);
        });
        btnAddToStory.setOnClickListener(v -> {
            dialog.dismiss();
            // Show dialog for text-only story
            showCreateTextContentDialog(false);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
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
                        // Filter out stories older than 24 hours
                        long nowMillis = System.currentTimeMillis();
                        long twentyFourHoursMillis = 24 * 60 * 60 * 1000;
                        List<Story> allFetchedStories = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Story story = doc.toObject(Story.class);
                            if (story != null && story.getTimestamp() != null) {
                                long storyMillis = story.getTimestamp().toDate().getTime();
                                if (nowMillis - storyMillis <= twentyFourHoursMillis) {
                                    story.setId(doc.getId());
                                    allFetchedStories.add(story);
                                }
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
        if (post == null || post.getId() == null) {
            Toast.makeText(getContext(), "Error: Post ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(getContext(), "You must be logged in to like posts.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> likedBy = post.getLikedBy();
        if (likedBy == null) likedBy = new java.util.ArrayList<>();
        boolean isLiked = likedBy.contains(currentUserId);
        // Prepare Firestore update
        com.google.firebase.firestore.DocumentReference postRef = db.collection("posts").document(post.getId());
        if (isLiked) {
            // Unlike: remove user from likedBy and decrement likesCount
            likedBy.remove(currentUserId);
            postRef.update(
                    "likedBy", likedBy,
                    "likesCount", Math.max(0, post.getLikesCount() - 1)
            ).addOnSuccessListener(aVoid ->
                    Toast.makeText(getContext(), "Unliked post!", Toast.LENGTH_SHORT).show()
            ).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to unlike post: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        } else {
            // Like: add user to likedBy and increment likesCount
            likedBy.add(currentUserId);
            postRef.update(
                    "likedBy", likedBy,
                    "likesCount", post.getLikesCount() + 1
            ).addOnSuccessListener(aVoid ->
                    Toast.makeText(getContext(), "Liked post!", Toast.LENGTH_SHORT).show()
            ).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to like post: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    public void onCommentClick(Post post) {
        if (post == null || post.getId() == null) {
            Toast.makeText(getContext(), "Error: Post ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        CommentsBottomSheetFragment.newInstance(post.getId())
                .show(getParentFragmentManager(), "CommentsBottomSheetFragment");
    }

    @Override
    public void onShareClick(Post post) {
        if (post == null || post.getId() == null) {
            Toast.makeText(getContext(), "Error: Post ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        ShareBottomSheetFragment.newInstance(post.getId(), post.getMediaUrl())
                .show(getParentFragmentManager(), "ShareBottomSheetFragment");
    }

    @Override
    public void onSaveClick(Post post) {
        if (post == null || post.getId() == null) {
            Log.d(TAG, "onSaveClick: Post or Post ID is null");
            Toast.makeText(getContext(), "Error: Post ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.d(TAG, "onSaveClick: User not logged in");
            Toast.makeText(getContext(), "You must be logged in to save posts.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Reference to the user's savedPosts array
        com.google.firebase.firestore.DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            List<String> savedPosts = (documentSnapshot.contains("savedPosts") && documentSnapshot.get("savedPosts") != null)
                ? (List<String>) documentSnapshot.get("savedPosts")
                : new ArrayList<>();
            Log.d(TAG, "Current savedPosts before update: " + savedPosts);
            boolean isSaved = savedPosts.contains(post.getId());
            if (isSaved) {
                savedPosts.remove(post.getId());
                Log.d(TAG, "Removing post from saved: " + post.getId());
                userRef.update("savedPosts", savedPosts)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully removed post from saved. New savedPosts: " + savedPosts);
                        Toast.makeText(getContext(), "Post removed from saved.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to unsave post: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to unsave post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            } else {
                savedPosts.add(post.getId());
                Log.d(TAG, "Adding post to saved: " + post.getId());
                userRef.update("savedPosts", savedPosts)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully saved post. New savedPosts: " + savedPosts);
                        Toast.makeText(getContext(), "Post saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to save post: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to save post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
        });
    }

    @Override
    public void onViewCommentsClick(Post post) {
        if (post == null || post.getId() == null) {
            Toast.makeText(getContext(), "Error: Post ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        CommentsBottomSheetFragment.newInstance(post.getId())
                .show(getParentFragmentManager(), "CommentsBottomSheetFragment");
    }

    @Override
    public void onPostOptionsClick(Post post) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean isOwnPost = currentUser != null && post.getUserId() != null && post.getUserId().equals(currentUser.getUid());
        if (isOwnPost) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View dialogView = inflater.inflate(R.layout.dialog_post_options, null);
            androidx.appcompat.widget.AppCompatButton btnEdit = dialogView.findViewById(R.id.btnEditPost);
            androidx.appcompat.widget.AppCompatButton btnDelete = dialogView.findViewById(R.id.btnDeletePost);
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .create();
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                showEditPostDialog(post);
            });
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                deletePost(post);
            });
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        } else {
            // For other users' posts: show report/hide only
            List<String> options = new ArrayList<>();
            options.add("Report Post");
            options.add("Hide Post");
            options.add("View Profile");
            CharSequence[] items = options.toArray(new CharSequence[0]);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Post Options");
            builder.setItems(items, (dialog, which) -> {
                String selected = options.get(which);
                switch (selected) {
                    case "Report Post":
                        Toast.makeText(getContext(), "Report Post (not implemented)", Toast.LENGTH_SHORT).show();
                        break;
                    case "Hide Post":
                        Toast.makeText(getContext(), "Hide Post (not implemented)", Toast.LENGTH_SHORT).show();
                        break;
                    case "View Profile":
                        if (callback != null && post.getUserId() != null) {
                            callback.navigateToProfileFromHome(post.getUserId());
                        } else {
                            Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void showEditPostDialog(Post post) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_edit_post, null);
        EditText etTextContent = dialogView.findViewById(R.id.etEditTextContent);
        androidx.appcompat.widget.AppCompatButton btnSave = dialogView.findViewById(R.id.btnSaveEditPost);
        TextView tvTitle = dialogView.findViewById(R.id.tvEditPostTitle);
        // Pre-fill with current text/caption
        if (post.getText() != null && !post.getText().isEmpty()) {
            etTextContent.setText(post.getText());
        } else if (post.getCaption() != null && !post.getCaption().isEmpty()) {
            etTextContent.setText(post.getCaption());
        }
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        btnSave.setOnClickListener(v -> {
            String newText = etTextContent.getText().toString().trim();
            if (newText.isEmpty()) {
                etTextContent.setError("Please enter some text");
                return;
            }
            dialog.dismiss();
            updatePostText(post, newText);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void updatePostText(Post post, String newText) {
        if (post.getId() == null) {
            Toast.makeText(getContext(), "Error: Post ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("text", newText);
        updates.put("edited", true);
        updates.put("editedAt", com.google.firebase.Timestamp.now());
        db.collection("posts").document(post.getId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Post updated!", Toast.LENGTH_SHORT).show();
                refreshPosts();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to update post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void deletePost(Post post) {
        if (post.getId() == null) {
            Toast.makeText(getContext(), "Error: Post ID is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("posts").document(post.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Post deleted!", Toast.LENGTH_SHORT).show();
                refreshPosts();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to delete post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
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

    private void showCreateTextContentDialog(boolean isPost) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_create_text_content, null);
        EditText etTextContent = dialogView.findViewById(R.id.etTextContent);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitTextContent);
        TextView tvTitle = dialogView.findViewById(R.id.tvCreateTextTitle);
        tvTitle.setText(isPost ? "Create Post" : "Add to Story");
        btnSubmit.setText(isPost ? "Post" : "Add to Story");
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        btnSubmit.setOnClickListener(v -> {
            String text = etTextContent.getText().toString().trim();
            if (text.isEmpty()) {
                etTextContent.setError("Please enter some text");
                return;
            }
            dialog.dismiss();
            if (isPost) {
                createTextOnlyPost(text);
            } else {
                createTextOnlyStory(text);
            }
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void createTextOnlyPost(String text) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        Post post = new Post();
        post.setUserId(user.getUid());
        post.setUsername(user.getDisplayName() != null ? user.getDisplayName() : "");
        post.setProfilePicUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        post.setText(text);
        post.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date()));
        // No mediaUrl for text-only
        db.collection("posts").add(post)
            .addOnSuccessListener(documentReference -> Toast.makeText(getContext(), "Post shared!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to share post: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createTextOnlyStory(String text) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        Story story = new Story();
        story.setUserId(user.getUid());
        story.setUsername(user.getDisplayName() != null ? user.getDisplayName() : "");
        story.setUserProfilePicUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        story.setText(text);
        story.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date()));
        // No mediaUrl for text-only
        db.collection("stories").add(story)
            .addOnSuccessListener(documentReference -> Toast.makeText(getContext(), "Story added!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add story: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}