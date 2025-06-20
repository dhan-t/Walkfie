package com.example.walkfie;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

// The interface definition here is correct and doesn't need changes.
// interface EditProfileCallback { } // This empty one is overridden below.

interface EditProfileCallback {
    void onProfileSaved(); // Notify HomeActivity that profile was saved
    void onEditProfileCancelled(); // Notify HomeActivity to go back
}

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";
    private static final String ARG_USER_UID = "userUid";

    // UI elements
    private ImageView ivBackArrowEdit;
    private ImageView ivEditProfilePicture;
    private ImageView ivAddPhoto;
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etBio;
    private TextView tvEmail;
    private Button btnSave;
    private Button btnRemoveProfilePicture;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private String currentProfilePicUrl; // To track existing URL for removal check

    private EditProfileCallback callback;

    // For image picking
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    public EditProfileFragment() {
        // Required empty public constructor
    }

    // Factory method to create an instance with arguments (e.g., user UID)
    public static EditProfileFragment newInstance(String userUid) {
        EditProfileFragment fragment = new EditProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID, userUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof EditProfileCallback) {
            callback = (EditProfileCallback) context;
        } else {
            // Log a warning if the parent activity doesn't implement the callback,
            // but still allow default back press behavior.
            Log.w(TAG, "Parent activity does not implement EditProfileCallback.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize ActivityResultLauncher for image picking
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).into(ivEditProfilePicture);
                            btnRemoveProfilePicture.setVisibility(View.VISIBLE); // Show remove button
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize UI elements
        ivBackArrowEdit = view.findViewById(R.id.ivBackArrowEdit);
        ivEditProfilePicture = view.findViewById(R.id.ivEditProfilePicture);
        ivAddPhoto = view.findViewById(R.id.ivAddPhoto);
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etBio = view.findViewById(R.id.etBio);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnSave = view.findViewById(R.id.btnSave);
        btnRemoveProfilePicture = view.findViewById(R.id.btnRemoveProfilePicture);

        // Load current user data
        loadUserProfile();

        // Set listeners
        ivBackArrowEdit.setOnClickListener(v -> {
            if (callback != null) {
                callback.onEditProfileCancelled();
            } else if (getActivity() != null) {
                getActivity().onBackPressed(); // Go back to previous fragment (ProfileFragment)
            }
        });

        ivAddPhoto.setOnClickListener(v -> pickImage());
        btnRemoveProfilePicture.setOnClickListener(v -> removeProfilePicture());
        btnSave.setOnClickListener(v -> saveProfile());

        return view;
    }

    private void loadUserProfile() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && getContext() != null) { // Ensure fragment is still active
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // Populate fields with existing data
                                etFirstName.setText(user.getFirstName());
                                etLastName.setText(user.getLastName());
                                etBio.setText(user.getBio());
                                tvEmail.setText(user.getEmail());

                                currentProfilePicUrl = user.getProfilePicUrl();
                                if (currentProfilePicUrl != null && !currentProfilePicUrl.isEmpty()) {
                                    Glide.with(this).load(currentProfilePicUrl).into(ivEditProfilePicture);
                                    btnRemoveProfilePicture.setVisibility(View.VISIBLE);
                                } else {
                                    ivEditProfilePicture.setImageResource(R.drawable.ic_default_profile_placeholder);
                                    btnRemoveProfilePicture.setVisibility(View.GONE);
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "User profile not found. Please log in again.", Toast.LENGTH_LONG).show();
                            // Optionally, navigate back to login or registration if profile is truly missing
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) { // Ensure fragment is still active
                        Log.e(TAG, "Error loading user profile: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void removeProfilePicture() {
        selectedImageUri = null; // Clear selected URI
        ivEditProfilePicture.setImageResource(R.drawable.ic_default_profile_placeholder);
        btnRemoveProfilePicture.setVisibility(View.GONE);
        currentProfilePicUrl = null; // Mark for removal in Firestore/Storage during save
        Toast.makeText(getContext(), "Profile picture removed. Tap Save to apply changes.", Toast.LENGTH_SHORT).show();
    }


    private void saveProfile() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String newFirstName = etFirstName.getText().toString().trim();
        String newLastName = etLastName.getText().toString().trim();
        String newBio = etBio.getText().toString().trim();
        String newUsername = newFirstName + " " + newLastName;

        // Disable save button to prevent multiple clicks
        btnSave.setEnabled(false);
        // TODO: Optionally, show a ProgressBar or other loading indicator here
        // if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Process profile update based on picture changes
        if (selectedImageUri != null) { // New image selected
            uploadProfilePicture(newFirstName, newLastName, newBio, newUsername);
        } else if (currentProfilePicUrl != null && selectedImageUri == null) { // Existing pic removed
            deleteProfilePictureFromStorage(newFirstName, newLastName, newBio, newUsername, ""); // Pass empty URL
        } else { // No change to picture, or no pic existed and none selected
            updateFirestoreProfile(newFirstName, newLastName, newBio, newUsername, currentProfilePicUrl);
        }
    }

    private void uploadProfilePicture(String firstName, String lastName, String bio, String username) {
        // If selectedImageUri is null here, it's an error in logic flow, should not happen.
        // But for safety, handle it.
        if (selectedImageUri == null) {
            updateFirestoreProfile(firstName, lastName, bio, username, currentProfilePicUrl);
            return;
        }

        // Delete old picture if exists and is different from new one (optional cleanup)
        if (currentProfilePicUrl != null && !currentProfilePicUrl.isEmpty()) {
            try {
                StorageReference oldPhotoRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentProfilePicUrl);
                oldPhotoRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Old profile picture deleted successfully from storage."))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete old profile picture from storage: " + e.getMessage()));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid URL for old profile picture, skipping deletion: " + currentProfilePicUrl);
            }
        }

        StorageReference profilePicRef = storage.getReference("profile_pictures/" + currentUser.getUid() + ".jpg");
        profilePicRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            updateFirestoreProfile(firstName, lastName, bio, username, downloadUrl);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Profile picture upload failed", e);
                    processCompletion(false); // Indicate failure, stay on page.
                });
    }

    private void deleteProfilePictureFromStorage(String firstName, String lastName, String bio, String username, String newProfilePicUrl) {
        if (currentProfilePicUrl == null || currentProfilePicUrl.isEmpty()) {
            updateFirestoreProfile(firstName, lastName, bio, username, newProfilePicUrl); // No picture to delete
            return;
        }

        try {
            StorageReference oldPhotoRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentProfilePicUrl);
            oldPhotoRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Old profile picture deleted successfully from storage.");
                        updateFirestoreProfile(firstName, lastName, bio, username, newProfilePicUrl); // Now update Firestore with empty URL
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete old profile picture from storage: " + e.getMessage());
                        // Even if deletion fails, try to update Firestore with the new (empty) URL
                        updateFirestoreProfile(firstName, lastName, bio, username, newProfilePicUrl);
                    });
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Invalid URL for old profile picture, skipping deletion: " + currentProfilePicUrl);
            updateFirestoreProfile(firstName, lastName, bio, username, newProfilePicUrl);
        }
    }


    private void updateFirestoreProfile(String firstName, String lastName, String bio, String username, String profilePicUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("username", username); // Update combined username too
        updates.put("bio", bio);
        updates.put("profilePicUrl", profilePicUrl); // Can be empty string or new URL

        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also update Firebase Auth profile (displayName and photoUrl)
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username) // Set display name in Firebase Auth
                            .setPhotoUri(profilePicUrl != null && !profilePicUrl.isEmpty() ? Uri.parse(profilePicUrl) : null)
                            .build();

                    currentUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                if (isAdded() && getContext() != null) { // Ensure fragment is still active
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Firebase Auth profile updated.");
                                        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                        processCompletion(true); // Indicate success, navigate back.
                                    } else {
                                        Log.e(TAG, "Failed to update Firebase Auth profile.", task.getException());
                                        Toast.makeText(getContext(), "Failed to update Firebase Auth profile: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        processCompletion(false); // Indicate failure, stay on page.
                                    }
                                } else {
                                    Log.w(TAG, "Fragment not attached when Firebase Auth profile update completed. UI update/Toast/Navigation skipped.");
                                    // If fragment is detached, the process is effectively complete for it,
                                    // but we can't update UI or navigate.
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore update failed", e);
                    processCompletion(false); // Indicate failure, stay on page.
                });
    }

    // Unified method to handle UI completion (re-enable button, hide loading) and navigation
    private void processCompletion(boolean success) {
        if (isAdded() && getContext() != null) {
            btnSave.setEnabled(true);
            // TODO: Optionally, hide a ProgressBar or other loading indicator here
            // if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (success) {
                if (callback != null) {
                    callback.onProfileSaved();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
            // If not successful, we intentionally stay on the page to show error toast
            // and allow the user to retry.
        } else {
            Log.w(TAG, "Fragment not attached when processCompletion called. UI updates skipped.");
        }
    }

    // This is the method you need to add to fix the error!
    public void setEditProfileCallback(EditProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null; // Clear callback to prevent leaks
    }
}