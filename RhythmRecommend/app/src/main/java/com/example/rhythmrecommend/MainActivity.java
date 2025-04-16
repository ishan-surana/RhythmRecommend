package com.example.rhythmrecommend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

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

        selectPdfButton = findViewById(R.id.select_pdf_button);
        selectPdfButton.setOnClickListener(view -> getContentLauncher.launch("application/pdf"));
        settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(View -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> recentUris = new ArrayList<>();
        for (Uri uri : recentPdfs) {
            recentUris.add(uri.toString()); // Convert Uri to String
        }
        outState.putStringArrayList("recent_pdfs", recentUris);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<String> recentUris = savedInstanceState.getStringArrayList("recent_pdfs");
        if (recentUris != null) {
            recentPdfs.clear();
            for (String uriString : recentUris) {
                recentPdfs.add(Uri.parse(uriString));
            }
            recentPdfsAdapter.notifyDataSetChanged();
        }
    }

}
