package com.example.walkfie;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class RecordFragment extends Fragment implements OnMapReadyCallback, ActivityAdapter.OnActivityClickListener {

    private MapView mapView;
    private GoogleMap gMap;

    private View recordingOverlay;
    private Button btnStartStop;
    private View bottomNavigationView;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final int LOCATION_PERMISSION_CODE = 1002;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker userMarker;
    private LatLng lastCameraPosition;
    private boolean isFirstLoad = true;
    private Handler recenterHandler = new Handler();
    private Runnable recenterRunnable;
    private boolean isFollowingUser = true;
    private boolean isRecording = false;
    private String currentActivityType = "";
    private Location lastKnownLocation;
    private LinearLayout bottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private RecyclerView recyclerViewActivities;
    private ActivityAdapter activityAdapter;
    private ImageView ivIcon1;
    private ImageView ivIcon2;
    private TextView tvDialogTitle;
    private LinearLayout activityIconsLayout;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_record, container, false);

        mapView = view.findViewById(R.id.mapViewRecord);
        recordingOverlay = view.findViewById(R.id.recordingOverlay);
        btnStartStop = view.findViewById(R.id.btnStartStop);

        // Customize the dialog's window to position it at the top
        View sheetView = requireActivity().findViewById(R.id.bottom_sheet);

        // Get references to your buttons inside the dialog layout
        ivIcon1 = sheetView.findViewById(R.id.ivIcon1);
        ivIcon2 = sheetView.findViewById(R.id.ivIcon2);
        tvDialogTitle = sheetView.findViewById(R.id.tvDialogTitle);

        // Set click listeners
        ivIcon1.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            startRecording("Run");
        });

        ivIcon2.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            startRecording("Ride");
        });

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnStartStop.setOnClickListener(v -> {
            if (!isRecording) {
                showActivityChooserDialog();
            } else {
                stopRecording();
            }
        });

        // get BottomNavigationView from the parent activity
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigation);

        // Get the layout from the activity
        bottomSheet = requireActivity().findViewById(R.id.bottom_sheet);

        // Get the behavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Customize the behavior
        bottomSheetBehavior.setPeekHeight(bottomNavigationView.getHeight());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // React to state change
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {

                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // React to dragging events
                if (slideOffset > 0) {
                    bottomSheetBehavior.setPeekHeight(bottomNavigationView.getHeight(), true);
                    mapView.setClickable(false);
                    mapView.setFocusable(false);
                } else {
                    bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight(), true);
                    mapView.setClickable(true);
                    mapView.setFocusable(true);
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the layout from the activity
        bottomSheet = requireActivity().findViewById(R.id.bottom_sheet);

        // Get the behavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Customize the behavior
        bottomSheetBehavior.setPeekHeight(bottomNavigationView.getHeight());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // React to state change
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {

                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // React to dragging events
                if (slideOffset > 0) {
                    bottomSheetBehavior.setPeekHeight(bottomNavigationView.getHeight(), true);
                    mapView.setClickable(false);
                    mapView.setFocusable(false);
                } else {
                    bottomSheetBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO, true);
                    mapView.setClickable(true);
                    mapView.setFocusable(true);
                }
            }
        });

        // Get the width of the parent
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        // Set the width of the bottom sheet
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.width = screenWidth;
        bottomSheet.setLayoutParams(layoutParams);
        // Set the bottom sheet's state to be hidden by default
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void showActivityChooserDialog() {
        bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight(), true);

        // Set the state to collapsed
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void centerMapOnUser(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(userLatLng, 16f);
        gMap.animateCamera(update);
        lastCameraPosition = userLatLng;
    }


    private void startRecording(String activityType) {
        recordingOverlay.setVisibility(View.VISIBLE);
        btnStartStop.setText("Stop");
        isRecording = true;
        currentActivityType = activityType;
        Toast.makeText(requireContext(), activityType + " recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        recordingOverlay.setVisibility(View.VISIBLE);
        btnStartStop.setText("Start");
        isRecording = false; // <-- add this!
        Toast.makeText(requireContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityClick(ActivityItem activity) {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        startRecording(activity.getName());
    }

    public void showRecordControls() {
        if(recordingOverlay != null && btnStartStop != null){
            recordingOverlay.setVisibility(View.VISIBLE);
            btnStartStop.setVisibility(View.VISIBLE);
            // Show dialog when fragment loads
            showActivityChooserDialog();
        }
    }

    public void hideRecordControls() {
        if(recordingOverlay != null && btnStartStop != null){
            recordingOverlay.setVisibility(View.GONE);
            btnStartStop.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.getUiSettings().setZoomControlsEnabled(true);
        gMap.getUiSettings().setMyLocationButtonEnabled(true);

        gMap.setOnCameraMoveListener(() -> {
            isFollowingUser = false;
            recenterHandler.removeCallbacks(recenterRunnable);
            recenterRunnable = () -> {
                if (lastKnownLocation != null) {
                    centerMapOnUser(lastKnownLocation);
                    isFollowingUser = true;
                }
            };
            recenterHandler.postDelayed(recenterRunnable, 1000);
        });


        checkLocationPermissionAndStartUpdates();
    }

    private void checkLocationPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000); // every 2 seconds
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(100); // don't delay batch delivery

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                lastKnownLocation = location;
                if (isFollowingUser || isFirstLoad) {
                    centerMapOnUser(location);
                    isFirstLoad = false;
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        gMap.setMyLocationEnabled(true);  // optional blue dot
    }

    private Location getBestLocation(LocationResult locationResult) {
        Location bestLocation = null;
        for (Location location : locationResult.getLocations()) {
            if (location != null) {
                if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = location;
                }
            }
        }
        return bestLocation;
    }

    private void updateUserLocation(Location location) {
        if (location == null) return;

        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userMarker == null) {
            userMarker = gMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .title("You are here"));
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
        } else {
            userMarker.setPosition(userLatLng);
            gMap.animateCamera(CameraUpdateFactory.newLatLng(userLatLng));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }
}