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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MODIFIED: Added StoryViewerFragment.StoryViewerCallback to the implemented interfaces
public class HomeActivity extends AppCompatActivity implements
        RecordFragmentCallback, MapFragmentCallback, ProfileFragmentCallback,
        MessageFragmentCallback, HomeFragmentCallback, EditProfileCallback,
        AddStoryCallback, PhotoFragmentCallback,
        StoryViewerFragment.StoryViewerCallback { // <--- NEW: Implements StoryViewerCallback

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
    private AddStoryFragment addStoryFragment; // NEW
    private CreatePostFragment createPostFragment; // NEW
    private FindFriendsFragment findFriendsFragment; // NEW
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
            createPostFragment = new CreatePostFragment();
            findFriendsFragment = new FindFriendsFragment();

            bottomNavFragmentsMap = new HashMap<>();
            bottomNavFragmentsMap.put(R.id.nav_home, homeFragment);
            bottomNavFragmentsMap.put(R.id.nav_photo, photoFragment);
            bottomNavFragmentsMap.put(R.id.nav_record, recordFragment);
            bottomNavFragmentsMap.put(R.id.nav_map, mapFragment);
            bottomNavFragmentsMap.put(R.id.nav_message, messageFragment);

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
                // Back stack is NOT empty: a pushed fragment (e.g., StoryViewer, EditProfile) is on top
                Log.d(TAG, "Back stack is not empty. A pushed fragment is on top. Hiding bottom nav.");

                // Hide the bottom navigation bar when a non-bottom-nav fragment is on top
                bottomNavigationView.setVisibility(View.GONE);

                // IMPORTANT: DO NOT PROGRAMMATICALLY SET SELECTED ITEM HERE.
                // This was the root cause of the immediate popBackStack(null, POP_BACK_STACK_INCLUSIVE).
                // bottomNavigationView.setSelectedItemId(0); // This line is intentionally REMOVED
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

// --- End of Part 1 ---

// --- Start of Part 2 ---

    @Override // Implementing the method from MapFragmentCallback
    public void navigateToProfile() {
        Log.d(TAG, "navigateToProfile called. currentFragment: " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
        // Check if ProfileFragment is already the current top fragment to avoid re-adding
        if (currentFragment == profileFragment && profileFragment.isAdded() && !profileFragment.isHidden()) {
            Log.d(TAG, "Already on ProfileFragment and visible. Skipping navigation.");
            return;
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

        // REMOVED: bottomNavigationView.setSelectedItemId(0);
        // The onBackStackChangedListener now handles bottom nav visibility.
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

        // REMOVED: bottomNavigationView.setSelectedItemId(0);
        // The onBackStackChangedListener now handles bottom nav visibility.
    }

    @Override
    public void navigateToSettings() {
        Toast.makeText(this, "Navigate to Settings Screen (Not implemented)", Toast.LENGTH_SHORT).show();
        // Example implementation for a new fragment:
        // SettingsFragment settingsFragment = new SettingsFragment();
        // FragmentTransaction transaction = fragmentManager.beginTransaction();
        // transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
        // if (currentFragment != null) transaction.hide(currentFragment);
        // transaction.add(R.id.fragment_container, settingsFragment, "SETTINGS_FRAGMENT_TAG");
        // transaction.addToBackStack("settings_fragment");
        // transaction.commit();
        // currentFragment = settingsFragment;
        // REMOVED: bottomNavigationView.setSelectedItemId(0);
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
    public void openPostDetails(PostAdapter.PostItem post) {
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
        // REMOVED: bottomNavigationView.setSelectedItemId(0);
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

        // OPTIONAL: If StoryViewerFragment needs to notify the activity when closed, set a callback.
        // It's good practice to set it here right after newInstance.
        // The StoryViewerFragment.onAttach will handle setting its internal callback reference if this is uncommented.
        // storyViewerFragment.setCallback(this); // You would need a setCallback method in StoryViewerFragment

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

        // IMPORTANT: DO NOT PROGRAMMATICALLY SET bottomNavigationView.setSelectedItemId(0) HERE!
        // The onBackStackChangedListener now handles bottom nav visibility.
    }

    @Override
    public void openFriendProfile(FriendsAdapter.FriendItem friend) {
        Toast.makeText(this, "Opening Friend Profile for: " + friend.getUsername(), Toast.LENGTH_SHORT).show();
        // Implement actual navigation to another ProfileFragment showing the friend's profile
        // This would involve creating a new instance of ProfileFragment (or a similar fragment)
        // and passing the friend's UID as an argument.
        // If you want to reuse ProfileFragment, you'd need a factory method like ProfileFragment.newInstance(userId)
        // ProfileFragment friendProfileFragment = ProfileFragment.newInstance(friend.getUserId());
        // FragmentTransaction transaction = fragmentManager.beginTransaction();
        // transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
        // if (currentFragment != null) transaction.hide(currentFragment);
        // transaction.add(R.id.fragment_container, friendProfileFragment, "FRIEND_PROFILE_FRAGMENT_TAG");
        // transaction.addToBackStack("friend_profile");
        // transaction.commit();
        // currentFragment = friendProfileFragment;
        // REMOVED: bottomNavigationView.setSelectedItemId(0);
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
        // Example:
        // ChatConversationFragment chatConversationFragment = ChatConversationFragment.newInstance(chatPartnerName);
        // FragmentTransaction transaction = fragmentManager.beginTransaction();
        // transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
        // if (currentFragment != null) transaction.hide(currentFragment);
        // transaction.add(R.id.fragment_container, chatConversationFragment, "CHAT_CONVERSATION_FRAGMENT_TAG");
        // transaction.addToBackStack("chat_conversation");
        // transaction.commit();
        // currentFragment = chatConversationFragment;
        // REMOVED: bottomNavigationView.setSelectedItemId(0);
    }

    @Override
    public void navigateToProfileFromHome() {
        navigateToProfile(); // Reuse the existing navigateToProfile method for consistency
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

        // REMOVED: bottomNavigationView.setSelectedItemId(0);
        // The onBackStackChangedListener now handles bottom nav visibility.
    }

    @Override
    public void navigateToPostCreationFromHome() {
        Log.d(TAG, "navigateToPostCreationFromHome called.");
        if (createPostFragment == null) {
            createPostFragment = new CreatePostFragment();
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        if (currentFragment != null) {
            transaction.hide(currentFragment); // Hide the current fragment (HomeFragment)
            Log.d(TAG, "Hiding " + currentFragment.getClass().getSimpleName() + " for CreatePostFragment push.");
        }
        if (!createPostFragment.isAdded()) {
            transaction.add(R.id.fragment_container, createPostFragment, "CREATE_POST_FRAGMENT_TAG");
            Log.d(TAG, "Adding CreatePostFragment.");
        } else {
            transaction.show(createPostFragment);
            Log.d(TAG, "Showing existing CreatePostFragment.");
        }
        transaction.addToBackStack("create_post"); // Add to back stack
        transaction.commit();
        currentFragment = createPostFragment; // Update current fragment
        Log.d(TAG, "Navigation to CreatePostFragment committed. New currentFragment: " + currentFragment.getClass().getSimpleName());

        // REMOVED: bottomNavigationView.setSelectedItemId(0);
        // The onBackStackChangedListener now handles bottom nav visibility.
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

        // Navigate to AddStoryFragment with the actual media URI and media type
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

        // REMOVED: bottomNavigationView.setSelectedItemId(0);
        // The onBackStackChangedListener now handles bottom nav visibility.
    }
    @Override
    public void onMediaCapturedForPost(Uri mediaUri, String mediaType) {
        Log.d(TAG, "onMediaCapturedForPost received in HomeActivity. URI: " + mediaUri + " Type: " + mediaType);
        Toast.makeText(this, "Media captured for Post (Not implemented yet): " + mediaUri.getLastPathSegment(), Toast.LENGTH_LONG).show();

        // Implement navigation to CreatePostFragment here, similar to AddStoryFragment
        CreatePostFragment createPostFragmentInstance = CreatePostFragment.newInstance(mediaUri, mediaType);
        // createPostFragmentInstance.setCreatePostCallback(this); // Set its callback if you have one

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
        // REMOVED: bottomNavigationView.setSelectedItemId(0);
        // The onBackStackChangedListener now handles bottom nav visibility.
    }

    @Override
    public void onPhotoFragmentCancelled() {
        Log.d(TAG, "Photo capture/selection cancelled. Popping back to previous fragment if applicable.");
        Toast.makeText(this, "Media selection cancelled.", Toast.LENGTH_SHORT).show();

        // This implies navigating back, so a simple popBackStack should suffice
        // The onBackStackChangedListener will then handle showing the correct bottom nav fragment.
        getSupportFragmentManager().popBackStack(); // Pop the PhotoFragment if it was pushed
    }

    // MODIFIED: navigateToHomeFragment
    private void navigateToHomeFragment(boolean shouldRefreshStories) {
        Log.d(TAG, "navigateToHomeFragment called. Should refresh stories: " + shouldRefreshStories);

        // This method is now primarily for ensuring HomeFragment is visible and potentially refreshing it.
        // It should clear the back stack only if there are non-bottom-nav fragments on top.

        // Clear the back stack ONLY IF the current fragment is NOT a bottom nav fragment
        // OR if the target is specifically Home and there's something else on top.
        if (fragmentManager.getBackStackEntryCount() > 0 &&
                !bottomNavFragmentsMap.containsValue(currentFragment)) {
            // If a non-bottom nav fragment is on top, pop everything until a bottom nav fragment is revealed
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Log.d(TAG, "Cleared back stack to return to a bottom nav state.");
            // The onBackStackChangedListener will handle displaying the correct bottom nav fragment
            // (likely HomeFragment if it was the last selected or is the default)
            // and showing the bottom nav bar.
        } else if (fragmentManager.getBackStackEntryCount() > 0 && currentFragment != homeFragment) {
            // If a different bottom nav fragment is on top, ensure HomeFragment is visible
            showBottomNavFragment(homeFragment);
        }


        // Ensure homeFragment instance is valid and callback is set
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            homeFragment.setHomeFragmentCallback(this); // Ensure callback is set for new instance
        }

        if (shouldRefreshStories) {
            homeFragment.refreshStories(); // Make sure this method exists and works
        }

        // If HomeFragment is already current and visible, no need for transaction
        if (currentFragment == homeFragment && homeFragment.isAdded() && !homeFragment.isHidden()) {
            Log.d(TAG, "HomeFragment is already current and visible. Skipping transaction.");
        } else {
            // Ensure HomeFragment is the visible one via show/hide (or add if not added)
            showBottomNavFragment(homeFragment);
        }

        // Programmatically select the home tab to update UI, but prevent listener's back stack clear
        isProgrammaticBottomNavSelection = true;
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        isProgrammaticBottomNavSelection = false;

        currentFragment = homeFragment; // Explicitly set currentFragment after this operation
    }


    @Override
    public void onStoryCreationCancelled() {
        Log.d(TAG, "Story creation cancelled. Navigating back to previous state.");
        Toast.makeText(this, "Story creation cancelled.", Toast.LENGTH_SHORT).show();

        // A simple popBackStack should handle navigating from AddStoryFragment back to PhotoFragment
        // or whatever was before it. The onBackStackChangedListener will then react.
        getSupportFragmentManager().popBackStack();
    }

    // MODIFIED: onStoryPostedSuccessfully
    @Override
    public void onStoryPostedSuccessfully() {
        Log.d(TAG, "Story posted successfully. Navigating back.");
        Toast.makeText(this, "Story posted!", Toast.LENGTH_SHORT).show();

        // Pop the AddStoryFragment off the back stack
        getSupportFragmentManager().popBackStack(); // This will trigger onBackStackChangedListener

        // The onBackStackChangedListener should now handle returning to the previous fragment
        // (which should be the HomeFragment or PhotoFragment depending on the flow)
        // and showing the bottom nav if the stack is now empty.

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

        // This method is called when the StoryViewerFragment wants to close itself.
        // We simply pop it from the back stack.
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(); // Pop the StoryViewerFragment
            Log.d(TAG, "DEBUG_FLOW: StoryViewerFragment popped from back stack.");
        } else {
            Log.w(TAG, "DEBUG_FLOW: Back stack is empty, cannot pop StoryViewerFragment. This shouldn't happen if it was added with addToBackStack.");
            // Fallback if somehow it wasn't added to the back stack (e.g., initial state)
            // If the HomeFragment was hidden, make sure to show it again
            Fragment homeFragmentInstance = fragmentManager.findFragmentByTag("HOME_FRAGMENT"); // Assuming "HOME_FRAGMENT" is its tag
            if (homeFragmentInstance != null && homeFragmentInstance.isHidden()) {
                fragmentManager.beginTransaction().show(homeFragmentInstance).commit();
                Log.d(TAG, "DEBUG_FLOW: HomeFragment explicitly shown as fallback.");
            } else if (!(homeFragmentInstance instanceof HomeFragment)) {
                // If there's no HomeFragment or it's a different one, replace it (less ideal for back stack flow)
                // This scenario means something unexpected happened, typically you'd want to return to a known state.
                fragmentManager.beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                Log.d(TAG, "DEBUG_FLOW: Replacing with new HomeFragment as fallback.");
            }
        }
        // The onBackStackChangedListener will handle showing the bottom nav bar again
        // and setting the currentFragment once the stack changes.
    }
}