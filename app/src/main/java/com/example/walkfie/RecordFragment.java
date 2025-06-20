package com.example.walkfie;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Handler;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import android.graphics.drawable.Drawable;

// Callback interface so HomeActivity can talk to RecordFragment
interface RecordFragmentCallback {
    void onRecordingStarted(String activityType);
    void onRecordingStopped();
    void navigateToProfile();
}

public class RecordFragment extends Fragment implements OnMapReadyCallback, ActivityAdapter.OnActivityClickListener {

    private MapView mapView;
    private GoogleMap gMap;
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

    private LinearLayout layoutRecordingInfo;
    private TextView tvDistance, tvDuration, tvPace;
    private Button btnPause, btnCapture, btnStop;
    private Location previousLocation;
    private float totalDistance = 0f;
    private long recordingStartTime = 0L;
    private boolean isPaused = false;
    private Handler timerHandler = new Handler();
    private LinearLayout activityIconsLayout;
    private long totalPausedTime = 0L;
    private long pauseStartTime = 0L;
    private ImageView ivRecordingIcon;
    private TextView tvDialogTitle;
    private LinearLayout bottomSheetContent;

    // NEW bottom sheet vars
    private NestedScrollView activityChooserSheet;
    private BottomSheetBehavior<NestedScrollView> activityChooserBehavior;
    private int peekHeightInPx;
    private RecordFragmentCallback callback;

    // Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserProfilePicUrl;

    // Path tracking variables
    private List<LatLng> recordedPath = new java.util.ArrayList<>();
    private com.google.android.gms.maps.model.Polyline pathPolyline;

    // NEW: Last uploaded map image URL
    private String lastMapImageUrl = null;

    public void setRecordFragmentCallback(RecordFragmentCallback callback) {
        this.callback = callback;
    }

    public RecordFragment() {
        // Required empty public constructor

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        mapView = view.findViewById(R.id.mapViewRecord);

        layoutRecordingInfo = view.findViewById(R.id.layoutRecordingInfo);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvDuration = view.findViewById(R.id.tvDuration);
        tvPace = view.findViewById(R.id.tvPace);
        btnPause = view.findViewById(R.id.btnPause);
        btnCapture = view.findViewById(R.id.btnCapture);
        // Disable capture button initially
        btnCapture.setEnabled(false);
        btnStop = view.findViewById(R.id.btnStop);
        activityIconsLayout = view.findViewById(R.id.activityIconsLayout);
        ivRecordingIcon = view.findViewById(R.id.ivRecordingIcon);
        tvDialogTitle = view.findViewById(R.id.tvDialogTitle);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        ImageView ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        ivProfileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.navigateToProfile();
                } else {
                    Toast.makeText(getContext(), "Error: Navigation callback not set!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ✅ Initialize BottomSheet
        bottomSheetContent = view.findViewById(R.id.bottom_sheet);
        activityChooserSheet = view.findViewById(R.id.activityChooserSheet);
        activityChooserBehavior = BottomSheetBehavior.from(activityChooserSheet);
        int peekHeightDp = 90; // for pill visible
        float scale = getResources().getDisplayMetrics().density;
        peekHeightInPx = (int) (peekHeightDp * scale + 0.5f);
        activityChooserBehavior.setPeekHeight(peekHeightInPx);
        activityChooserBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        activityChooserBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Optional: Lock states if needed

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Optional: No-op

            }
        });

        // ✅ Setup sheet buttons (Run / Ride)
        view.findViewById(R.id.ivIcon1).setOnClickListener(v -> {
            startRecording("Run");
            // hideBottomSheet();
        });

        view.findViewById(R.id.ivIcon2).setOnClickListener(v -> {
            startRecording("Ride");
            // hideBottomSheet();
        });

        // Implement the btnPause click listener
        btnPause.setOnClickListener(v -> {
            if (isRecording) {
                if (!isPaused) {
                    // Pause recording
                    isPaused = true;
                    stopTimer(); // Stop the timer
                    if (fusedLocationClient != null && locationCallback != null) {
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                    btnPause.setText("Resume"); // Change button text
                    Toast.makeText(requireContext(), "Recording paused", Toast.LENGTH_SHORT).show();
                    pauseStartTime = System.currentTimeMillis(); // Record pause start time
                } else {
                    // Resume recording
                    isPaused = false;
                    startTimer(); // Resume the timer
                    startLocationUpdates(); // Restart location updates
                    btnPause.setText("Pause"); // Change button text back
                    Toast.makeText(requireContext(), "Recording resumed", Toast.LENGTH_SHORT).show();
                    if (pauseStartTime > 0) {
                        totalPausedTime += (System.currentTimeMillis() - pauseStartTime); // Add paused duration
                        pauseStartTime = 0L; // Reset pause start time
                    }
                }
            }
        });
        // Implement the btnCapture click listener
        btnCapture.setOnClickListener(v -> {
            if (!isRecording) {
                Toast.makeText(requireContext(), "Start recording to capture a map snapshot!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (gMap != null) {
                // Disable both Capture and Stop during upload
                btnCapture.setText("Uploading...");
                btnCapture.setEnabled(false);
                btnStop.setEnabled(false);
                gMap.snapshot(bitmap -> {
                    if (bitmap != null) {
                        uploadMapSnapshotToFirebase(bitmap, new OnMapImageUploadedListener() {
                            @Override
                            public void onSuccess(String url) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "Map snapshot uploaded!", Toast.LENGTH_SHORT).show();
                                    btnCapture.setText("Capture");
                                    btnCapture.setEnabled(true);
                                    btnStop.setEnabled(true);
                                    // Only save record if still recording
                                    if (isRecording) {
                                        saveRecordToFirestoreWithImage(url);
                                    } else {
                                        Toast.makeText(requireContext(), "Recording stopped before upload finished.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            @Override
                            public void onFailure(String error) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "Failed to upload map snapshot: " + error, Toast.LENGTH_SHORT).show();
                                    btnCapture.setText("Capture");
                                    btnCapture.setEnabled(true);
                                    btnStop.setEnabled(true);
                                });
                            }
                        });
                    } else {
                        Toast.makeText(requireContext(), "Failed to capture map snapshot", Toast.LENGTH_SHORT).show();
                        btnCapture.setText("Capture");
                        btnCapture.setEnabled(true);
                        btnStop.setEnabled(true);
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Map not ready yet", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Attempting to save record...", Toast.LENGTH_SHORT).show();
            saveRecordToFirestore();
            stopRecording();
            hideBottomSheet();
        });

        // ✅ Setup RecyclerView in sheet
        // RecyclerView recyclerView = view.findViewById(R.id.recyclerViewActivities);
        // recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // List<ActivityItem> activityList = Arrays.asList(
        // new ActivityItem("Walk", "Start Walk"),
        // new ActivityItem("Hike", "Start Hike"),
        // new ActivityItem("Swim", "Start Swim")
        // );
        // ActivityAdapter adapter = new ActivityAdapter(activityList, activity -> {
        // startRecording(activity.getName());
        // hideBottomSheet();
        // });
        // recyclerView.setAdapter(adapter);
        // recyclerView.setVisibility(View.VISIBLE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        currentUserProfilePicUrl = snapshot.getString("profilePicUrl");
                        if (currentUserProfilePicUrl != null && !currentUserProfilePicUrl.isEmpty()) {
                            Glide.with(requireContext())
                                .load(currentUserProfilePicUrl)
                                .placeholder(R.drawable.ic_default_profile_placeholder)
                                .error(R.drawable.ic_default_profile_placeholder)
                                .into(ivProfileIcon);
                        } else {
                            ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
                        }
                    }
                });
        }

        return view;
    }

    // Helper to upload map snapshot to Firebase Storage
    private void uploadMapSnapshotToFirebase(Bitmap bitmap, OnMapImageUploadedListener listener) {
        if (bitmap == null || currentUserId == null) {
            listener.onFailure("Bitmap or user ID is null");
            return;
        }
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String fileName = "map_snapshots/" + currentUserId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = ref.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot ->
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                lastMapImageUrl = uri.toString();
                listener.onSuccess(lastMapImageUrl);
            }).addOnFailureListener(e -> listener.onFailure(e.getMessage()))
        ).addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // Listener interface for upload callback
    private interface OnMapImageUploadedListener {
        void onSuccess(String url);
        void onFailure(String error);
    }

    private void saveRecordToFirestore() {
        Log.d("RecordFragment", "saveRecordToFirestore() called");
        if (db == null) {
            Log.e("RecordFragment", "Firestore db is null!");
            Toast.makeText(getContext(), "Firestore db is null!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId == null) {
            Log.e("RecordFragment", "User not logged in!");
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("RecordFragment", "recordingStartTime: " + recordingStartTime + ", recordedPath.size(): " + recordedPath.size());
        if (recordingStartTime > 0 && recordedPath.size() >= 1) {
            Toast.makeText(getContext(), "Saving record to Firestore...", Toast.LENGTH_SHORT).show();
            long duration = System.currentTimeMillis() - recordingStartTime - totalPausedTime;
            float avgPace = (duration > 0) ? (totalDistance / (duration / 1000f / 60f)) : 0f; // meters per min
            java.util.Map<String, Object> record = new java.util.HashMap<>();
            record.put("userId", currentUserId);
            record.put("activityType", currentActivityType);
            record.put("distance", totalDistance);
            record.put("duration", duration);
            record.put("avgPace", avgPace);
            record.put("timestamp", com.google.firebase.Timestamp.now());
            java.util.List<java.util.Map<String, Double>> pathList = new java.util.ArrayList<>();
            for (LatLng latLng : recordedPath) {
                java.util.Map<String, Double> point = new java.util.HashMap<>();
                point.put("lat", latLng.latitude);
                point.put("lng", latLng.longitude);
                pathList.add(point);
            }
            record.put("path", pathList);
            Log.d("RecordFragment", "Attempting to add record to Firestore: " + record.toString());
            db.collection("records").add(record)
                .addOnSuccessListener(documentReference -> {
                    Log.d("RecordFragment", "Record saved! Document ID: " + documentReference.getId());
                    Toast.makeText(getContext(), "Record saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("RecordFragment", "Failed to save record: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to save record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            Log.w("RecordFragment", "Not saving: No path or recording not started. recordingStartTime=" + recordingStartTime + ", recordedPath.size()=" + recordedPath.size());
            Toast.makeText(getContext(), "Not saving: No path or recording not started.", Toast.LENGTH_SHORT).show();
        }
    }

    // Save record with map image URL
    private void saveRecordToFirestoreWithImage(String mapImageUrl) {
        Log.d("RecordFragment", "saveRecordToFirestoreWithImage() called with mapImageUrl: " + mapImageUrl);
        if (db == null) {
            Log.e("RecordFragment", "Firestore db is null!");
            Toast.makeText(getContext(), "Firestore db is null!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId == null) {
            Log.e("RecordFragment", "User not logged in!");
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("RecordFragment", "recordingStartTime: " + recordingStartTime + ", recordedPath.size(): " + recordedPath.size());
        if (recordingStartTime > 0 && recordedPath.size() >= 1) {
            Toast.makeText(getContext(), "Saving record with image to Firestore...", Toast.LENGTH_SHORT).show();
            long duration = System.currentTimeMillis() - recordingStartTime - totalPausedTime;
            float avgPace = (duration > 0) ? (totalDistance / (duration / 1000f / 60f)) : 0f;
            java.util.Map<String, Object> record = new java.util.HashMap<>();
            record.put("userId", currentUserId);
            record.put("activityType", currentActivityType);
            record.put("distance", totalDistance);
            record.put("duration", duration);
            record.put("avgPace", avgPace);
            record.put("timestamp", com.google.firebase.Timestamp.now());
            record.put("mapImageUrl", mapImageUrl);
            java.util.List<java.util.Map<String, Double>> pathList = new java.util.ArrayList<>();
            for (LatLng latLng : recordedPath) {
                java.util.Map<String, Double> point = new java.util.HashMap<>();
                point.put("lat", latLng.latitude);
                point.put("lng", latLng.longitude);
                pathList.add(point);
            }
            record.put("path", pathList);
            Log.d("RecordFragment", "Attempting to add record with image to Firestore: " + record.toString());
            db.collection("records").add(record)
                .addOnSuccessListener(documentReference -> {
                    Log.d("RecordFragment", "Record with map image saved! Document ID: " + documentReference.getId());
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Record with map image saved!", Toast.LENGTH_SHORT).show();
                        btnCapture.setEnabled(true);
                        btnStop.setEnabled(true);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("RecordFragment", "Failed to save record with image: " + e.getMessage(), e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to save record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnCapture.setEnabled(true);
                        btnStop.setEnabled(true);
                    });
                });
        } else {
            Log.w("RecordFragment", "Not saving: No path or recording not started. recordingStartTime=" + recordingStartTime + ", recordedPath.size()=" + recordedPath.size());
            Toast.makeText(getContext(), "Not saving: No path or recording not started.", Toast.LENGTH_SHORT).show();
            btnCapture.setEnabled(true);
            btnStop.setEnabled(true);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Force activityChooserSheet and its content to be visible and fully expanded
        if (activityChooserSheet != null) {
            activityChooserSheet.setVisibility(View.VISIBLE);
            activityChooserSheet.setAlpha(1f);
        }
        if (bottomSheetContent != null) {
            bottomSheetContent.setVisibility(View.VISIBLE);
            bottomSheetContent.setAlpha(1f);
        }
        if (activityChooserBehavior != null) {
            activityChooserBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void showBottomSheet() {
        if (bottomSheetContent != null) {
            Animation slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
            bottomSheetContent.startAnimation(slideUpAnimation);
        }
        activityChooserBehavior.setPeekHeight(peekHeightInPx);
        activityChooserBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideBottomSheet() {
        if (bottomSheetContent != null) {
            Animation slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down);
            bottomSheetContent.startAnimation(slideDownAnimation);
        }
        activityChooserBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void updateRecordingMetrics(Location currentLocation) {
        if (previousLocation != null) {
            float distanceInMeters = previousLocation.distanceTo(currentLocation);
            totalDistance += distanceInMeters;
            float distanceInKilometers = totalDistance / 1000f; // Convert meters to kilometers
            tvDistance.setText(String.format("%.2f km", distanceInKilometers));
        }
        // Track path
        if (isRecording && !isPaused && currentLocation != null) {
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            recordedPath.add(latLng);
            // Toast.makeText(getContext(), "Path point added: " + latLng.latitude + ", " + latLng.longitude, Toast.LENGTH_SHORT).show();
            drawPathOnMap();
        }

        if (totalDistance > 0) // Avoid division by zero
        {
            long elapsedMillisForSpeed = (System.currentTimeMillis() - recordingStartTime) - totalPausedTime;
            long secondsForSpeed = elapsedMillisForSpeed / 1000;
            if (secondsForSpeed > 0) {
                float elapsedHours = secondsForSpeed / 3600f; // Convert seconds to hours
                float speedKmh = (totalDistance / 1000f) / elapsedHours; // Distance (km) / Time (hours)
                tvPace.setText(String.format("%.2f km/h", speedKmh)); // Renamed tvPace to reflect speed
            } else {
                tvPace.setText("0.00 km/h"); // Default value
            }
        } else {
            tvPace.setText("0.00 km/h"); // Default when no distance covered
        }
        previousLocation = currentLocation;
    }

    private void drawPathOnMap() {
        if (gMap == null || recordedPath.isEmpty()) return;
        if (pathPolyline != null) pathPolyline.remove();
        pathPolyline = gMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions()
                .addAll(recordedPath)
                .color(android.graphics.Color.BLUE)
                .width(10f));
    }

    private void startRecording(String activityType) {
        currentActivityType = activityType;
        isRecording = true;
        isPaused = false;
        previousLocation = null;
        totalDistance = 0f;
        totalPausedTime = 0L;
        recordingStartTime = System.currentTimeMillis();

        tvDistance.setText("0.00 km");
        tvDuration.setText("00:00:00");
        tvPace.setText("0.00 km/h");

        // Show and set the recording icon
        if (ivRecordingIcon != null) {
            ivRecordingIcon.setVisibility(View.VISIBLE);
            if (activityType.equals("Run")) {
                ivRecordingIcon.setImageResource(R.drawable.run);
            } else if (activityType.equals("Ride")) {
                ivRecordingIcon.setImageResource(R.drawable.ride);
            }
        }

        // Hide the "Record Activity" text
        if (tvDialogTitle != null) {
            tvDialogTitle.setVisibility(View.GONE);
        }

        if (activityIconsLayout != null && layoutRecordingInfo != null && activityChooserSheet != null && activityChooserBehavior != null) {
            // Prepare layoutRecordingInfo
            layoutRecordingInfo.setVisibility(View.VISIBLE);
            layoutRecordingInfo.setAlpha(0f);
            layoutRecordingInfo.setTranslationY(layoutRecordingInfo.getHeight()); // Start below

            // Animate activityIconsLayout to slide down and out
            activityIconsLayout.animate()
                    .translationY(activityIconsLayout.getHeight())
                    .alpha(0f)
                    .setDuration(300) // Shorter duration for the hide
                    .withEndAction(() -> {
                        activityIconsLayout.setVisibility(View.GONE);

                        // Delay the expansion of the bottom sheet
                        new Handler().postDelayed(() -> {
                            activityChooserBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                            // Animate layoutRecordingInfo to slide up and in
                            layoutRecordingInfo.animate()
                                    .translationY(0)
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                        }, 300); // Adjust the delay (in milliseconds) as needed
                    })
                    .start();
        } else if (layoutRecordingInfo != null && activityChooserBehavior != null) {
            // Fallback
            layoutRecordingInfo.setVisibility(View.VISIBLE);
            layoutRecordingInfo.setAlpha(0f);
            layoutRecordingInfo.setTranslationY(layoutRecordingInfo.getHeight());
            activityChooserBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            layoutRecordingInfo.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(300)
                    .start();
        }

        // Set initial text for pause button
        if (btnPause != null) {
            btnPause.setText("Pause");
        }
        Toast.makeText(requireContext(), activityType + " recording started", Toast.LENGTH_SHORT).show();

        if (callback != null) {
            callback.onRecordingStarted(activityType);
        }
        startLocationUpdates();
        startTimer();

        // Keep the bottom sheet collapsed initially
        activityChooserBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // Modify stopRecording
    private void stopRecording() {
        isRecording = false;
        isPaused = false; // Reset paused state

        // Hide the recording information layout with animation
        if (layoutRecordingInfo != null) {
            layoutRecordingInfo.animate()
                    .translationY(layoutRecordingInfo.getHeight()) // Slide down and out
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> layoutRecordingInfo.setVisibility(View.GONE))
                    .start();
        }
        // Hide the recording icon
        if (ivRecordingIcon != null) {
            ivRecordingIcon.setVisibility(View.GONE);
        }

        // Show the "Record Activity" text again
        if (tvDialogTitle != null) {
            tvDialogTitle.setVisibility(View.VISIBLE);
        }

        // Show the activity icons layout again with animation
        if (activityIconsLayout != null) {
            activityIconsLayout.setVisibility(View.VISIBLE);
            activityIconsLayout.setAlpha(0f);
            activityIconsLayout.setTranslationY(0); // Reset translationY to bring it back to its original position
            activityIconsLayout.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        // Ensure the bottom sheet is in a state where the icons are visible
                        if (activityChooserBehavior != null) {
                            activityChooserBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            // Optionally, fully expand it if that was the previous state

                            // activityChooserBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    })
                    .start();
        }
        Toast.makeText(requireContext(), "Recording stopped", Toast.LENGTH_SHORT).show();

        if (callback != null) {
            callback.onRecordingStopped();
        }

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);

        // Stop the timer
        stopTimer();

        // Reset recording data
        previousLocation = null;
        totalPausedTime = 0L;
        totalDistance = 0f;
        recordingStartTime = 0L;
        // Clear path
        recordedPath.clear();
        if (pathPolyline != null) {
            pathPolyline.remove();
            pathPolyline = null;
        }
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording && !isPaused && recordingStartTime > 0) {
                long currentTime = System.currentTimeMillis();
                long elapsedMillis = (currentTime - recordingStartTime) - totalPausedTime;
                long seconds = (elapsedMillis / 1000) % 60;
                long minutes = (elapsedMillis / (1000 * 60)) % 60;
                long hours = elapsedMillis / (1000 * 60 * 60);

                tvDuration.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000); // Update every second
            }
        }
    };

    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onActivityClick(ActivityItem activity) {
        // startRecording(activity.getName());
        // hideBottomSheet();
        layoutRecordingInfo.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        gMap.getUiSettings().setZoomControlsEnabled(false);
        gMap.getUiSettings().setMyLocationButtonEnabled(false);

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

        // Added: Attempt to center on the last known location immediately if available and permission granted
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    lastKnownLocation = location;
                    centerMapOnUser(location);
                    isFirstLoad = false; // Mark as first load complete
                }
            });
        }
    }

    private void centerMapOnUser(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(userLatLng, 16f);
        gMap.animateCamera(update);
        lastCameraPosition = userLatLng;
    }

    private void checkLocationPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    // Modify the locationCallback
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setMaxWaitTime(100);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(getContext(), "No location updates received!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Location location = locationResult.getLastLocation();
                lastKnownLocation = location; // Update last known location

                if (location != null) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // if (userMarker == null) {
                    // userMarker = gMap.addMarker(new MarkerOptions().position(latLng).title("You"));
                    // } else {
                    // userMarker.setPosition(latLng);
                    // }

                    // Add this check for initial centering
                    if (isFirstLoad) {
                        centerMapOnUser(location);
                        isFirstLoad = false; // Mark as first load complete after centering
                    } else if (isFollowingUser) {
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                    }

                    // Only update metrics if recording AND NOT paused
                    if (isRecording && !isPaused) {
                        updateRecordingMetrics(location);
                    }
                    setCustomUserMarker(latLng);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission not granted!", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        gMap.setMyLocationEnabled(true);
    }

    // Helper to create a pin-shaped marker with profile image
    private void setCustomUserMarker(LatLng latLng) {
        if (currentUserProfilePicUrl == null || getContext() == null || gMap == null) return;
        Glide.with(requireContext())
            .asBitmap()
            .load(currentUserProfilePicUrl)
            .circleCrop()
            .into(new CustomTarget<Bitmap>(120, 120) {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    Bitmap markerBitmap = createPinMarkerBitmap(resource);
                    if (userMarker == null) {
                        userMarker = gMap.addMarker(new MarkerOptions().position(latLng).icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerBitmap)).anchor(0.5f, 1f));
                    } else {
                        userMarker.setPosition(latLng);
                        userMarker.setIcon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(markerBitmap));
                    }
                }
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
    }

    // Helper to create a pin-shaped bitmap with the profile image
    private Bitmap createPinMarkerBitmap(Bitmap profileBitmap) {
        int width = 120, height = 160;
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Draw pin shape
        Path pinPath = new Path();
        pinPath.moveTo(width / 2f, height);
        pinPath.cubicTo(width * 0.1f, height * 0.7f, width * 0.1f, height * 0.3f, width / 2f, width / 2f);
        pinPath.cubicTo(width * 0.9f, height * 0.3f, width * 0.9f, height * 0.7f, width / 2f, height);
        paint.setColor(0xFF222222); // dark pin
        canvas.drawPath(pinPath, paint);
        // Draw white circle for profile
        paint.setColor(0xFFFFFFFF);
        float cx = width / 2f, cy = width / 2f, radius = width * 0.4f;
        canvas.drawCircle(cx, cy, radius + 6, paint);
        // Draw profile image as circle
        BitmapShader shader = new BitmapShader(profileBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
        return output;
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
        showBottomSheet();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        stopTimer();
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
        stopTimer();
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

    @Override
    public void onDestroyView() {
        // Clean up mapView and handlers to prevent view persistence/memory leaks
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
        }
        if (recenterHandler != null) {
            recenterHandler.removeCallbacksAndMessages(null);
        }
        callback = null;
        super.onDestroyView();
    }
}