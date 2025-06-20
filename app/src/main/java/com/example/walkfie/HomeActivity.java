package com.example.walkfie;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
// Ensure this import is correct. If StoryViewerFragment is in a different package, adjust.
import com.example.walkfie.StoryViewerFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MODIFIED: Added StoryViewerFragment.StoryViewerCallback AND CreatePostFragment.CreatePostCallback
public class HomeActivity extends AppCompatActivity implements
        RecordFragmentCallback, MapFragmentCallback, ProfileFragmentCallback,
        MessageFragmentCallback, HomeFragment.HomeFragmentCallback, EditProfileCallback,
        AddStoryCallback, PhotoFragmentCallback,
        StoryViewerFragment.StoryViewerCallback, // Existing
        CreatePostFragment.CreatePostCallback { // <--- NEW: Implements CreatePostCallback

    private static final String TAG = "HomeActivity";
    BottomNavigationView bottomNavigationView;
    View fragmentContainer;
    private FragmentManager fragmentManager;
    private Fragment currentFragment;
    private HomeFragment homeFragment;
    private PhotoFragment photoFragment;
    private RecordFragment recordFragment;
    private MapFragment mapFragment;
    private MessageFragment messageFragment;
    private ProfileFragment profileFragment;
    private EditProfileFragment editProfileFragment;
    private AddStoryFragment addStoryFragment;
    private CreatePostFragment createPostFragment; // Existing
    private FindFriendsFragment findFriendsFragment;
    private FirebaseAuth mAuth; // Existing
    // Add references for PostDetailsFragment if you create it, storyViewerFragment instance is created on demand
    // private PostDetailsFragment postDetailsFragment;

    // NEW: Flag to manage programmatic bottom nav selection to prevent unwanted backstack pops
    private boolean isProgrammaticBottomNavSelection = false;

    private Fragment lastSelectedBottomNavFragment;
    private Map<Integer, Fragment> bottomNavFragmentsMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: savedInstanceState is NULL. Initializing new fragments.");
            homeFragment = new HomeFragment();
            photoFragment = new PhotoFragment();
            recordFragment = new RecordFragment();
            mapFragment = new MapFragment();
            messageFragment = new MessageFragment();
            profileFragment = new ProfileFragment();
            addStoryFragment = new AddStoryFragment();
            // createPostFragment and findFriendsFragment are typically initialized when navigated to,
            // but can be initialized here if they are part of the initial set of retained fragments.
            // For now, we'll keep them instantiated on demand or reuse if already in map.
            // createPostFragment = new CreatePostFragment(); // No need to initialize here if newInstance() is always used
            // findFriendsFragment = new FindFriendsFragment(); // No need to initialize here if newInstance() is always used


            bottomNavFragmentsMap = new HashMap<>();
            bottomNavFragmentsMap.put(R.id.nav_home, homeFragment);
            bottomNavFragmentsMap.put(R.id.nav_photo, photoFragment);
            bottomNavFragmentsMap.put(R.id.nav_record, recordFragment);
            bottomNavFragmentsMap.put(R.id.nav_map, mapFragment);
            bottomNavFragmentsMap.put(R.id.nav_message, messageFragment);
            // ProfileFragment is also a root bottom nav fragment for PROFILE_FRAGMENT_TAG
//            bottomNavFragmentsMap.put(R.id.nav_profile, profileFragment); // Assuming you have a nav_profile item in your menu


            loadInitialBottomNavFragments();
            lastSelectedBottomNavFragment = homeFragment;
        } else {
            Log.d(TAG, "onCreate: savedInstanceState is NOT NULL. Restoring existing fragments.");
            homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("HOME_FRAGMENT");
            photoFragment = (PhotoFragment) fragmentManager.findFragmentByTag("PHOTO_FRAGMENT");
            recordFragment = (RecordFragment) fragmentManager.findFragmentByTag("RECORD_FRAGMENT");
            mapFragment = (MapFragment) fragmentManager.findFragmentByTag("MAP_FRAGMENT");
            messageFragment = (MessageFragment) fragmentManager.findFragmentByTag("MESSAGE_FRAGMENT");
            profileFragment = (ProfileFragment) fragmentManager.findFragmentByTag("PROFILE_FRAGMENT_TAG");
            addStoryFragment = (AddStoryFragment) fragmentManager.findFragmentByTag("ADD_STORY_FRAGMENT_TAG");
            createPostFragment = (CreatePostFragment) fragmentManager.findFragmentByTag("CREATE_POST_FRAGMENT_TAG");
            findFriendsFragment = (FindFriendsFragment) fragmentManager.findFragmentByTag("FIND_FRIENDS_FRAGMENT_TAG");

            bottomNavFragmentsMap = new HashMap<>();
            bottomNavFragmentsMap.put(R.id.nav_home, homeFragment);
            bottomNavFragmentsMap.put(R.id.nav_photo, photoFragment);
            bottomNavFragmentsMap.put(R.id.nav_record, recordFragment);
            bottomNavFragmentsMap.put(R.id.nav_map, mapFragment);
            bottomNavFragmentsMap.put(R.id.nav_message, messageFragment);
//            bottomNavFragmentsMap.put(R.id.nav_profile, profileFragment); // Assuming nav_profile

            for (Fragment fragment : fragmentManager.getFragments()) {
                if (fragment != null && fragment.isAdded() && !fragment.isHidden()) {
                    currentFragment = fragment;
                    Log.d(TAG, "Restored currentFragment: " + currentFragment.getClass().getSimpleName());
                    break;
                }
            }
            if (currentFragment != null && bottomNavFragmentsMap.containsValue(currentFragment)) {
                lastSelectedBottomNavFragment = currentFragment;
            } else {
                if (savedInstanceState != null) {
                    int lastBottomNavId = savedInstanceState.getInt("LAST_BOTTOM_NAV_ID", R.id.nav_home);
                    lastSelectedBottomNavFragment = bottomNavFragmentsMap.get(lastBottomNavId);
                } else {
                    lastSelectedBottomNavFragment = homeFragment;
                }
                Log.d(TAG, "Restored lastSelectedBottomNavFragment: " + (lastSelectedBottomNavFragment != null ? lastSelectedBottomNavFragment.getClass().getSimpleName() : "null"));
            }
        }

        // Set callbacks for fragments (ensure null checks for restored fragments)
        if (homeFragment != null) homeFragment.setHomeFragmentCallback(this);
        if (recordFragment != null) recordFragment.setRecordFragmentCallback(this);
        if (mapFragment != null) mapFragment.setMapFragmentCallback(this);
        if (profileFragment != null) profileFragment.setProfileFragmentCallback(this);
        if (messageFragment != null) messageFragment.setMessageFragmentCallback(this);
        // editProfileFragment, addStoryFragment, createPostFragment, findFriendsFragment are usually created fresh or managed on demand
        // so their callbacks are typically set when they are instantiated or pushed.

        setupAnimations();
        setupNavigation();

        // MODIFIED: Fragment back stack changed listener to handle bottom nav visibility
        fragmentManager.addOnBackStackChangedListener(() -> {
            Log.d(TAG, "Back stack changed. Count: " + fragmentManager.getBackStackEntryCount());
            Fragment topFragment = fragmentManager.findFragmentById(R.id.fragment_container);

            if (topFragment != null) {
                currentFragment = topFragment;
                Log.d(TAG, "Current fragment after back stack change: " + currentFragment.getClass().getSimpleName());
            } else {
                currentFragment = null; // Should not happen if a fragment is always in container
                Log.d(TAG, "No fragment found in container after back stack change.");
            }

            if (fragmentManager.getBackStackEntryCount() == 0) {
                // Back stack is empty: user navigated back to a root bottom nav fragment
                Log.d(TAG, "Back stack is empty. Attempting to restore lastSelectedBottomNavFragment: " +
                        (lastSelectedBottomNavFragment != null ? lastSelectedBottomNavFragment.getClass().getSimpleName() : "null"));

                if (lastSelectedBottomNavFragment != null) {
                    showBottomNavFragment(lastSelectedBottomNavFragment); // This method already calls commit
                    // Prevent setupNavigation from acting on this programmatic selection
                    isProgrammaticBottomNavSelection = true;
                    for (Map.Entry<Integer, Fragment> entry : bottomNavFragmentsMap.entrySet()) {
                        if (entry.getValue() == lastSelectedBottomNavFragment) {
                            bottomNavigationView.setSelectedItemId(entry.getKey());
                            Log.d(TAG, "Bottom nav item selected: " + entry.getKey() + " for fragment: " + lastSelectedBottomNavFragment.getClass().getSimpleName());
                            break;
                        }
                    }
                    isProgrammaticBottomNavSelection = false;
                } else {
                    Log.w(TAG, "lastSelectedBottomNavFragment is null. Defaulting to HomeFragment.");
                    isProgrammaticBottomNavSelection = true;
                    showBottomNavFragment(homeFragment);
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    lastSelectedBottomNavFragment = homeFragment;
                    isProgrammaticBottomNavSelection = false;
                }
                // Show bottom navigation bar
                bottomNavigationView.setVisibility(View.VISIBLE);

            } else {
                // Back stack is NOT empty: a pushed fragment (e.g., StoryViewer, EditProfile, CreatePost) is on top
                Log.d(TAG, "Back stack is not empty. A pushed fragment is on top. Hiding bottom nav.");

                // Hide the bottom navigation bar when a non-bottom-nav fragment is on top
                bottomNavigationView.setVisibility(View.GONE);
            }
        });
    }

    private void showBottomNavFragment(Fragment fragmentToShow) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out); // For pop transitions

        // Hide current fragment only if it's different from the one to show
        if (currentFragment != null && currentFragment != fragmentToShow) {
            transaction.hide(currentFragment);
            Log.d(TAG, "Hiding bottom nav fragment: " + currentFragment.getClass().getSimpleName());
        }

        if (!fragmentToShow.isAdded()) {
            String tag;
            if (fragmentToShow == homeFragment) tag = "HOME_FRAGMENT";
            else if (fragmentToShow == photoFragment) tag = "PHOTO_FRAGMENT";
            else if (fragmentToShow == recordFragment) tag = "RECORD_FRAGMENT";
            else if (fragmentToShow == mapFragment) tag = "MAP_FRAGMENT";
            else if (fragmentToShow == messageFragment) tag = "MESSAGE_FRAGMENT";
            else if (fragmentToShow == profileFragment) tag = "PROFILE_FRAGMENT_TAG";
            else tag = fragmentToShow.getClass().getSimpleName().toUpperCase() + "_FRAGMENT_TAG"; // Fallback tag
            transaction.add(R.id.fragment_container, fragmentToShow, tag);
            Log.d(TAG, "Adding bottom nav fragment: " + fragmentToShow.getClass().getSimpleName() + " with tag: " + tag);
        } else {
            transaction.show(fragmentToShow);
            Log.d(TAG, "Showing existing bottom nav fragment: " + fragmentToShow.getClass().getSimpleName());
        }
        transaction.commit();
        currentFragment = fragmentToShow; // Update current fragment after commit
    }

    private void loadInitialBottomNavFragments() {
        Log.d(TAG, "Loading initial bottom nav fragments...");
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, homeFragment, "HOME_FRAGMENT");
        transaction.add(R.id.fragment_container, photoFragment, "PHOTO_FRAGMENT").hide(photoFragment);
        transaction.add(R.id.fragment_container, recordFragment, "RECORD_FRAGMENT").hide(recordFragment);
        transaction.add(R.id.fragment_container, mapFragment, "MAP_FRAGMENT").hide(mapFragment);
        transaction.add(R.id.fragment_container, messageFragment, "MESSAGE_FRAGMENT").hide(messageFragment);
        if (!profileFragment.isAdded()) { // Ensure profileFragment is also initially added if it's a root bottom nav fragment
            transaction.add(R.id.fragment_container, profileFragment, "PROFILE_FRAGMENT_TAG").hide(profileFragment);
        }
        transaction.commit();
        currentFragment = homeFragment;
        Log.d(TAG, "Initial currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    private void setupAnimations() {
        Animation fadeSlideFragment = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fragmentContainer.startAnimation(fadeSlideFragment);
        Animation fadeSlideNav = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeSlideNav.setStartOffset(200);
        bottomNavigationView.startAnimation(fadeSlideNav);
        bottomNavigationView.post(this::animateNavIconsStaggered);
    }

    // MODIFIED: setupNavigation to incorporate the new flag and improved back stack handling
    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // If selection is programmatic, ignore the listener's logic to prevent unwanted cycles
            if (isProgrammaticBottomNavSelection) {
                return true;
            }

            int itemId = item.getItemId();
            Fragment selectedBottomNavFragment = bottomNavFragmentsMap.get(itemId);

            // This logic is for when a user *manually* clicks a bottom nav item.
            // If there are fragments on the back stack (meaning we're *not* on a root bottom nav fragment),
            // we should pop the back stack to reveal the target bottom nav fragment.
            if (fragmentManager.getBackStackEntryCount() > 0) {
                // Pop back to the root of the back stack, effectively clearing it
                // and revealing the last bottom nav fragment if one exists, or homeFragment.
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                Log.d(TAG, "Cleared back stack due to bottom nav selection (user click).");
                // The onBackStackChangedListener will now take over and display the correct bottom nav fragment
                // and hide the bottom nav bar temporarily if needed.
                return true; // Let the onBackStackChangedListener handle the rest
            }

            // If we are already on a bottom nav fragment and the selected one is the same, do nothing.
            if (selectedBottomNavFragment != null && selectedBottomNavFragment == currentFragment) {
                Log.d(TAG, "Already on selected bottom nav fragment: " + selectedBottomNavFragment.getClass().getSimpleName());
                return true;
            }

            // If we are already on a bottom nav fragment and a different one is selected, switch them.
            if (selectedBottomNavFragment != null) {
                Log.d(TAG, "Switching bottom nav from " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null") + " to " + selectedBottomNavFragment.getClass().getSimpleName());
                showBottomNavFragment(selectedBottomNavFragment);
                lastSelectedBottomNavFragment = selectedBottomNavFragment;
            }
            return true;
        });
    }

    private void animateNavIconsStaggered() {
        View menuView = bottomNavigationView.getChildAt(0);
        if (menuView instanceof ViewGroup) {
            ViewGroup menuViewGroup = (ViewGroup) menuView;
            for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
                View item = menuViewGroup.getChildAt(i);
                Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
                scaleIn.setStartOffset(300 + (i * 100));
                item.startAnimation(scaleIn);
            }
        }
    }

    @Override
    public void onRecordingStarted(String activityType) {
        // Your existing code for onRecordingStarted
    }

    @Override
    public void onRecordingStopped() {
        // Your existing code for onRecordingStopped
    }

    @Override
    public void navigateToProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Call the new, more general method with the current user's ID
            navigateToProfileFromHome(currentUser.getUid());
        } else {
            Toast.makeText(this, "User not logged in to view profile.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Attempted to navigate to profile, but user is not logged in.");
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);

        // Hide the current fragment if it's a bottom nav fragment or another pushed fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for ProfileFragment push.");
        }

        if (!profileFragment.isAdded()) {
            transaction.add(R.id.fragment_container, profileFragment, "PROFILE_FRAGMENT_TAG");
            Log.d(TAG, "Adding ProfileFragment.");
        } else {
            transaction.show(profileFragment);
            Log.d(TAG, "Showing existing ProfileFragment.");
        }

        // Only add to back stack if it's a new push and not just re-showing the same fragment
        if (currentFragment != profileFragment) {
            transaction.addToBackStack("profile_fragment_tag"); // Use a specific tag for easier management
            Log.d(TAG, "ProfileFragment added to back stack.");
        }
        transaction.commit();
        currentFragment = profileFragment; // Update current fragment
        Log.d(TAG, "Navigation to ProfileFragment committed. New currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called. Back stack entry count: " + fragmentManager.getBackStackEntryCount());

        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(); // Pop the top fragment from the stack
        } else {
            super.onBackPressed(); // Let the system handle if back stack is empty (exit app)
        }
    }

    @Override
    public void onBackPressFromProfile() {
        onBackPressed(); // Delegate to the activity's onBackPressed
    }

    @Override
    public void navigateToProfileEdit() {
        Log.d(TAG, "navigateToProfileEdit called.");
        EditProfileFragment editProfileFragment = new EditProfileFragment();
        editProfileFragment.setEditProfileCallback(this); // Set the callback

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        if (currentFragment != null) {
            transaction.hide(currentFragment); // Hide the current fragment (ProfileFragment)
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for EditProfileFragment push.");
        }
        transaction.add(R.id.fragment_container, editProfileFragment, "EDIT_PROFILE_FRAGMENT_TAG");
        transaction.addToBackStack("edit_profile"); // Add to back stack with a name
        transaction.commit();
        currentFragment = editProfileFragment; // Update current fragment
        Log.d(TAG, "Navigation to EditProfileFragment committed. New currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    @Override
    public void navigateToSettings() {
        Toast.makeText(this, "Navigate to Settings Screen (Not implemented)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToFullScreenMap() {
        Toast.makeText(this, "Navigate to Full Screen Map (Not implemented)", Toast.LENGTH_SHORT).show();
    }

    // These methods should be called from your HomeFragmentCallback implementation
    @Override
    public void navigateToPostCreation() {
        // This is a generic method, assuming it routes to the specific one below
        navigateToPostCreationFromHome();
    }

    @Override
    public void navigateToFriendSearch() {
        // This is a generic method, assuming it routes to the specific one below
        navigateToFriendSearchFromHome();
    }

    @Override
    public void navigateToStoryCreation() {
        // This is a generic method, assuming it routes to the specific one below
        navigateToStoryCreationFromHome();
    }

    @Override
    public void openPostDetails(Post post) { // Changed PostAdapter.PostItem to Post
        Toast.makeText(this, "Opening Post Details for: " + post.getId(), Toast.LENGTH_SHORT).show();
        // Implement actual navigation to a PostDetailsFragment
        // Example:
        // PostDetailsFragment postDetailsFragment = PostDetailsFragment.newInstance(post.getId());
        // FragmentTransaction transaction = fragmentManager.beginTransaction();
        // transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
        // if (currentFragment != null) transaction.hide(currentFragment);
        // transaction.add(R.id.fragment_container, postDetailsFragment, "POST_DETAILS_FRAGMENT_TAG");
        // transaction.addToBackStack("post_details");
        // transaction.commit();
        // currentFragment = postDetailsFragment;
    }

    // MODIFIED: openStoryViewer to correctly add fragment and not interfere with bottom nav
    @Override
    public void openStoryViewer(List<Story> storiesToView, int startIndex) {
        if (storiesToView == null || storiesToView.isEmpty()) {
            Toast.makeText(this, "No stories to view.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "DEBUG_FLOW: openStoryViewer called. Stories count: " + storiesToView.size()); // New log
        StoryViewerFragment storyViewerFragment = StoryViewerFragment.newInstance(
                new ArrayList<>(storiesToView),
                startIndex
        );

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);

        if (currentFragment != null) {
            transaction.hide(currentFragment);
            Log.d(TAG, "DEBUG_FLOW: Hiding current fragment " + currentFragment.getClass().getSimpleName() + " for StoryViewerFragment."); // New log
        }

        Log.d(TAG, "DEBUG_FLOW: About to add StoryViewerFragment to R.id.fragment_container."); // New log
        transaction.add(R.id.fragment_container, storyViewerFragment, "STORY_VIEWER_FRAGMENT_TAG");
        transaction.addToBackStack("story_viewer");
        Log.d(TAG, "DEBUG_FLOW: About to commit StoryViewerFragment transaction."); // New log
        transaction.commit();
        Log.d(TAG, "DEBUG_FLOW: StoryViewerFragment transaction committed. Back stack entry count after commit: " + fragmentManager.getBackStackEntryCount()); // Modified log

        currentFragment = storyViewerFragment; // Update current fragment reference
    }

    @Override
    public void openFriendProfile(FriendsAdapter.FriendItem friend) {
        Toast.makeText(this, "Opening Friend Profile for: " + friend.getUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToProfileFromMessages() {
        navigateToProfile(); // Reuse the existing navigateToProfile method
    }

    @Override
    public void onProfileSaved() {
        Log.d(TAG, "Profile saved successfully. Navigating back to ProfileFragment.");
        getSupportFragmentManager().popBackStack(); // Pop the EditProfileFragment off the stack (will trigger onBackStackChangedListener)
        Toast.makeText(this, "Profile changes saved!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditProfileCancelled() {
        Log.d(TAG, "Edit profile cancelled. Navigating back.");
        getSupportFragmentManager().popBackStack(); // Go back to the previous fragment (will trigger onBackStackChangedListener)
    }

    @Override
    public void navigateToFriendSearchFromMessages() {
        navigateToFriendSearchFromHome(); // Reuse the HomeFragmentCallback implementation
    }

    @Override
    public void navigateToChatScreen(String chatPartnerName) {
        Toast.makeText(this, "Opening chat with: " + chatPartnerName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToProfileFromHome(String userId) {
        Log.d(TAG, "navigateToProfileFromHome called from HomeFragment with userId: " + userId);

        ProfileFragment targetProfileFragment = ProfileFragment.newInstance(userId); // Use the new factory method!

        // IMPORTANT: Ensure your ProfileFragmentCallback is set if ProfileFragment
        // needs to communicate back to HomeActivity.
        // targetProfileFragment.setProfileFragmentCallback(this); // You would need to implement ProfileFragment.Callback

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);

        if (currentFragment != null) {
            transaction.hide(currentFragment);
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for ProfileFragment push (userId: " + userId + ").");
        }

        // Always add a new instance when using newInstance for different users,
        // unless you have a very specific caching strategy (which is more complex).
        transaction.add(R.id.fragment_container, targetProfileFragment, "PROFILE_FRAGMENT_TAG_" + userId);
        Log.d(TAG, "Adding ProfileFragment for userId: " + userId + ".");

        transaction.addToBackStack("profile_" + userId); // Use unique tag for back stack
        Log.d(TAG, "ProfileFragment for userId: " + userId + " added to back stack.");

        transaction.commit();
        currentFragment = targetProfileFragment; // Update current fragment
        Log.d(TAG, "Navigation to ProfileFragment committed. New currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    @Override
    public void navigateToFriendSearchFromHome() {
        Log.d(TAG, "navigateToFriendSearchFromHome called.");
        if (findFriendsFragment == null) {
            findFriendsFragment = new FindFriendsFragment();
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        if (currentFragment != null) {
            transaction.hide(currentFragment); // Hide the current fragment (HomeFragment)
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for FindFriendsFragment push.");
        }
        if (!findFriendsFragment.isAdded()) {
            transaction.add(R.id.fragment_container, findFriendsFragment, "FIND_FRIENDS_FRAGMENT_TAG");
            Log.d(TAG, "Adding FindFriendsFragment.");
        } else {
            transaction.show(findFriendsFragment);
            Log.d(TAG, "Showing existing FindFriendsFragment.");
        }
        transaction.addToBackStack("find_friends"); // Add to back stack
        transaction.commit();
        currentFragment = findFriendsFragment; // Update current fragment
        Log.d(TAG, "Navigation to FindFriendsFragment committed. New currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    // MODIFIED: navigateToPostCreationFromHome - Set callback
    @Override
    public void navigateToPostCreationFromHome() {
        Log.d(TAG, "navigateToPostCreationFromHome called.");
        // Always create a new instance of CreatePostFragment when navigating to it
        // because it receives arguments (mediaUri, mediaType) and holds transient UI state.
        // It's not a root bottom nav fragment to be retained like others.
        // We ensure it gets the callback set here.
        createPostFragment = new CreatePostFragment(); // Re-instantiate if needed, or if it's new
        createPostFragment.setCreatePostCallback(this); // <--- NEW: Set the callback here

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        if (currentFragment != null) {
            transaction.hide(currentFragment); // Hide the current fragment (HomeFragment)
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for CreatePostFragment push.");
        }
        // No need for .isAdded() check for createPostFragment as we are creating a new instance
        transaction.add(R.id.fragment_container, createPostFragment, "CREATE_POST_FRAGMENT_TAG");
        Log.d(TAG, "Adding CreatePostFragment.");

        transaction.addToBackStack("create_post"); // Add to back stack
        transaction.commit();
        currentFragment = createPostFragment; // Update current fragment
        Log.d(TAG, "Navigation to CreatePostFragment committed. New currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    @Override
    public void navigateToStoryCreationFromHome() {
        Log.d(TAG, "navigateToStoryCreationFromHome called from HomeFragment. Showing PhotoFragment (Story mode).");
        if (photoFragment != null) { // Ensure photoFragment is initialized
            // You might need to tell PhotoFragment it's for story creation if its behavior differs
            // photoFragment.setMode(PhotoFragment.MODE_STORY_CREATION); // Example
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            if (currentFragment != null) {
                transaction.hide(currentFragment);
                Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for PhotoFragment (Story) push.");
            }
            if (!photoFragment.isAdded()) {
                transaction.add(R.id.fragment_container, photoFragment, "PHOTO_FRAGMENT");
                Log.d(TAG, "Adding PhotoFragment (Story mode).");
            } else {
                transaction.show(photoFragment);
                Log.d(TAG, "Showing existing PhotoFragment (Story mode).");
            }
            transaction.addToBackStack("story_creation_photo"); // Add to back stack
            transaction.commit();
            currentFragment = photoFragment;
            Log.d(TAG, "Navigation to PhotoFragment (Story mode) committed. New currentFragment: " + currentFragment.getClass().getSimpleName());
        } else {
            Log.e(TAG, "PhotoFragment is null when trying to navigate to Story Creation.");
            Toast.makeText(this, "Error: Photo functionality not available.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMediaCapturedForStory(Uri mediaUri, String mediaType) {
        Log.d(TAG, "onMediaCapturedForStory received in HomeActivity. URI: " + mediaUri + " Type: " + mediaType);

        AddStoryFragment addStoryFragmentInstance = AddStoryFragment.newInstance(mediaUri, mediaType);
        addStoryFragmentInstance.setAddStoryCallback(this); // Ensure callback is set

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);

        // Hide the current fragment (PhotoFragment in this case)
        if (currentFragment != null) {
            transaction.hide(currentFragment);
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for AddStoryFragment push.");
        }

        transaction.add(R.id.fragment_container, addStoryFragmentInstance, "ADD_STORY_FRAGMENT_TAG");
        transaction.addToBackStack("add_story"); // Add to back stack
        transaction.commit();

        currentFragment = addStoryFragmentInstance; // Update current fragment
        Log.d(TAG, "Navigation to AddStoryFragment from PhotoFragment success. New currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    @Override
    public void onMediaCapturedForPost(Uri mediaUri, String mediaType) {
        Log.d(TAG, "onMediaCapturedForPost received in HomeActivity. URI: " + mediaUri + " Type: " + mediaType);
        Toast.makeText(this, "Media captured for Post, navigating to creation screen.", Toast.LENGTH_LONG).show();

        // Create new instance of CreatePostFragment with media URI and type
        CreatePostFragment createPostFragmentInstance = CreatePostFragment.newInstance(mediaUri, mediaType);
        createPostFragmentInstance.setCreatePostCallback(this); // <--- NEW: Set the callback here

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);

        // Hide the current fragment (PhotoFragment in this case)
        if (currentFragment != null) {
            transaction.hide(currentFragment);
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for CreatePostFragment push.");
        }

        transaction.add(R.id.fragment_container, createPostFragmentInstance, "CREATE_POST_FRAGMENT_TAG");
        transaction.addToBackStack("create_post");
        transaction.commit();

        currentFragment = createPostFragmentInstance;
        Log.d(TAG, "Navigation to CreatePostFragment from PhotoFragment success. New currentFragment: " + currentFragment.getClass().getSimpleName());
    }

    @Override
    public void onPhotoFragmentCancelled() {
        Log.d(TAG, "Photo capture/selection cancelled. Popping back to previous fragment if applicable.");
        Toast.makeText(this, "Media selection cancelled.", Toast.LENGTH_SHORT).show();
        getSupportFragmentManager().popBackStack(); // Pop the PhotoFragment if it was pushed
    }

    // MODIFIED: navigateToHomeFragment
    private void navigateToHomeFragment(boolean shouldRefreshStories) {
        Log.d(TAG, "navigateToHomeFragment called. Should refresh stories: " + shouldRefreshStories);

        if (fragmentManager.getBackStackEntryCount() > 0 &&
                !bottomNavFragmentsMap.containsValue(currentFragment)) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Log.d(TAG, "Cleared back stack to return to a bottom nav state.");
        } else if (fragmentManager.getBackStackEntryCount() > 0 && currentFragment != homeFragment) {
            showBottomNavFragment(homeFragment);
        }

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            homeFragment.setHomeFragmentCallback(this);
        }

        if (shouldRefreshStories) {
            homeFragment.refreshStories(); // Make sure this method exists and works
        }

        if (currentFragment == homeFragment && homeFragment.isAdded() && !homeFragment.isHidden()) {
            Log.d(TAG, "HomeFragment is already current and visible. Skipping transaction.");
        } else {
            showBottomNavFragment(homeFragment);
        }

        isProgrammaticBottomNavSelection = true;
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        isProgrammaticBottomNavSelection = false;

        currentFragment = homeFragment;
    }

    @Override
    public void onStoryCreationCancelled() {
        Log.d(TAG, "Story creation cancelled. Navigating back to previous state.");
        Toast.makeText(this, "Story creation cancelled.", Toast.LENGTH_SHORT).show();
        getSupportFragmentManager().popBackStack();
    }

    // MODIFIED: onStoryPostedSuccessfully
    @Override
    public void onStoryPostedSuccessfully() {
        Log.d(TAG, "Story posted successfully. Navigating back.");
        Toast.makeText(this, "Story posted!", Toast.LENGTH_SHORT).show();
        getSupportFragmentManager().popBackStack(); // Pop the AddStoryFragment off the back stack

        // Ensure HomeFragment refreshes its stories when it becomes visible again.
        if (homeFragment != null) {
            homeFragment.refreshStories(); // 🔄 Refresh Firestore listener & UI
        } else {
            Log.e(TAG, "HomeFragment is null in onStoryPostedSuccessfully!");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastSelectedBottomNavFragment != null) {
            for (Map.Entry<Integer, Fragment> entry : bottomNavFragmentsMap.entrySet()) {
                if (entry.getValue() == lastSelectedBottomNavFragment) {
                    outState.putInt("LAST_BOTTOM_NAV_ID", entry.getKey());
                    break;
                }
            }
        }
    }

    // --- NEW: StoryViewerFragment.StoryViewerCallback implementation ---
    @Override
    public void onStoryViewerClosed() {
        Log.d(TAG, "DEBUG_FLOW: onStoryViewerClosed called by StoryViewerFragment.");

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            Log.d(TAG, "DEBUG_FLOW: StoryViewerFragment popped from back stack.");
        } else {
            Log.w(TAG, "DEBUG_FLOW: Back stack is empty, cannot pop StoryViewerFragment. This shouldn't happen if it was added with addToBackStack.");
            Fragment homeFragmentInstance = fragmentManager.findFragmentByTag("HOME_FRAGMENT");
            if (homeFragmentInstance != null && homeFragmentInstance.isHidden()) {
                fragmentManager.beginTransaction().show(homeFragmentInstance).commit();
                Log.d(TAG, "DEBUG_FLOW: HomeFragment explicitly shown as fallback.");
            } else if (!(homeFragmentInstance instanceof HomeFragment)) {
                fragmentManager.beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                Log.d(TAG, "DEBUG_FLOW: Replacing with new HomeFragment as fallback.");
            }
        }
    }

    // --- NEW: CreatePostFragment.CreatePostCallback implementation ---
    @Override
    public void onPostCreatedSuccessfully() {
        Log.d(TAG, "onPostCreatedSuccessfully called by CreatePostFragment.");
        Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show();

        // Pop the CreatePostFragment off the back stack
        getSupportFragmentManager().popBackStack();

        // Optionally, refresh the posts in HomeFragment
        if (homeFragment != null) {
            homeFragment.refreshPosts(); // <--- NEW: Assuming HomeFragment has a refreshPosts() method
            Log.d(TAG, "HomeFragment refreshPosts() called.");
        } else {
            Log.e(TAG, "HomeFragment is null in onPostCreatedSuccessfully!");
        }
    }

    @Override
    public void onPostCreationCancelled() {
        Log.d(TAG, "onPostCreationCancelled called by CreatePostFragment.");
        Toast.makeText(this, "Post creation cancelled.", Toast.LENGTH_SHORT).show();

        // Pop the CreatePostFragment off the back stack
        getSupportFragmentManager().popBackStack();
    }
}