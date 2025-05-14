package com.example.walkfie;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 4500; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView gifView = findViewById(R.id.gifView);

        Glide.with(this)
                .asGif()
                .load(R.raw.walkfienanamannanaman) // Place your gif in res/raw/walkaskdlfasj.gif
                .into(gifView);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish(); // Finish SplashActivity so it won't return
        }, SPLASH_DELAY);
    }
}
