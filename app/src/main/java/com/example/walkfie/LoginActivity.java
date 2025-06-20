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
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginButton;
    ImageView googleButton, facebookButton;
    TextView orText;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Use Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        googleButton = findViewById(R.id.googleButton);
        facebookButton = findViewById(R.id.facebookButton);
        orText = findViewById(R.id.orText);

        animateStaggered(emailInput, 0);
        animateStaggered(passwordInput, 100);
        animateStaggered(loginButton, 200);
        animateStaggered(orText, 500);
        animateStaggered(googleButton, 600);
        animateStaggered(facebookButton, 700);

        loginButton.setOnClickListener(v -> {
            String inputEmail = emailInput.getText().toString().trim();
            String inputPassword = passwordInput.getText().toString().trim();

            if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(inputEmail, inputPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Check if user profile exists in Firestore, if not, create it
                                ensureUserProfileExistsInFirestore(firebaseUser);
                            }
                            startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            finish();
                        } else {
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        googleButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, GoogleSignInActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        facebookButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, FacebookSignInActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void ensureUserProfileExistsInFirestore(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // User profile does not exist, create it
                        String username = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User";
                        String firstName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
                        String lastName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
                        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                        String profilePicUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

                        User newUser = new User(
                                uid,
                                firstName,
                                lastName,
                                username,
                                "", // Empty bio initially
                                profilePicUrl,
                                email
                        );
                        db.collection("users").document(uid).set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(LoginActivity.this, "User profile created (first login)!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, "Error creating user profile (first login): " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                    // If it exists, do nothing, the ProfileFragment will load it.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Error checking user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
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