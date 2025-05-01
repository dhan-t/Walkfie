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

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginButton;
    ImageView googleButton, facebookButton;
    TextView orText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

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
