package com.example.rhythmrecommend;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntroActivity extends AppCompatActivity {
    // In your IntroActivity.java

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        Button skipButton = findViewById(R.id.skip_button);

        // Skip button listener to go directly to the main activity
        skipButton.setOnClickListener(v -> {
            navigateToMainActivity();
        });
    }

    // Method to navigate to the main activity
    private void navigateToMainActivity() {
        Intent intent = new Intent(IntroActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
