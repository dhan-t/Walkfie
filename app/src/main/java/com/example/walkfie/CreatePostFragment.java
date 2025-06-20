package com.example.walkfie;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.Timestamp; // Will use this if we don't rely on @ServerTimestamp
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Removed java.util.Date import if no longer needed
import java.util.HashMap;
import java.util.Map;

public class CreatePostFragment extends Fragment {

    private static final String TAG = "CreatePostFragment";
    private static final String ARG_MEDIA_URI = "media_uri";
    private static final String ARG_MEDIA_TYPE = "media_type";

    private Uri mediaUri;
    private String mediaType; // "image" or "video"

    // UI Elements
    private ImageView ivMediaPreview;
    private EditText etCaption;
    private Button btnPost;
    private ImageView ivCancelPost;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Callback interface
    public interface CreatePostCallback {
        void onPostCreatedSuccessfully();
        void onPostCreationCancelled();
    }

    private CreatePostCallback callback;

    public CreatePostFragment() {
        // Required empty public constructor
    }

    public static CreatePostFragment newInstance(Uri mediaUri, String mediaType) {
        CreatePostFragment fragment = new CreatePostFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MEDIA_URI, mediaUri);
        args.putString(ARG_MEDIA_TYPE, mediaType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof CreatePostCallback) {
            callback = (CreatePostCallback) context;
        } else {
            Log.w(TAG, "Host Activity does not implement CreatePostCallback.");
            // Consider throwing a RuntimeException here if the callback is strictly mandatory
            // throw new RuntimeException(context.toString() + " must implement CreatePostCallback");
        }
    }

    // NEW: Add this setter method
    public void setCreatePostCallback(CreatePostCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mediaUri = getArguments().getParcelable(ARG_MEDIA_URI);
            mediaType = getArguments().getString(ARG_MEDIA_TYPE);
            Log.d(TAG, "onCreate: Received mediaUri = " + mediaUri + ", mediaType = " + mediaType);
        } else {
            Log.e(TAG, "onCreate: No arguments provided for CreatePostFragment. Media will be null.");
        }

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post, container, false);

        // Bind UI elements
        ivMediaPreview = view.findViewById(R.id.ivMediaPreview);
        etCaption = view.findViewById(R.id.etCaption);
        btnPost = view.findViewById(R.id.btnPost);
        ivCancelPost = view.findViewById(R.id.ivCancelPost);
        progressBar = view.findViewById(R.id.progressBar);

        // Display media preview
        if (mediaUri != null) {
            Glide.with(this)
                    .load(mediaUri)
                    .into(ivMediaPreview);
        } else {
            Log.e(TAG, "Media URI is null, cannot display preview.");
            Toast.makeText(getContext(), "Error: No media to post.", Toast.LENGTH_SHORT).show();
            // Optionally, disable post button or dismiss fragment
            btnPost.setEnabled(false);
        }

        // Set up listeners
        ivCancelPost.setOnClickListener(v -> {
            if (callback != null) {
                callback.onPostCreationCancelled();
            } else {
                getParentFragmentManager().popBackStack(); // Fallback if callback not set
            }
        });

        etCaption.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Enable post button if caption is not empty and media is present
                btnPost.setEnabled(s.toString().trim().length() > 0 && mediaUri != null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPost.setOnClickListener(v -> uploadPost());

        return view;
    }

    private void uploadPost() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You need to be logged in to post.", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onPostCreationCancelled(); // Redirect to login or handle
            return;
        }

        String userId = currentUser.getUid();
        String caption = etCaption.getText().toString().trim();

        if (caption.isEmpty()) {
            Toast.makeText(getContext(), "Please add a caption.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mediaUri == null) {
            Toast.makeText(getContext(), "No media selected to post.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true); // Show progress bar, disable UI

        // Step 1: Get user details from Firestore
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String userProfilePicUrl = documentSnapshot.getString("profilePicUrl");

                        if (username == null || username.isEmpty()) {
                            username = "Unknown User"; // Fallback
                            Log.w(TAG, "Username not found for user: " + userId);
                        }
                        // Profile pic might be null, which is fine

                        // Step 2: Upload media to Firebase Storage
                        uploadMediaToStorage(userId, username, userProfilePicUrl, caption);

                    } else {
                        Log.e(TAG, "User document not found for ID: " + userId);
                        Toast.makeText(getContext(), "Error: User data not found.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data: " + e.getMessage());
                    Toast.makeText(getContext(), "Error fetching user data.", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }

    private void uploadMediaToStorage(String userId, String username, @Nullable String userProfilePicUrl, String caption) {
        StorageReference mediaRef = storage.getReference()
                .child("posts")
                .child(userId)
                .child(System.currentTimeMillis() + "_" + mediaUri.getLastPathSegment());

        mediaRef.putFile(mediaUri)
                .addOnSuccessListener(taskSnapshot -> {
                    mediaRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String mediaUrl = downloadUri.toString();
                        Log.d(TAG, "Media uploaded successfully. Download URL: " + mediaUrl);
                        // Step 3: Save post data to Firestore
                        savePostToFirestore(userId, username, userProfilePicUrl, mediaUrl, caption);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL: " + e.getMessage());
                        Toast.makeText(getContext(), "Error getting media URL.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    });
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "Upload is " + progress + "% done");
                    // You could update a progress indicator here if desired
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Media upload failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Media upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
    }

    private void savePostToFirestore(String userId, String username, @Nullable String userProfilePicUrl, String mediaUrl, String caption) {
        // Create a new Post object (using your Post.java model)
        // IMPORTANT: The Post class should now expect 'Timestamp' for its timestamp field,
        // and we'll rely on @ServerTimestamp for new posts or manually create a Timestamp.
        // Given your Post.java, the simpler constructor is likely:
        Post newPost = new Post(
                userId,
                username,
                userProfilePicUrl,
                mediaUrl,
                caption
                // No timestamp here, as @ServerTimestamp will handle it when written to Firestore
                // No likesCount or commentsCount here, as they are initialized to 0 in Post.java's default constructor
        );

        db.collection("posts")
                .add(newPost) // Add a new document with an auto-generated ID
                .addOnSuccessListener(documentReference -> {
                    // This `newPost.setId` is purely for local consistency if you were to use
                    // this `newPost` object immediately after this callback.
                    // Firestore will assign the ID anyway.
                    newPost.setId(documentReference.getId());
                    Log.d(TAG, "Post added to Firestore with ID: " + documentReference.getId());
                    Toast.makeText(getContext(), "Post created successfully!", Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onPostCreatedSuccessfully();
                    }
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding post to Firestore: " + e.getMessage());
                    Toast.makeText(getContext(), "Error creating post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnPost.setEnabled(false); // Disable post button
            ivCancelPost.setEnabled(false); // Disable cancel
            etCaption.setEnabled(false); // Disable caption input
            // Optionally, also disable touch on ivMediaPreview
        } else {
            progressBar.setVisibility(View.GONE);
            // Re-enable button based on caption length
            btnPost.setEnabled(etCaption.getText().toString().trim().length() > 0 && mediaUri != null);
            ivCancelPost.setEnabled(true);
            etCaption.setEnabled(true);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null; // Avoid memory leaks
    }
}