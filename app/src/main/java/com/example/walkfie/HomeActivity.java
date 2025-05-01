package com.example.walkfie;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    View fragmentContainer;
    private FragmentManager fragmentManager;
    private Fragment currentFragment;
    private Fragment homeFragment;
    private Fragment photoFragment;
    private Fragment recordFragment;
    private Fragment mapFragment;
    private Fragment messageFragment;
    private LinearLayout bottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize FragmentManager
        fragmentManager = getSupportFragmentManager();

        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheet.setVisibility(View.GONE);

        getSupportFragmentManager().addOnBackStackChangedListener(this::onBackStackChanged);

        // Set HomeFragment as the initial fragment
        currentFragment = new HomeFragment();
        homeFragment = new HomeFragment();
        photoFragment = new PhotoFragment();
        recordFragment = new RecordFragment();
        mapFragment = new MapFragment();
        messageFragment = new MessageFragment();
        loadFragment(currentFragment);

        // Animate fragment container
        Animation fadeSlideFragment = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fragmentContainer.startAnimation(fadeSlideFragment);

        // Animate bottom navigation bar (delayed)
        Animation fadeSlideNav = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeSlideNav.setStartOffset(200);
        bottomNavigationView.startAnimation(fadeSlideNav);

        // Animate nav icons one by one (staggered)
        bottomNavigationView.post(() -> animateNavIconsStaggered());

        // Nav item selection (using if-else)
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
                // Only trigger for the RecordFragment
                if (selectedFragment == recordFragment) {
                    bottomSheet.setVisibility(View.VISIBLE);

                } else {
                    bottomSheet.setVisibility(View.GONE);
                }
            }
            return true;
        });
    }

    private void onBackStackChanged() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof RecordFragment) {
            bottomSheet.setVisibility(View.VISIBLE);
        } else {
            bottomSheet.setVisibility(View.GONE);
        }
    }

    private void animateNavIconsStaggered() {
        // BottomNavigationView menu items are wrapped in a BottomNavigationMenuView
        View menuView = bottomNavigationView.getChildAt(0);
        if (menuView != null && menuView instanceof ViewGroup) {
            ViewGroup menuViewGroup = (ViewGroup) menuView;
            for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
                View item = menuViewGroup.getChildAt(i);
                Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
                scaleIn.setStartOffset(300 + (i * 100));
                item.startAnimation(scaleIn);
            }
        }
    }


    public void triggerRecordControls() {
        if (recordFragment != null && recordFragment.isVisible()) {
            ((RecordFragment) recordFragment).showRecordControls();
        }
    }

    private void loadFragment(Fragment fragment) {
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "HOME_FRAGMENT")
                .add(R.id.fragment_container, photoFragment, "PHOTO_FRAGMENT")
                .hide(photoFragment)
                .add(R.id.fragment_container, recordFragment, "RECORD_FRAGMENT")
                .hide(recordFragment)
                .add(R.id.fragment_container, mapFragment, "MAP_FRAGMENT")
                .hide(mapFragment)
                .add(R.id.fragment_container, messageFragment, "MESSAGE_FRAGMENT")
                .hide(messageFragment)
                .commit();

        // Set HomeFragment as the initial visible fragment
        currentFragment = homeFragment;

    }
}