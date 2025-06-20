package com.example.walkfie;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp; // Use Firebase's Timestamp
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue; // For server timestamp
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date; // For java.util.Date if needed for Timestamp constructor
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Define a callback interface for AddStoryFragment actions
interface AddStoryCallback {
    void onStoryPostedSuccessfully();
    void onStoryCreationCancelled();
}

public class AddStoryFragment extends Fragment {

    private static final String TAG = "AddStoryFragment";
    private static final String ARG_MEDIA_URI = "media_uri"; // Changed to media_uri
    private static final String ARG_MEDIA_TYPE = "media_type"; // New argument for media type

    private ImageView ivStoryContent;
    private EditText etStoryText;
    private Button btnPostStory;
    private ImageView ivCloseStory;
    private ProgressBar progressBar;

    private Uri mediaUri; // Changed name from storyImageUri to mediaUri
    private String mediaType; // To store "image" or "video"
    private AddStoryCallback callback;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    public AddStoryFragment() {
        // Required empty public constructor
    }

    // !!! IMPORTANT: Updated newInstance method to accept mediaType !!!
    public static AddStoryFragment newInstance(Uri mediaUri, String mediaType) {
        AddStoryFragment fragment = new AddStoryFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MEDIA_URI, mediaUri);
        args.putString(ARG_MEDIA_TYPE, mediaType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AddStoryCallback) {
            callback = (AddStoryCallback) context;
        }
    }

    // Method to be called by the hosting activity to set the callback (if not set via onAttach)
    public void setAddStoryCallback(AddStoryCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("stories_media"); // More general path

        if (getArguments() != null) {
            mediaUri = getArguments().getParcelable(ARG_MEDIA_URI);
            mediaType = getArguments().getString(ARG_MEDIA_TYPE);

            if (mediaUri == null) {
                Log.e(TAG, "Media URI is null in arguments!");
                if (callback != null) {
                    callback.onStoryCreationCancelled();
                } else {
                    // Fallback if no callback set
                    Toast.makeText(requireContext(), "Error: No media to display.", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                }
            }
        } else {
            Log.e(TAG, "No arguments provided to AddStoryFragment!");
            if (callback != null) {
                callback.onStoryCreationCancelled();
            } else {
                Toast.makeText(requireContext(), "Error: Fragment launched without media.", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_story, container, false);

        ivStoryContent = view.findViewById(R.id.ivStoryContent);
        etStoryText = view.findViewById(R.id.etStoryText);
        btnPostStory = view.findViewById(R.id.btnPostStory);
        ivCloseStory = view.findViewById(R.id.ivCloseStory);
        progressBar = view.findViewById(R.id.progressBar);

        // Load the media into the ImageView/VideoView (assuming ImageView for now, but handle video later)
        if (mediaUri != null) {
            // Glide can handle both images and display a thumbnail for videos
            Glide.with(this)
                    .load(mediaUri)
                    .placeholder(R.drawable.sample_story_placeholder)
                    .error(R.drawable.sample_story_placeholder)
                    .into(ivStoryContent);
        } else {
            Log.w(TAG, "mediaUri is null, cannot load media.");
            ivStoryContent.setImageResource(R.drawable.sample_story_placeholder);
        }

        // Set Click Listeners
        ivCloseStory.setOnClickListener(v -> {
            if (callback != null) {
                callback.onStoryCreationCancelled();
            } else {
                Toast.makeText(requireContext(), "Story creation cancelled.", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });

        btnPostStory.setOnClickListener(v -> postStory());

        return view;
    }


    private void postStory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You need to be logged in to post a story.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaUri == null) {
            Toast.makeText(requireContext(), "No media selected for story.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String userId = currentUser.getUid();
        // It's generally better to fetch username/profilePic from Firestore once
        // as Auth profile might not always be updated or complete.
        // This pattern fetches the user data first, then proceeds.
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            String username = user.getUsername() != null ? user.getUsername() : "Anonymous";
                            String profilePicUrl = user.getProfilePicUrl() != null ? user.getProfilePicUrl() : "";
                            uploadMediaAndPostStory(userId, username, profilePicUrl);
                        } else {
                            Log.e(TAG, "User object is null after conversion.");
                            uploadMediaAndPostStory(userId, "Anonymous", ""); // Fallback
                        }
                    } else {
                        Log.e(TAG, "User document does not exist for " + userId);
                        uploadMediaAndPostStory(userId, "Anonymous", ""); // Fallback
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user details for story: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error getting user details. Try again.", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }

    private void uploadMediaAndPostStory(String userId, String username, String userProfilePicUrl) {
        String fileExtension = getFileExtension(mediaUri, mediaType);
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        StorageReference mediaRef = storageRef.child(userId + "/" + fileName);

        mediaRef.putFile(mediaUri)
                .addOnSuccessListener(taskSnapshot -> mediaRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    String storyText = etStoryText.getText().toString().trim();

                    Map<String, Object> story = new HashMap<>();
                    story.put("userId", userId);
                    story.put("username", username);
                    story.put("userProfilePicUrl", userProfilePicUrl);
                    story.put("mediaUrl", downloadUrl); // Changed from imageUrl to mediaUrl
                    story.put("mediaType", mediaType); // Store the media type
                    story.put("text", storyText);
                    story.put("timestamp", new Timestamp(new Date())); // Use server timestamp

                    db.collection("stories").add(story)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Story posted successfully with ID: " + documentReference.getId());
                                Log.d(TAG, "Story data uploaded: " + story.toString());
                                Toast.makeText(requireContext(), "Story posted!", Toast.LENGTH_SHORT).show();
                                setLoading(false);
                                if (callback != null) {
                                    callback.onStoryPostedSuccessfully();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding story to Firestore", e);
                                Toast.makeText(requireContext(), "Failed to post story: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                setLoading(false);
                            });
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Media upload failed for story", e);
                    Toast.makeText(requireContext(), "Failed to upload media: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
    }

    private String getFileExtension(Uri uri, String mediaType) {
        if (uri == null) return "unknown"; // Fallback

        // Try to get extension from URI first
        String path = uri.getPath();
        if (path != null) {
            int dot = path.lastIndexOf('.');
            if (dot >= 0) {
                return path.substring(dot + 1);
            }
        }

        // Fallback to mediaType if URI path doesn't have an extension
        if ("image".equals(mediaType)) {
            return "jpg"; // Default for images
        } else if ("video".equals(mediaType)) {
            return "mp4"; // Default for videos
        }
        return "bin"; // Generic binary if type is unknown
    }


    private void setLoading(boolean isLoading) {
        if (!isAdded()) return;

        // Use isVisible to ensure UI updates only if fragment is currently visible
        // You can also check if (getView() != null)
        if (getView() == null) {
            return;
        }

        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnPostStory.setEnabled(false);
            ivCloseStory.setEnabled(false);
            etStoryText.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnPostStory.setEnabled(true);
            ivCloseStory.setEnabled(true);
            etStoryText.setEnabled(true);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}