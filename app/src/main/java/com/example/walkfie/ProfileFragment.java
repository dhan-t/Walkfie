package com.example.walkfie;

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
import android.util.Log; // Add Log import

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

import java.util.ArrayList;
import java.util.List;
import java.util.Date; // For story timestamp filtering if needed
import com.google.firebase.Timestamp; // For story timestamp filtering if needed


// Define a callback interface for actions in ProfileFragment
interface ProfileFragmentCallback {
    void navigateToProfileEdit();
    void navigateToSettings();
    void navigateToFullScreenMap();
    void navigateToPostCreation();
    void navigateToFriendSearch();
    void navigateToStoryCreation();
    void onBackPressFromProfile();
    void openPostDetails(PostAdapter.PostItem post);
    // Updated signature: now passes a List<Story> and starting index
    void openStoryViewer(List<Story> storiesToView, int startIndex);
    void openFriendProfile(FriendsAdapter.FriendItem friend);
}

public class ProfileFragment extends Fragment implements OnMapReadyCallback {

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
    private ProfileStoriesAdapter profileStoriesAdapter; // Use the new adapter
    private List<Story> storiesList; // Change to List<Story>

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
    private List<PostAdapter.PostItem> postsList;

    private ProfileFragmentCallback callback;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private DocumentReference userProfileRef;
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

    public void setProfileFragmentCallback(ProfileFragmentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof ProfileFragmentCallback) {
            callback = (ProfileFragmentCallback) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ProfileFragmentCallback");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userProfileRef = db.collection("users").document(currentUser.getUid());
            postsCollectionRef = db.collection("posts");
            storiesCollectionRef = db.collection("stories");
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_LONG).show();
            // Consider navigating to login/onboarding here
        }

        friendsList = new ArrayList<>();
        storiesList = new ArrayList<>(); // Now List<Story>
        postsList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // --- Initialize Views --- (unchanged)
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
        setupStoriesRecyclerView(); // This one changed
        setupPostsRecyclerView();

        // --- Initialize mini map ---
        miniMapView.onCreate(savedInstanceState);

        // --- Set Click Listeners --- (unchanged)
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
        if (currentUser != null) {
            listenForUserProfile();
            listenForUserPosts();
            listenForUserStories();
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

    private void listenForUserProfile() {
        if (userProfileRef == null) return;

        userProfileListener = userProfileRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e("ProfileFragment", "Error fetching profile: " + e.getMessage(), e); // Use Log for debugging
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
                tvUserName.setText("Complete Your Profile");
                tvUserDescription.setText("Add a description to tell others about yourself.");
                ivProfilePicture.setImageResource(R.drawable.ic_default_profile_placeholder);
                tvUserName.setTextColor(getResources().getColor(android.R.color.darker_gray));
                tvUserDescription.setTextColor(getResources().getColor(android.R.color.darker_gray));
                Toast.makeText(getContext(), "Please complete your profile!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForUserPosts() {
        if (currentUser == null) return;

        userPostsListener = postsCollectionRef
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("ProfileFragment", "Error listening for user posts: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error loading posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        List<PostAdapter.PostItem> newPosts = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                PostAdapter.PostItem post = doc.toObject(PostAdapter.PostItem.class);
                                post.setId(doc.getId()); // Set the Firestore document ID
                                newPosts.add(post);
                            } catch (Exception parseException) {
                                Log.e("ProfileFragment", "Error parsing post document " + doc.getId() + ": " + parseException.getMessage(), parseException);
                            }
                        }
                        postsList.clear();
                        postsList.addAll(newPosts);
                        profilePostsAdapter.updatePosts(postsList);

                        if (postsList.isEmpty()) {
                            rvPosts.setVisibility(View.GONE);
                            tvNoPostsMessage.setVisibility(View.VISIBLE);
                            btnCreatePost.setVisibility(View.VISIBLE);
                        } else {
                            rvPosts.setVisibility(View.VISIBLE);
                            tvNoPostsMessage.setVisibility(View.GONE);
                            btnCreatePost.setVisibility(View.GONE);
                        }
                    }
                });
    }

    /**
     * Fetches and displays the current user's stories from Firestore.
     * Updated to use the 'Story' model directly and filter by 24 hours.
     */
    private void listenForUserStories() {
        if (currentUser == null) return;

        // Filter stories that are older than 24 hours.
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        Timestamp cutoffTimestamp = new Timestamp(new Date(twentyFourHoursAgo));

        userStoriesListener = storiesCollectionRef
                .whereEqualTo("userId", currentUser.getUid()) // Filter by current user's ID
                .whereGreaterThanOrEqualTo("timestamp", cutoffTimestamp) // Only stories from last 24 hours
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order by latest first
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("ProfileFragment", "Error listening for user stories: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error loading stories: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        List<Story> newStories = new ArrayList<>(); // Now List<Story>
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Story story = doc.toObject(Story.class);
                                story.setId(doc.getId()); // Set the Firestore document ID from doc.getId()
                                newStories.add(story);
                            } catch (Exception parseException) {
                                Log.e("ProfileFragment", "Error parsing story document " + doc.getId() + ": " + parseException.getMessage(), parseException);
                            }
                        }
                        storiesList.clear();
                        storiesList.addAll(newStories);
                        profileStoriesAdapter.updateStories(storiesList); // Update the adapter

                        if (storiesList.isEmpty()) {
                            rvStories.setVisibility(View.GONE);
                            tvNoStoriesMessage.setVisibility(View.VISIBLE);
                            btnAddStory.setVisibility(View.VISIBLE);
                        } else {
                            rvStories.setVisibility(View.VISIBLE);
                            tvNoStoriesMessage.setVisibility(View.GONE);
                            btnAddStory.setVisibility(View.GONE);
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

        // This is where you'd load actual map data if available
        LatLng userLocation = new LatLng(14.5995, 120.9842); // Example: Manila
        gMap.addMarker(new MarkerOptions().position(userLocation).title("My Last Walkfie Spot"));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f));

        // Optional: Apply map style (e.g., from R.raw.map_style_black_accents)
    }

    private void setupFriendsRecyclerView() {
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        friendsAdapter = new FriendsAdapter(friendsList, friendItem -> {
            if (callback != null) {
                callback.openFriendProfile(friendItem);
            } else {
                Toast.makeText(getContext(), "Viewing friend: " + friendItem.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });
        rvFriends.setAdapter(friendsAdapter);
    }

    private void setupStoriesRecyclerView() {
        rvStories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        profileStoriesAdapter = new ProfileStoriesAdapter(storiesList, (stories, clickedPosition) -> {
            if (callback != null) {
                // Pass the full list of stories and the index of the clicked one
                callback.openStoryViewer(stories, clickedPosition);
            } else {
                Toast.makeText(getContext(), "Viewing story at index: " + clickedPosition, Toast.LENGTH_SHORT).show();
            }
        });
        profileStoriesAdapter.setContext(getContext()); // Set context for Glide
        rvStories.setAdapter(profileStoriesAdapter);
    }

    private void setupPostsRecyclerView() {
        rvPosts.setLayoutManager(new GridLayoutManager(getContext(), 3));
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
        callback = null;
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
        if (miniMapView != null && miniMapView.getVisibility() == View.VISIBLE) {
            miniMapView.onSaveInstanceState(mapViewBundle);
        }
    }
}