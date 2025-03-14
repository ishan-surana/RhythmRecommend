package com.example.rhythmrecommend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rhythmrecommend.RecentPdfsAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private Button selectPdfButton;
    private FloatingActionButton settingsButton;

    private List<Uri> recentPdfs = new ArrayList<>();
    private RecentPdfsAdapter recentPdfsAdapter;
    private RecyclerView recentPdfsRecyclerView;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String DARK_MODE_KEY = "dark_mode_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        if (isDarkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        recentPdfsRecyclerView = findViewById(R.id.recent_books_recycler_view);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recentPdfsRecyclerView.setLayoutManager(layoutManager);
        recentPdfsAdapter = new RecentPdfsAdapter(recentPdfs, this);
        recentPdfsRecyclerView.setAdapter(recentPdfsAdapter);

        // Handle PDF selection
        selectPdfButton = findViewById(R.id.select_pdf_button);
        selectPdfButton.setOnClickListener(view -> getContentLauncher.launch("application/pdf"));

        settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(View -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    // Declare an ActivityResultLauncher to launch the file picker
    private final ActivityResultLauncher<String> getContentLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        if(!recentPdfs.contains(uri)) {
                            recentPdfs.add(0, uri);
                            if(recentPdfs.size() > 4) recentPdfs.remove(recentPdfs.size()-1);
                            recentPdfsAdapter.notifyDataSetChanged();
                        }
                        Intent intent = new Intent(MainActivity.this, ReadingActivity.class);
                        intent.setData(uri);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to select PDF.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
}
