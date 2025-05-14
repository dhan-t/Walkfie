package com.example.walkfie;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ActiveRecordingActivity extends AppCompatActivity {

    private TextView tvActivityType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_recording);

        tvActivityType = findViewById(R.id.tvActivityType);

        String activityType = getIntent().getStringExtra("activityType");
        tvActivityType.setText("Recording: " + activityType);
    }
}
