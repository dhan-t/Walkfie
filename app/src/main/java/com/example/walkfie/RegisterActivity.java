package com.example.walkfie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest; // Import for updating display name
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    EditText firstName;
    EditText lastName;
    EditText email;
    EditText password;
    Button registerButton;
    ImageView googleButton, facebookButton;
    TextView orText;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Use Firestore instead of Realtime Database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        registerButton = findViewById(R.id.registerButton);
        googleButton = findViewById(R.id.googleButton);
        facebookButton = findViewById(R.id.facebookButton);
        orText = findViewById(R.id.orText);

        animateStaggered(firstName, 0);
        animateStaggered(lastName, 100);
        animateStaggered(email, 200);
        animateStaggered(password, 300);
        animateStaggered(registerButton, 400);
        animateStaggered(orText, 500);
        animateStaggered(googleButton, 600);
        animateStaggered(facebookButton, 700);

        registerButton.setOnClickListener(v -> {
            String emailInput = email.getText().toString().trim();
            String passwordInput = password.getText().toString().trim();
            String firstNameInput = firstName.getText().toString().trim();
            String lastNameInput = lastName.getText().toString().trim();
            String username = firstNameInput + " " + lastNameInput; // Combine for full name

            if (emailInput.isEmpty() || passwordInput.isEmpty() || firstNameInput.isEmpty() || lastNameInput.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(emailInput, passwordInput)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // 1. Update FirebaseUser's display name
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        // .setPhotoUri(Uri.parse("url_to_default_profile_pic")) // Optional: Set a default photo URL
                                        .build();
                                firebaseUser.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                // 2. Save user profile to Firestore
                                                saveUserProfileToFirestore(firebaseUser, firstNameInput, lastNameInput, username, emailInput);
                                            } else {
                                                Toast.makeText(this, "Failed to update profile: " + Objects.requireNonNull(profileTask.getException()).getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                            // Navigate to LoginActivity (or directly to WelcomeActivity if user is auto-logged in)
                            // It's common to go straight to WelcomeActivity after registration if auto-login occurs
                            startActivity(new Intent(RegisterActivity.this, WelcomeActivity.class));
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            finish();
                        } else {
                            Toast.makeText(this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        googleButton.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, GoogleSignInActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        facebookButton.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, FacebookSignInActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void saveUserProfileToFirestore(FirebaseUser firebaseUser, String firstName, String lastName, String username, String email) {
        String uid = firebaseUser.getUid();
        // Initialize User object with all required fields
        User newUser = new User(
                uid,
                firstName,
                lastName,
                username,
                "", // Empty bio initially
                "",
                email
        );

        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "User profile created in Firestore!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error creating user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void animateStaggered(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(100f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setInterpolator(new OvershootInterpolator())
                .setStartDelay(delay)
                .setDuration(500)
                .start();
    }
}