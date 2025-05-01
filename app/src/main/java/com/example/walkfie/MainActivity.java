package com.example.walkfie;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private boolean isFirstLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupAnimations();

        Button loginButton = findViewById(R.id.button_login);
        Button registerButton = findViewById(R.id.button_register);

        Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);

        loginButton.setOnClickListener(v -> {
            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            loginButton.startAnimation(slideOut);
            v.postDelayed(() -> {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 0);
        });

        registerButton.setOnClickListener(v -> {
            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            registerButton.startAnimation(slideOut);
            v.postDelayed(() -> {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 0);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFirstLaunch) {
            setupAnimations();
        } else {
            isFirstLaunch = false;
        }
    }

    private void setupAnimations() {
        View mainLayout = findViewById(R.id.main);
        ImageView logo = findViewById(R.id.logoImage);
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
        Button loginButton = findViewById(R.id.button_login);
        Button registerButton = findViewById(R.id.button_register);

        // Reset views
        mainLayout.setAlpha(0f);
        mainLayout.setTranslationY(100f);
        logo.setAlpha(0f);
        logo.setTranslationY(100f);
        buttonContainer.setAlpha(0f);
        loginButton.setAlpha(0f);
        loginButton.setTranslationY(30f);
        registerButton.setAlpha(0f);
        registerButton.setTranslationY(30f);

        logo.setVisibility(View.INVISIBLE);
        buttonContainer.setVisibility(View.INVISIBLE);

        mainLayout.setAlpha(0f);
        mainLayout.setTranslationY(100f);

        mainLayout.post(() -> {
            ObjectAnimator mainFade = ObjectAnimator.ofFloat(mainLayout, "alpha", 0f, 1f);
            ObjectAnimator mainSlide = ObjectAnimator.ofFloat(mainLayout, "translationY", 100f, 0f);
            AnimatorSet mainSet = new AnimatorSet();
            mainSet.setInterpolator(new AccelerateDecelerateInterpolator());
            mainSet.playTogether(mainFade, mainSlide);
            mainSet.setDuration(600);
            mainSet.start();
        });


        new Handler().postDelayed(() -> {
            logo.setVisibility(View.VISIBLE);

            ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
            ObjectAnimator logoSlide = ObjectAnimator.ofFloat(logo, "translationY", 100f, 0f);
            AnimatorSet logoSet = new AnimatorSet();
            logoSet.setInterpolator(new OvershootInterpolator());
            logoSet.playTogether(logoFadeIn, logoSlide);
            logoSet.setDuration(600);
            logoSet.start();
        }, 0);

        new Handler().postDelayed(() -> {
            buttonContainer.setVisibility(View.VISIBLE);
            buttonContainer.setAlpha(1f);

            ObjectAnimator loginFadeIn = ObjectAnimator.ofFloat(loginButton, "alpha", 0f, 1f);
            ObjectAnimator loginTranslate = ObjectAnimator.ofFloat(loginButton, "translationY", 30f, 0f);
            AnimatorSet loginSet = new AnimatorSet();
            loginSet.setInterpolator(new OvershootInterpolator());
            loginSet.playTogether(loginFadeIn, loginTranslate);
            loginSet.setDuration(600);

            ObjectAnimator registerFadeIn = ObjectAnimator.ofFloat(registerButton, "alpha", 0f, 1f);
            ObjectAnimator registerTranslate = ObjectAnimator.ofFloat(registerButton, "translationY", 30f, 0f);
            AnimatorSet registerSet = new AnimatorSet();
            registerSet.setInterpolator(new OvershootInterpolator());
            registerSet.playTogether(registerFadeIn, registerTranslate);
            registerSet.setDuration(600);

            loginSet.setStartDelay(0);
            registerSet.setStartDelay(200);

            loginSet.start();
            registerSet.start();
        }, 0);
    }
}
