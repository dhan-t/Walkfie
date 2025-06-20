package com.example.walkfie;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class FacebookSignInActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_facebook_sign_in);

        firebaseAuth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();

        findViewById(R.id.startFacebookSignIn).setOnClickListener(v -> signIn());

        animateEntrance(findViewById(R.id.startFacebookSignIn));

        signIn();
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
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(FacebookSignInActivity.this, "Facebook Login Cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Toast.makeText(FacebookSignInActivity.this, "Facebook Login Failed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        if (token == null) {
            Toast.makeText(this, "Facebook token is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToDatabase();
                        startActivity(new Intent(FacebookSignInActivity.this, HomeActivity.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void saveUserToDatabase() {
        FirebaseUser user = firebaseAuth.getCurrentUser(); // Use FirebaseUser, not var
        if (user != null) {
            String uid = user.getUid();
            String username = user.getDisplayName() != null ? user.getDisplayName() : "New User"; // Handle null display name
            String firstName = user.getDisplayName() != null ? user.getDisplayName() : ""; // Handle null first name
            String lastName = user.getDisplayName() != null ? user.getDisplayName() : ""; // Handle null last name
            String email = user.getEmail() != null ? user.getEmail() : ""; // Handle null email
            String profilePicUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""; // Get photo URL from FirebaseUser

            // Use the full 5-argument constructor
            User newUser = new User(
                    uid,
                    firstName,
                    lastName,
                    username,
                    "", // Empty bio initially
                    profilePicUrl,
                    email
            );

            // IMPORTANT: Use FirebaseFirestore, not FirebaseDatabase.getInstance().getReference("Users")
            // Your Firestore setup expects db.collection("users").document(uid).set(userObject)
            Object profile = new Object();
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(profile)
                    .addOnSuccessListener(aVoid -> {
                        // Use FacebookSignInActivity.this or this as the Context
                        Toast.makeText(FacebookSignInActivity.this, "User profile saved to Firestore!", Toast.LENGTH_SHORT).show();
                        // You might want to navigate to Home/Profile Fragment here
                    })
                    .addOnFailureListener(e -> {
                        // Use FacebookSignInActivity.this or this as the Context
                        Toast.makeText(FacebookSignInActivity.this, "Error saving user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
