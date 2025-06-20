package com.example.walkfie;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.video.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.RecordingStats;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView; // Correct PreviewView import
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia;
import androidx.core.util.Consumer; // Explicitly import java.util.function.Consumer

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

interface PhotoFragmentCallback {
    void onMediaCapturedForStory(Uri mediaUri, String mediaType); // ADDED mediaType
    void onMediaCapturedForPost(Uri mediaUri, String mediaType);   // ADDED mediaType
    void onPhotoFragmentCancelled();
}

public class PhotoFragment extends Fragment {

    private static final String TAG = "PhotoFragment";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
            // Removed Manifest.permission.READ_EXTERNAL_STORAGE
            // If you need to write to external storage (other than MediaStore), consider
            // managing files in app-specific directories or using Storage Access Framework.
            // For saving photos/videos with CameraX, MediaStore is the way.
    };

    private static final String ARG_MEDIA_TYPE_MODE = "media_type_mode";

    private PreviewView viewFinder;
    private ImageButton btnCapture;
    private ImageButton btnGallery;
    private TextView tvLocation;
    private TextView tvStory, tvPost;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location lastKnownLocation;

    private String currentMediaType = "Story";

    private ImageCapture imageCapture;
    private VideoCapture videoCaptureUseCase;
    private Recorder videoRecorder;
    private Recording currentRecording;
    private ExecutorService cameraExecutor;

    private PhotoFragmentCallback callback;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    public PhotoFragment() {
        // Required empty public constructor
    }

    public static PhotoFragment newInstance(String initialMediaType) {
        PhotoFragment fragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_TYPE_MODE, initialMediaType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PhotoFragmentCallback) {
            callback = (PhotoFragmentCallback) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PhotoFragmentCallback");
        }

        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), // Assuming you've switched to PickVisualMedia
                uri -> {
                    if (uri != null) {
                        Log.d(TAG, "Media picked from gallery: " + uri.toString());
                        // Determine media type for callback (essential here, as it could be image or video)
                        String mediaType = "unknown";
                        if (uri.toString().startsWith("content://")) {
                            String mimeType = requireContext().getContentResolver().getType(uri);
                            if (mimeType != null) {
                                if (mimeType.startsWith("image")) mediaType = "image";
                                else if (mimeType.startsWith("video")) mediaType = "video";
                            }
                        }

                        if ("Story".equals(currentMediaType) && callback != null) {
                            callback.onMediaCapturedForStory(uri, mediaType); // <-- Pass mediaType
                        } else if ("Post".equals(currentMediaType) && callback != null) {
                            callback.onMediaCapturedForPost(uri, mediaType); // <-- Pass mediaType
                        }
                    } else {
                        Log.d(TAG, "No media selected from gallery.");
                        Toast.makeText(getContext(), "No media selected.", Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onPhotoFragmentCancelled();
                        }
                    }
                });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (getArguments() != null) {
            currentMediaType = getArguments().getString(ARG_MEDIA_TYPE_MODE, "Story");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Fragment view being created.");
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        viewFinder = view.findViewById(R.id.viewFinder);
        btnCapture = view.findViewById(R.id.btnCapture);
        btnGallery = view.findViewById(R.id.btnGallery);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvStory = view.findViewById(R.id.tvStory);
        tvPost = view.findViewById(R.id.tvPost);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Fragment view created and initialized.");

        if (allPermissionsGranted()) {
            Log.d(TAG, "All permissions granted. Starting camera and location updates.");
            startCamera();
            startLocationUpdates();
        } else {
            Log.d(TAG, "Not all permissions granted. Requesting permissions.");
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // FIX: Ensure this method is public in PhotoFragment
        updateMediaTypeSelection(currentMediaType);

        tvStory.setOnClickListener(v -> updateMediaTypeSelection("Story"));
        tvPost.setOnClickListener(v -> updateMediaTypeSelection("Post"));

        btnCapture.setOnClickListener(v -> takePhoto());
        btnCapture.setOnLongClickListener(v -> {
            if (currentRecording == null) {
                startVideoRecording();
            } else {
                Toast.makeText(requireContext(), "Video recording manually stopped.", Toast.LENGTH_SHORT).show();
                stopVideoRecording();
            }
            return true;
        });

        btnGallery.setOnClickListener(v -> pickMediaFromGallery());
    }

    // FIX: This method MUST be public
    public void updateMediaTypeSelection(String selectedType) {
        currentMediaType = selectedType;
        Log.d(TAG, "Media type selected: " + selectedType);

        tvStory.setBackgroundResource(0);
        tvStory.setTextColor(Color.parseColor("#80FFFFFF"));
        tvStory.setTypeface(null, Typeface.NORMAL);

        tvPost.setBackgroundResource(0);
        tvPost.setTextColor(Color.parseColor("#80FFFFFF"));
        tvPost.setTypeface(null, Typeface.NORMAL);

        if ("Story".equals(selectedType)) {
            tvStory.setBackgroundResource(R.drawable.selected_tab_background);
            tvStory.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
            tvStory.setTypeface(null, Typeface.BOLD);
        } else if ("Post".equals(selectedType)) {
            tvPost.setBackgroundResource(R.drawable.selected_tab_background);
            tvPost.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
            tvPost.setTypeface(null, Typeface.BOLD);
        }
    }

    private void startCamera() {
        Log.d(TAG, "startCamera: Attempting to start CameraX.");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Log.d(TAG, "ProcessCameraProvider obtained successfully. Binding use cases.");
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException e) {
                Log.e(TAG, "startCamera: ExecutionException - " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Camera initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (InterruptedException e) {
                Log.e(TAG, "startCamera: InterruptedException - " + e.getMessage(), e);
                Thread.currentThread().interrupt();
                Toast.makeText(requireContext(), "Camera initialization interrupted: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "startCamera: Generic error during camera provider setup: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "bindCameraUseCases: Binding camera use cases (Preview, ImageCapture, VideoCapture).");
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        // Store the Recorder instance directly
        videoRecorder = new Recorder.Builder()
                .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HIGHEST))
                .build();
        videoCaptureUseCase = VideoCapture.withOutput(videoRecorder); // Use the stored recorder

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCaptureUseCase); // Bind the use case
            Log.d(TAG, "bindCameraUseCases: All CameraX use cases bound to lifecycle.");
        } catch (Exception exc) {
            Log.e(TAG, "bindCameraUseCases: Use case binding failed: " + exc.getMessage(), exc);
            Toast.makeText(requireContext(), "Camera binding failed: " + exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is null, cannot take photo.");
            Toast.makeText(requireContext(), "Camera not ready for photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Capturing " + currentMediaType + " photo...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "takePhoto: Initiating photo capture.");

        String name = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Walkfie");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                requireContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        if (savedUri != null) {
                            Log.d(TAG, "Photo capture succeeded: " + savedUri.toString());
                            Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show();

                            // Determine media type (it's always "image" here)
                            String mediaType = "image"; // Or use "image/jpeg" if you want MIME type

                            if ("Story".equals(currentMediaType) && callback != null) {
                                callback.onMediaCapturedForStory(savedUri, mediaType); // <-- Pass mediaType
                            } else if ("Post".equals(currentMediaType) && callback != null) {
                                callback.onMediaCapturedForPost(savedUri, mediaType); // <-- Pass mediaType
                            }
                        } else {
                            Log.e(TAG, "Photo capture failed: Saved URI is null.");
                            Toast.makeText(requireContext(), "Photo capture failed: No URI.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                        Toast.makeText(requireContext(), "Photo capture failed: " + exc.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void startVideoRecording() {
        if (videoRecorder == null) {
            Log.e(TAG, "VideoRecorder is null, cannot start video recording.");
            Toast.makeText(requireContext(), "Camera not ready for video.", Toast.LENGTH_SHORT).show();
            return;
        }

        // FIX: Use requireContext() for permission check in fragment
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "RECORD_AUDIO permission is required for video recording.", Toast.LENGTH_LONG).show();
            // You might want to request permission here again or prompt the user.
            // For now, we'll just return.
            return;
        }

        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }

        Toast.makeText(requireContext(), "Starting " + currentMediaType + " video recording...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "startVideoRecording: Initiating video recording.");

        String name = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Walkfie");
        }

        // FIX: The MediaStoreOutputOptions.Builder constructor is correct
        MediaStoreOutputOptions outputOptions = new MediaStoreOutputOptions.Builder(
                requireContext().getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        // FIX: Call prepareRecording on videoRecorder
        // FIX: Use explicit Consumer implementation with correct type
        currentRecording = videoRecorder.prepareRecording(requireContext(), outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext()), new Consumer<VideoRecordEvent>() {
                    @Override
                    public void accept(VideoRecordEvent videoRecordEvent) {
                        if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                            Log.d(TAG, "Video recording started.");
                            btnCapture.setBackgroundResource(R.drawable.capture_button_recording_background);
                        } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                            if (!finalizeEvent.hasError()) {
                                Uri savedUri = finalizeEvent.getOutputResults().getOutputUri();
                                Log.d(TAG, "Video recording succeeded: " + savedUri.toString());
                                Toast.makeText(requireContext(), "Video saved!", Toast.LENGTH_SHORT).show();

                                // Determine media type (it's always "video" here)
                                String mediaType = "video"; // Or use "video/mp4"

                                if ("Story".equals(currentMediaType) && callback != null) {
                                    callback.onMediaCapturedForStory(savedUri, mediaType); // <-- Pass mediaType
                                } else if ("Post".equals(currentMediaType) && callback != null) {
                                    callback.onMediaCapturedForPost(savedUri, mediaType); // <-- Pass mediaType
                                }
                            } else {
                                Log.e(TAG, "Video recording failed: " + finalizeEvent.getError() + ", " + finalizeEvent.getCause());
                                Toast.makeText(requireContext(), "Video recording failed: " + finalizeEvent.getError(), Toast.LENGTH_LONG).show();
                                if (finalizeEvent.getOutputResults().getOutputUri() != null) {
                                    // Attempt to delete partial file if error occurred
                                    try {
                                        requireContext().getContentResolver().delete(finalizeEvent.getOutputResults().getOutputUri(), null, null);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to delete partial video file: " + e.getMessage());
                                    }
                                }
                            }
                            btnCapture.setBackgroundResource(R.drawable.capture_button_background);
                            currentRecording = null;
                        } else if (videoRecordEvent instanceof VideoRecordEvent.Status) {
                            RecordingStats stats = ((VideoRecordEvent.Status) videoRecordEvent).getRecordingStats();
                            Log.d(TAG, "Video recording status: Bytes: " + stats.getNumBytesRecorded() + ", Duration: " + stats.getRecordedDurationNanos() / 1_000_000_000 + "s");
                        }
                    }
                });

        new Handler(Looper.getMainLooper()).postDelayed(this::stopVideoRecording, 30000);
    }

    private void stopVideoRecording() {
        if (currentRecording != null) {
            Log.d(TAG, "stopVideoRecording: Stopping current recording.");
            currentRecording.stop();
        } else {
            Log.d(TAG, "stopVideoRecording: No active recording to stop.");
        }
    }

    // Modify pickMediaFromGallery
    private void pickMediaFromGallery() {
        Log.d(TAG, "pickMediaFromGallery: Launching Photo Picker.");
        // Use PickVisualMediaRequest
        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE) // Request both images and videos
                .build());
    }

    // --- LOCATION-RELATED METHODS ---
    private void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: Attempting to start location updates.");
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    lastKnownLocation = locationResult.getLastLocation();
                    Log.d(TAG, "Location received: Lat=" + lastKnownLocation.getLatitude() + ", Lng=" + lastKnownLocation.getLongitude() + ", Accuracy=" + lastKnownLocation.getAccuracy());
                    getAddressFromLocation(lastKnownLocation);
                } else {
                    Log.w(TAG, "onLocationResult: LocationResult or LastLocation is null.");
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission granted. Requesting location updates.");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.w(TAG, "Location permission NOT granted during startLocationUpdates. Will request via onRequestPermissionsResult.");
        }
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (address.getLocality() != null && address.getAdminArea() != null) {
                    sb.append(address.getLocality()).append(", ").append(address.getAdminArea());
                } else if (address.getAddressLine(0) != null) {
                    sb.append(address.getAddressLine(0));
                }
                if (sb.length() == 0 && address.getFeatureName() != null) {
                    sb.append(address.getFeatureName());
                }
                String fullAddress = sb.toString().trim();
                tvLocation.setText(fullAddress);
                Log.d(TAG, "Geocoder: Fetched address: " + fullAddress +
                        " (Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude() + ")");
            } else {
                tvLocation.setText("Unknown Location");
                Log.w(TAG, "Geocoder: No addresses found for Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage(), e);
            tvLocation.setText("Location Unavailable");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Geocoder IllegalArgumentException: " + e.getMessage() +
                    " (Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude() + ")");
            tvLocation.setText("Location Unavailable");
        }
    }

    // --- PERMISSION AND LIFECYCLE METHODS ---
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "onRequestPermissionsResult: All permissions granted by user.");
                startCamera();
                startLocationUpdates();
            } else {
                Log.w(TAG, "onRequestPermissionsResult: Permissions NOT granted by user.");
                Toast.makeText(requireContext(), "Permissions not granted by the user. Camera, microphone, and location features disabled.", Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onPhotoFragmentCancelled();
                }
            }
        }
    }

    private boolean allPermissionsGranted() {
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission NOT granted: " + permission);
                allGranted = false;
            } else {
                Log.d(TAG, "Permission granted: " + permission);
            }
        }
        return allGranted;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Removing location updates.");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (currentRecording != null) {
            stopVideoRecording();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed. Checking if location updates need to be restarted.");
        if (allPermissionsGranted() && fusedLocationClient != null && locationRequest != null) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onResume: Location permission still granted. Requesting location updates.");
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
        }
        if (viewFinder != null && viewFinder.isAttachedToWindow() && allPermissionsGranted()) {
            startCamera();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        Log.d(TAG, "onDestroyView: Camera executor shut down.");
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
        if (pickMediaLauncher != null) {
            pickMediaLauncher = null;
        }
    }
}