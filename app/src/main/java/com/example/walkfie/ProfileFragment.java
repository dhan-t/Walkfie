package com.example.walkfie;

import static android.content.ContentValues.TAG; // Keep this import if you're using android.content.ContentValues.TAG

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections; // Added for sorting
import java.util.Date;
import java.util.List;


// Define a callback interface for actions in ProfileFragment
// Your interface looks fine, assuming HomeActivity implements all these.
// The key is that `openPostDetails` and `openStoryViewer` already accept the correct types.
interface ProfileFragmentCallback {
    void navigateToProfileEdit();
    void navigateToSettings();
    void navigateToFullScreenMap();
    void navigateToPostCreation();
    void navigateToFriendSearch();
    void navigateToStoryCreation();
    void onBackPressFromProfile();
    void openPostDetails(Post post);
    void openStoryViewer(List<Story> storiesToView, int startIndex);
    void openFriendProfile(FriendsAdapter.FriendItem friend);
}

public class ProfileFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_USER_ID = "userId"; // Constant for argument key
    private String displayUserId; // The userId whose profile is being displayed

    private ImageView ivBackArrow;
    private ImageView ivSettings;
    private ImageView ivProfilePicture;
    private TextView tvUserName;
    private TextView tvUserDescription;
    private Button btnEditProfile;

    // Friends section
    private TextView tvFriendsCount;
    private RecyclerView rvFriends;
    private TextView tvNoFriendsMessage;
    private Button btnAddFriends;
    private FriendsAdapter friendsAdapter;
    private List<FriendsAdapter.FriendItem> friendsList;

    // Stories section
    private RecyclerView rvStories;
    private TextView tvNoStoriesMessage;
    private Button btnAddStory;
    private ProfileStoriesAdapter profileStoriesAdapter;
    private List<Story> storiesList;

    // Map section
    private MapView miniMapView;
    private GoogleMap gMap;
    private TextView tvNoMapDataMessage;
    private Button btnFullScreenMap;
    private boolean hasMapData = false;

    // Posts section
    private RecyclerView rvPosts;
    private TextView tvNoPostsMessage;
    private Button btnCreatePost;
    private ProfilePostsAdapter profilePostsAdapter;
    private List<Post> postsList;

    private ProfileFragmentCallback callback;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentLoggedInUser; // Renamed to clarify this is the currently authenticated user
    private DocumentReference userProfileRef; // Refers to the profile of 'displayUserId'
    private CollectionReference postsCollectionRef;
    private CollectionReference storiesCollectionRef;

    // Listener registrations
    private ListenerRegistration userProfileListener;
    private ListenerRegistration userPostsListener;
    private ListenerRegistration userStoriesListener;
    private ListenerRegistration friendsListener;


    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userId Parameter 1.
     * @return A new instance of fragment ProfileFragment.
     */
    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setProfileFragmentCallback(ProfileFragmentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ProfileFragmentCallback) {
            callback = (ProfileFragmentCallback) context;
        } else {
            // It's often better to log a warning here if the fragment can still function
            // without the callback, or provide a default no-op implementation.
            // If the callback is absolutely mandatory for core functionality, then throw.
            Log.e(TAG, "Host Activity must implement ProfileFragmentCallback");
            // throw new RuntimeException(context.toString() + " must implement ProfileFragmentCallback");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentLoggedInUser = mAuth.getCurrentUser(); // This is the user logged into the app

        // Retrieve the userId from arguments, default to currentLoggedInUser's UID if not provided
        if (getArguments() != null) {
            displayUserId = getArguments().getString(ARG_USER_ID);
        }

        // If no userId was passed in arguments, or if currentLoggedInUser is null (not logged in)
        // AND no userId was explicitly set, then we might have a problem.
        // For now, if displayUserId is null, we assume it's for the currently logged-in user.
        if (displayUserId == null && currentLoggedInUser != null) {
            displayUserId = currentLoggedInUser.getUid();
            Log.d(TAG, "ProfileFragment: No userId argument provided, displaying current user's profile: " + displayUserId);
        } else if (displayUserId != null) {
            Log.d(TAG, "ProfileFragment: Displaying profile for userId from arguments: " + displayUserId);
        } else {
            Log.e(TAG, "ProfileFragment: No user ID available for display. User not logged in and no argument provided.");
            Toast.makeText(getContext(), "No user profile to display. Please log in.", Toast.LENGTH_LONG).show();
            // You might want to navigate back or to a login screen here.
        }

        // Initialize Firestore references using displayUserId
        if (displayUserId != null) {
            userProfileRef = db.collection("users").document(displayUserId);
            postsCollectionRef = db.collection("posts"); // Will query by userId later
            storiesCollectionRef = db.collection("stories"); // Will query by userId later
        }

        friendsList = new ArrayList<>();
        storiesList = new ArrayList<>();
        postsList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // --- Initialize Views ---
        ivBackArrow = view.findViewById(R.id.ivBackArrow);
        ivSettings = view.findViewById(R.id.ivSettings);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserDescription = view.findViewById(R.id.tvUserDescription);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        tvFriendsCount = view.findViewById(R.id.tvFriendsCount);
        rvFriends = view.findViewById(R.id.rvFriends);
        tvNoFriendsMessage = view.findViewById(R.id.tvNoFriendsMessage);
        btnAddFriends = view.findViewById(R.id.btnAddFriends);

        rvStories = view.findViewById(R.id.rvStories);
        tvNoStoriesMessage = view.findViewById(R.id.tvNoStoriesMessage);
        btnAddStory = view.findViewById(R.id.btnAddStory);

        miniMapView = view.findViewById(R.id.miniMapView);
        tvNoMapDataMessage = view.findViewById(R.id.tvNoMapDataMessage);
        btnFullScreenMap = view.findViewById(R.id.btnFullScreenMap);

        rvPosts = view.findViewById(R.id.rvPosts);
        tvNoPostsMessage = view.findViewById(R.id.tvNoPostsMessage);
        btnCreatePost = view.findViewById(R.id.btnCreatePost);

        // --- Setup RecyclerView Adapters ---
        setupFriendsRecyclerView();
        setupStoriesRecyclerView();
        setupPostsRecyclerView();

        // --- Initialize mini map ---
        miniMapView.onCreate(savedInstanceState);
        miniMapView.getMapAsync(this); // Get map asynchronously

        // --- Set Click Listeners ---
        ivBackArrow.setOnClickListener(v -> {
            if (callback != null) {
                callback.onBackPressFromProfile();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        ivSettings.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToSettings();
            } else {
                Toast.makeText(getContext(), "Settings clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        // Show/Hide Edit Profile, Add Story, Create Post, Add Friends buttons based on whether
        // this is the current user's profile being viewed.
        boolean isCurrentUserProfile = (currentLoggedInUser != null && currentLoggedInUser.getUid().equals(displayUserId));
        btnEditProfile.setVisibility(isCurrentUserProfile ? View.VISIBLE : View.GONE);
        btnAddStory.setVisibility(isCurrentUserProfile ? View.VISIBLE : View.GONE);
        btnCreatePost.setVisibility(isCurrentUserProfile ? View.VISIBLE : View.GONE);
        btnAddFriends.setVisibility(isCurrentUserProfile ? View.VISIBLE : View.GONE); // For current user, this might be "Find Friends"

        // For other users, btnAddFriends might be "Add Friend" or "Message".
        // You'll need separate logic if the button text/action changes for other users.
        // For now, it's hidden if not current user.


        btnEditProfile.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToProfileEdit();
            } else {
                Toast.makeText(getContext(), "Edit Profile clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddFriends.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToFriendSearch();
            } else {
                Toast.makeText(getContext(), "Find Friends clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddStory.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToStoryCreation();
            } else {
                Toast.makeText(getContext(), "Share Your Story clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        btnFullScreenMap.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToFullScreenMap();
            } else {
                Toast.makeText(getContext(), "Full screen map clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCreatePost.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToPostCreation();
            } else {
                Toast.makeText(getContext(), "Create Your First Post clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Only attempt to fetch data if we have a valid userId to display
        if (displayUserId != null) {
            listenForUserProfile();
            listenForUserPosts();
            listenForUserStories();
            listenForFriends(); // Call new method to listen for friends
        }
        if (miniMapView != null) miniMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userProfileListener != null) userProfileListener.remove();
        if (userPostsListener != null) userPostsListener.remove();
        if (userStoriesListener != null) userStoriesListener.remove();
        if (friendsListener != null) friendsListener.remove();

        if (miniMapView != null) miniMapView.onStop();
    }

    /**
     * Loads the profile data for the `displayUserId`.
     * This method is now generalized to load any user's profile.
     */
    private void listenForUserProfile() {
        if (displayUserId == null) {
            Log.e(TAG, "listenForUserProfile: displayUserId is null, cannot fetch profile.");
            return;
        }
        userProfileRef = db.collection("users").document(displayUserId); // Ensure this ref points to the correct user

        userProfileListener = userProfileRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error fetching profile for user " + displayUserId + ": " + e.getMessage(), e);
                Toast.makeText(getContext(), "Error fetching profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                User user = snapshot.toObject(User.class);
                if (user != null) {
                    String displayUserName = user.getFirstName() + " " + user.getLastName();
                    if (displayUserName.trim().isEmpty()) {
                        displayUserName = user.getUsername();
                    }
                    tvUserName.setText(displayUserName);
                    tvUserDescription.setText(user.getBio());

                    if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                        Glide.with(this)
                                .load(user.getProfilePicUrl())
                                .placeholder(R.drawable.ic_default_profile_placeholder)
                                .error(R.drawable.ic_default_profile_placeholder)
                                .into(ivProfilePicture);
                    } else {
                        ivProfilePicture.setImageResource(R.drawable.ic_default_profile_placeholder);
                    }
                    tvUserName.setTextColor(getResources().getColor(android.R.color.black));
                    tvUserDescription.setTextColor(getResources().getColor(android.R.color.black));
                }
            } else {
                Log.w(TAG, "Profile data for user " + displayUserId + " not found or empty.");
                tvUserName.setText("Profile Not Found");
                tvUserDescription.setText("This user's profile data could not be loaded.");
                ivProfilePicture.setImageResource(R.drawable.ic_default_profile_placeholder);
                tvUserName.setTextColor(getResources().getColor(android.R.color.darker_gray));
                tvUserDescription.setTextColor(getResources().getColor(android.R.color.darker_gray));
                Toast.makeText(getContext(), "Profile data not found!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetches and displays posts for the `displayUserId`.
     */
    private void listenForUserPosts() {
        if (displayUserId == null) {
            Log.e(TAG, "listenForUserPosts: displayUserId is null, cannot fetch posts.");
            return;
        }

        userPostsListener = postsCollectionRef
                .whereEqualTo("userId", displayUserId) // Query for posts of the displayed user
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for user posts: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error loading posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        List<Post> newPosts = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Post post = doc.toObject(Post.class);
                                if (post != null) {
                                    post.setId(doc.getId());
                                    newPosts.add(post);
                                }
                            } catch (Exception parseException) {
                                Log.e(TAG, "Error parsing post document " + doc.getId() + ": " + parseException.getMessage(), parseException);
                            }
                        }
                        postsList.clear();
                        postsList.addAll(newPosts);
                        profilePostsAdapter.updatePosts(postsList);

                        if (postsList.isEmpty()) {
                            rvPosts.setVisibility(View.GONE);
                            tvNoPostsMessage.setVisibility(View.VISIBLE);
                            btnCreatePost.setVisibility(View.GONE); // Only show for current user, handled in onCreateView now
                        } else {
                            rvPosts.setVisibility(View.VISIBLE);
                            tvNoPostsMessage.setVisibility(View.GONE);
                            btnCreatePost.setVisibility(View.GONE); // Only show for current user, handled in onCreateView now
                        }
                    }
                });
    }

    /**
     * Fetches and displays stories for the `displayUserId`.
     * Updated to use the 'Story' model directly and filter by 24 hours.
     */
    private void listenForUserStories() {
        if (displayUserId == null) {
            Log.e(TAG, "listenForUserStories: displayUserId is null, cannot fetch stories.");
            return;
        }

        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        Timestamp cutoffTimestamp = new Timestamp(new Date(twentyFourHoursAgo));

        userStoriesListener = storiesCollectionRef
                .whereEqualTo("userId", displayUserId) // Query for stories of the displayed user
                .whereGreaterThanOrEqualTo("timestamp", cutoffTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for user stories: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error loading stories: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        List<Story> newStories = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Story story = doc.toObject(Story.class);
                                if (story != null) {
                                    story.setId(doc.getId());
                                    newStories.add(story);
                                }
                            } catch (Exception parseException) {
                                Log.e(TAG, "Error parsing story document " + doc.getId() + ": " + parseException.getMessage(), parseException);
                            }
                        }
                        storiesList.clear();
                        storiesList.addAll(newStories);
                        profileStoriesAdapter.updateStories(storiesList);

                        if (storiesList.isEmpty()) {
                            rvStories.setVisibility(View.GONE);
                            tvNoStoriesMessage.setVisibility(View.VISIBLE);
                            btnAddStory.setVisibility(View.GONE); // Only show for current user, handled in onCreateView now
                        } else {
                            rvStories.setVisibility(View.VISIBLE);
                            tvNoStoriesMessage.setVisibility(View.GONE);
                            btnAddStory.setVisibility(View.GONE); // Only show for current user, handled in onCreateView now
                        }
                    }
                });
    }

    /**
     * Fetches and displays friends for the `displayUserId`.
     * This assumes a "friends" subcollection or a "friendships" collection
     * where each document directly references the friend's user ID.
     */
    private void listenForFriends() {
        if (displayUserId == null) {
            Log.e(TAG, "listenForFriends: displayUserId is null, cannot fetch friends.");
            return;
        }

        // Assuming a subcollection 'friends' under each user document
        // e.g., /users/{userId}/friends/{friendshipDocId}
        // or a top-level 'friendships' collection
        // e.g., /friendships/{friendshipDocId} where it contains userId1, userId2, status
        // For simplicity, let's assume a subcollection like: /users/{displayUserId}/friends
        // and each document in it contains a 'friendId' field and 'username', 'profilePicUrl'.
        // You might need to adjust this query based on your actual Firestore structure.

        // Example: Fetching friends from a 'friends' subcollection
        friendsListener = db.collection("users").document(displayUserId).collection("friends")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for friends: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error loading friends: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        List<FriendsAdapter.FriendItem> newFriends = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                // Assuming your 'friends' subcollection documents contain these fields
                                String friendId = doc.getId(); // Or doc.getString("friendId") if it's a field
                                String username = doc.getString("username");
                                String profilePicUrl = doc.getString("profilePicUrl");

                                if (username != null && profilePicUrl != null) {
                                    newFriends.add(new FriendsAdapter.FriendItem(friendId, username, profilePicUrl));
                                }
                            } catch (Exception parseException) {
                                Log.e(TAG, "Error parsing friend document " + doc.getId() + ": " + parseException.getMessage(), parseException);
                            }
                        }
                        friendsList.clear();
                        friendsList.addAll(newFriends);
                        friendsAdapter.updateFriends(friendsList); // Make sure your FriendsAdapter has this method

                        tvFriendsCount.setText(String.valueOf(friendsList.size()));

                        if (friendsList.isEmpty()) {
                            rvFriends.setVisibility(View.GONE);
                            tvNoFriendsMessage.setVisibility(View.VISIBLE);
                            btnAddFriends.setVisibility(View.GONE); // Only show for current user, handled in onCreateView
                        } else {
                            rvFriends.setVisibility(View.VISIBLE);
                            tvNoFriendsMessage.setVisibility(View.GONE);
                            btnAddFriends.setVisibility(View.GONE); // Only show for current user, handled in onCreateView
                        }
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        gMap.getUiSettings().setAllGesturesEnabled(false);
        gMap.getUiSettings().setZoomControlsEnabled(false);
        gMap.getUiSettings().setMapToolbarEnabled(false);

        // Fetch and display map data for the 'displayUserId'
        // This is a placeholder; you'd load actual location data associated with 'displayUserId'
        // from Firestore and then add markers/polylines.
        // For example, if you have a 'locations' collection with userId and LatLng:
        // db.collection("locations").whereEqualTo("userId", displayUserId).get()...
        LatLng defaultLocation = new LatLng(14.5995, 120.9842); // Example: Manila
        gMap.addMarker(new MarkerOptions().position(defaultLocation).title("User's Last Known Location"));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));

        // If you actually fetch map data and it's not empty:
        hasMapData = true; // Set this based on actual data
        if (hasMapData) {
            miniMapView.setVisibility(View.VISIBLE);
            tvNoMapDataMessage.setVisibility(View.GONE);
            btnFullScreenMap.setVisibility(View.VISIBLE);
        } else {
            miniMapView.setVisibility(View.GONE);
            tvNoMapDataMessage.setVisibility(View.VISIBLE);
            btnFullScreenMap.setVisibility(View.GONE);
        }
        // Optional: Apply map style (e.g., from R.raw.map_style_black_accents)
    }

    private void setupFriendsRecyclerView() {
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        friendsAdapter = new FriendsAdapter(friendsList, friendItem -> {
            if (callback != null) {
                // When clicking a friend's profile from *this* profile, navigate to *their* profile
                callback.openFriendProfile(friendItem); // Or callback.navigateToProfileFromHome(friendItem.getUserId());
            } else {
                Toast.makeText(getContext(), "Viewing friend: " + friendItem.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });
        rvFriends.setAdapter(friendsAdapter);
    }

    private void setupStoriesRecyclerView() {
        rvStories.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns for stories
        profileStoriesAdapter = new ProfileStoriesAdapter(storiesList, (stories, clickedPosition) -> {
            if (callback != null) {
                callback.openStoryViewer(stories, clickedPosition);
            } else {
                Toast.makeText(getContext(), "Viewing story at index: " + clickedPosition, Toast.LENGTH_SHORT).show();
            }
        });
        profileStoriesAdapter.setContext(getContext()); // Set context for Glide
        rvStories.setAdapter(profileStoriesAdapter);
    }

    private void setupPostsRecyclerView() {
        rvPosts.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns for posts
        profilePostsAdapter = new ProfilePostsAdapter(postsList, postItem -> {
            if (callback != null) {
                callback.openPostDetails(postItem);
            } else {
                Toast.makeText(getContext(), "Viewing post: " + postItem.getId(), Toast.LENGTH_SHORT).show();
            }
        });
        rvPosts.setAdapter(profilePostsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (miniMapView != null) miniMapView.onResume();
    }

    @Override
    public void onPause() {
        if (miniMapView != null) miniMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        callback = null; // Clear callback to prevent leaks
        if (miniMapView != null) {
            miniMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (miniMapView != null) miniMapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle("MapViewBundleKey");
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle("MapViewBundleKey", mapViewBundle);
        }
        // Only save map state if it's visible. Otherwise, it might cause issues.
        if (miniMapView != null && miniMapView.getVisibility() == View.VISIBLE) {
            miniMapView.onSaveInstanceState(mapViewBundle);
        }
        // Save the displayUserId for rotation/recreation
        outState.putString(ARG_USER_ID, displayUserId);
    }

    // Restore userId if fragment is recreated
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            displayUserId = savedInstanceState.getString(ARG_USER_ID);
            // Re-initialize Firestore references if they were null or pointing to old user
            if (displayUserId != null && userProfileRef == null) {
                userProfileRef = db.collection("users").document(displayUserId);
                postsCollectionRef = db.collection("posts");
                storiesCollectionRef = db.collection("stories");
            }
        }
    }
}