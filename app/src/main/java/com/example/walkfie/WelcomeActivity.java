package com.example.walkfie;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private static final int WELCOME_DISPLAY_LENGTH = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        TextView welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setAlpha(0f);
        welcomeText.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(300)
                .start();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, WELCOME_DISPLAY_LENGTH);
    }
}
