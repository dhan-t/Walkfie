package com.example.walkfie;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class RecordFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap gMap;

    private View recordingOverlay;
    private Button btnStartStop;

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

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Call this to show the controls right away
    }

    private void showActivityChooserDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_activity_chooser, null);
        dialog.setContentView(sheetView);

        Button btnRun = sheetView.findViewById(R.id.btnRun);
        Button btnRide = sheetView.findViewById(R.id.btnRide);

        btnRun.setOnClickListener(v -> {
            dialog.dismiss();
            startRecording("Run");
        });

        btnRide.setOnClickListener(v -> {
            dialog.dismiss();
            startRecording("Ride");
        });

        dialog.show();
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
        isRecording = true; // <-- add this!
        currentActivityType = activityType;
        Toast.makeText(requireContext(), activityType + " recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        recordingOverlay.setVisibility(View.VISIBLE);
        btnStartStop.setText("Start");
        isRecording = false; // <-- add this!
        Toast.makeText(requireContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
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