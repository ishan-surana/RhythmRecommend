package com.example.rhythmrecommend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.github.barteksc.pdfviewer.PDFView;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadingActivity extends AppCompatActivity {
    private FloatingActionButton musicFab, settingsButton;
    private MediaPlayer mediaPlayer;
    private ProgressBar progressBar;
    private boolean isMusicPlaying = true;
    private String currentMood = "neutral";
    private ExecutorService executorService;
    private int previousParagraphIndex = -1;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String COLOR_KEY = "saved_color";
    private static final String DIRECTION_KEY = "direction_horizontal";

    private static final String SENTIMENT_API = "https://rhythm-recommend-backend.vercel.app/sentiment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PDFBoxResourceLoader.init(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);
        PDFView pdfView = findViewById(R.id.pdfView);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(View -> {
            Intent intent = new Intent(ReadingActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        musicFab = findViewById(R.id.music_fab);
        progressBar = findViewById(R.id.progress_bar);
        musicFab.startAnimation(AnimationUtils.loadAnimation(this, R.anim.spin_animation));
        musicFab.setOnClickListener(view -> toggleMusic());
        executorService = Executors.newFixedThreadPool(2);
        startNeutralMusic();

        Uri pdfUri = getIntent().getData();
        assert pdfUri != null;

        try {
            Cursor returnCursor = getContentResolver().query(pdfUri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            returnCursor.moveToFirst();
            String name = returnCursor.getString(0);
            returnCursor.close();

            TextView title = findViewById(R.id.document_title);
            title.setText(name);

            boolean swipe_dir = sharedPreferences.getBoolean(DIRECTION_KEY, true);
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);

            if (inputStream != null) {
                pdfView.fromStream(inputStream)
                        .enableSwipe(true)
                        .swipeHorizontal(swipe_dir)
                        .enableDoubletap(true)
                        .onPageScroll((position, offset) -> {
                            int currentParagraph = calculateCurrentParagraph(position);
                            if (currentParagraph != previousParagraphIndex) {
                                extractAndProcessParagraph(pdfUri, currentParagraph);
                                previousParagraphIndex = currentParagraph;
                            }
                        })
                        .load();
            } else {
                Toast.makeText(this, "Failed to open PDF stream", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading PDF file", Toast.LENGTH_SHORT).show();
        }

        int savedColor = sharedPreferences.getInt(COLOR_KEY, Color.RED);
        progressBar.setProgressTintList(ColorStateList.valueOf(savedColor));
    }

    private void toggleMusic() {
        if (isMusicPlaying) {
            mediaPlayer.pause();
            musicFab.clearAnimation();
            musicFab.setImageResource(R.drawable.ic_music_off);
        } else {
            mediaPlayer.start();
            musicFab.startAnimation(AnimationUtils.loadAnimation(this, R.anim.spin_animation));
            musicFab.setImageResource(R.drawable.ic_music_note);
        }
        isMusicPlaying = !isMusicPlaying;
    }

    private void startNeutralMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private int calculateCurrentParagraph(float position) {
        return (int) position;
    }

    private void extractAndProcessParagraph(Uri pdfUri, int paragraphIndex) {
        executorService.submit(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(pdfUri)) {
                PDDocument document = PDDocument.load(inputStream);
                String author = "by " + document.getDocumentInformation().getAuthor();
                if (author.equals("by null")) author = "(no author information available)";
                String finalAuthor = author;
                runOnUiThread(() -> ((TextView) findViewById(R.id.document_author)).setText(finalAuthor));

                int progress = (int) (((float) (paragraphIndex + 1) / document.getNumberOfPages()) * 100);
                runOnUiThread(() -> progressBar.setProgress(progress, true));

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(paragraphIndex);
                stripper.setEndPage(Math.min(paragraphIndex + 2, document.getNumberOfPages()));
                String text = stripper.getText(document);
                document.close();

                Log.d("text", text);

                if (text != null && !text.trim().isEmpty()) {
                    analyzeMood(text.trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void analyzeMood(String paragraph) {
        executorService.submit(() -> {
            try {
                URL url = new URL(SENTIMENT_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("text", paragraph);

                try (OutputStream os = conn.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"))) {
                    writer.write(body.toString());
                    writer.flush();
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

//                String mood = response.toString().replace("\"", "").toLowerCase(); // Clean response
                JSONObject jsonResponse = new JSONObject(response.toString());
                String mood = jsonResponse.getString("emotion").toLowerCase();
                if (!mood.equals(currentMood)) {
                    runOnUiThread(() -> {
                        changeMusicBasedOnMood(mood);
                    });
                    currentMood = mood;
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void changeMusicBasedOnMood(String mood) {
        if (mood == null || mood.isEmpty()) mood = "neutral";

        Log.d("ReadingChange", "Mood: " + mood);

        HashMap<String, Integer> songs = new HashMap<>();
        songs.put("neutral", R.raw.neutral_music);
        songs.put("happy", R.raw.happy_music);
        songs.put("sad", R.raw.sad_music);
        songs.put("angry", R.raw.angry_music);
        songs.put("excited", R.raw.excited_music);
        songs.put("calm", R.raw.calm_music);
        Log.d("SONGS", songs.toString());

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, songs.getOrDefault(mood, R.raw.neutral_music));
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            Log.d("MusicChange", "Music changed to: " + mood);
        } catch (Exception e) {
            Log.d("ERROR", "Error changing music");
            e.printStackTrace();
            mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
