package com.example.walkfie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

import java.util.Objects;

public class GoogleSignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db; // Use Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_google_sign_in);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Secrets.getGoogleWebClientId(this)) // <-- IMPORTANT
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.startGoogleSignIn).setOnClickListener(v -> signIn());

        // Animation
        animateEntrance(findViewById(R.id.startGoogleSignIn));

        signIn(); // Auto-start sign-in when activity starts
    }

    private void animateEntrance(View view) {
        view.setAlpha(0f);
        view.setTranslationY(100f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();
    }

    private void signIn() {
        // 1. Sign out first to clear any cached account from previous sessions if needed
        // This is a good practice if you want to force account selection every time.
        // If you want seamless sign-in for returning users, you might skip signOut().
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // 2. After sign out (or immediately if skipping signOut), start the sign-in intent
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            var task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish(); // Finish activity on failure
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Save/update user profile in Firestore after successful Firebase Auth
                            saveOrUpdateUserProfileInFirestore(firebaseUser, acct);
                        }
                        startActivity(new Intent(GoogleSignInActivity.this, WelcomeActivity.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        finish(); // Finish activity on failure
                    }
                });
    }

    // Renamed from saveUserToDatabase and updated logic
    private void saveOrUpdateUserProfileInFirestore(FirebaseUser firebaseUser, GoogleSignInAccount acct) {
        String uid = firebaseUser.getUid();
        String firstName = acct.getGivenName(); // Use getGivenName() for first name
        String lastName = acct.getFamilyName(); // Use getFamilyName() for last name

        // Handle cases where these might be null
        if (firstName == null) {
            firstName = ""; // Or provide a default, or try to parse from displayName
        }
        if (lastName == null) {
            lastName = ""; // Or provide a default
        }

        // Username can be derived from Google's display name or given name as a fallback
        String username = acct.getDisplayName();
        if (username == null || username.isEmpty()) {
            username = acct.getGivenName() != null ? acct.getGivenName() : "";
        }

        String email = acct.getEmail() != null ? acct.getEmail() : "";
        String profilePicUrl = acct.getPhotoUrl() != null ? acct.getPhotoUrl().toString() : "";

        // Attempt to get existing user data, or create new if not exists
        String finalFirstName = firstName;
        String finalLastName = lastName;
        String finalUsername = username;
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User profile already exists, just update if needed (e.g., photo URL might change)
                        // For simplicity, we just log that it exists. Update logic can be more complex.
                        // You could update specific fields:
                        // db.collection("users").document(uid).update("profilePicUrl", profilePicUrl);
                        Toast.makeText(GoogleSignInActivity.this, "User profile already exists in Firestore!", Toast.LENGTH_SHORT).show();
                    } else {
                        // User profile does not exist, create it
                        User newUser = new User(
                                uid,
                                finalFirstName,
                                finalLastName,
                                finalUsername,
                                "", // Empty bio initially
                                profilePicUrl,
                                email
                        );
                        db.collection("users").document(uid).set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(GoogleSignInActivity.this, "User profile created in Firestore!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(GoogleSignInActivity.this, "Error creating user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GoogleSignInActivity.this, "Error checking user profile in Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}