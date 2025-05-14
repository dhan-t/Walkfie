// HomeActivity.java — FINAL VERSION
package com.example.walkfie;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity implements RecordFragmentCallback {  // ✅ IMPLEMENT CALLBACK

    BottomNavigationView bottomNavigationView;
    View fragmentContainer;
    private FragmentManager fragmentManager;
    private Fragment currentFragment;
    private HomeFragment homeFragment;
    private PhotoFragment photoFragment;
    private RecordFragment recordFragment;
    private MapFragment mapFragment;
    private MessageFragment messageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        fragmentManager = getSupportFragmentManager();

        // Initialize fragments
        homeFragment = new HomeFragment();
        photoFragment = new PhotoFragment();
        recordFragment = new RecordFragment();
        mapFragment = new MapFragment();
        messageFragment = new MessageFragment();

        // ✅ Pass callback to RecordFragment (IMPORTANT!)
        recordFragment.setRecordFragmentCallback(this);

        loadFragments();
        setupAnimations();
        setupNavigation();
    }

    private void loadFragments() {
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "HOME_FRAGMENT")
                .add(R.id.fragment_container, photoFragment, "PHOTO_FRAGMENT").hide(photoFragment)
                .add(R.id.fragment_container, recordFragment, "RECORD_FRAGMENT").hide(recordFragment)
                .add(R.id.fragment_container, mapFragment, "MAP_FRAGMENT").hide(mapFragment)
                .add(R.id.fragment_container, messageFragment, "MESSAGE_FRAGMENT").hide(messageFragment)
                .commit();

        currentFragment = homeFragment;
    }

    private void setupAnimations() {
        Animation fadeSlideFragment = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fragmentContainer.startAnimation(fadeSlideFragment);

        Animation fadeSlideNav = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeSlideNav.setStartOffset(200);
        bottomNavigationView.startAnimation(fadeSlideNav);

        bottomNavigationView.post(this::animateNavIconsStaggered);
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = homeFragment;
            } else if (itemId == R.id.nav_photo) {
                selectedFragment = photoFragment;
            } else if (itemId == R.id.nav_record) {
                selectedFragment = recordFragment;
            } else if (itemId == R.id.nav_map) {
                selectedFragment = mapFragment;
            } else if (itemId == R.id.nav_message) {
                selectedFragment = messageFragment;
            }

            if (selectedFragment != null && currentFragment != selectedFragment) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .hide(currentFragment)
                        .show(selectedFragment)
                        .commit();
                currentFragment = selectedFragment;
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

    // ✅ Callback methods (optional but clean)
    @Override
    public void onRecordingStarted(String activityType) {
        // Optional: react when recording starts
    }

    @Override
    public void onRecordingStopped() {
        // Optional: react when recording stops
    }
}
