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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    EditText firstName, lastName, email, password;
    Button registerButton;
    ImageView googleButton, facebookButton;
    TextView orText;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

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

            if (emailInput.isEmpty() || passwordInput.isEmpty() || firstNameInput.isEmpty() || lastNameInput.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(emailInput, passwordInput)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String uid = firebaseUser.getUid();
                                mDatabase.child("users").child(uid).setValue(new User(firstNameInput, lastNameInput, emailInput));
                            }
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
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
