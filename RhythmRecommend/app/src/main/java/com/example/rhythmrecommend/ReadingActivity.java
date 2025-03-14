package com.example.rhythmrecommend;

import android.content.ContentResolver;
import android.content.Context;
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
import androidx.core.net.UriCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.github.barteksc.pdfviewer.PDFView;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadingActivity extends AppCompatActivity {
    private FloatingActionButton musicFab;
    private MediaPlayer mediaPlayer;
    private ProgressBar progressBar;
    private boolean isMusicPlaying = true;
    private String currentMood = "neutral"; // Track the current mood
    private ExecutorService executorService;
    private int previousParagraphIndex = -1; // Track the last processed paragraph index

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String COLOR_KEY = "saved_color";  // Key for color preference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PDFBoxResourceLoader.init(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        // Initialize SharedPreferences to get saved color
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize UI components
        PDFView pdfView = findViewById(R.id.pdfView);
        musicFab = findViewById(R.id.music_fab);
        progressBar = findViewById(R.id.progress_bar);

        // Music playback setup
        musicFab.startAnimation(AnimationUtils.loadAnimation(this, R.anim.spin_animation));
        musicFab.setOnClickListener(view -> toggleMusic());

        // Initialize ExecutorService for background tasks
        executorService = Executors.newFixedThreadPool(2);

        // Initialize and start music early (even before extracting text)
        startNeutralMusic();

        // Get the PDF URI from MainActivity
        Uri pdfUri = getIntent().getData();
        assert pdfUri != null;

        InputStream inputStream = null;

        // Handle URI for content:// scheme (document provider)
        if (Objects.equals(pdfUri.getScheme(), "content")) {
            try {
                // Get and set filename
                String[] projection = new String[] { android.provider.OpenableColumns.DISPLAY_NAME };
                Cursor returnCursor = getContentResolver().query(pdfUri, projection, null, null, null);
                returnCursor.moveToFirst();
                String name = returnCursor.getString(0);
                returnCursor.close();
                TextView title = findViewById(R.id.document_title);
                title.setText(name);

                // Open InputStream for the selected PDF file
                inputStream = getContentResolver().openInputStream(pdfUri);
                if (inputStream != null) {
                    pdfView.fromStream(inputStream)
                            .enableSwipe(true)
                            .swipeHorizontal(true)
                            .enableDoubletap(true)
                            .onPageScroll((position, offset) -> {
                                // Track paragraph change based on scroll position
                                int currentParagraph = calculateCurrentParagraph(position);
                                if (currentParagraph != previousParagraphIndex) {
                                    // Process the paragraph for mood change
                                    extractAndProcessParagraph(pdfUri, currentParagraph);
                                    previousParagraphIndex = currentParagraph; // Update the last processed paragraph
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
        } else {
            Toast.makeText(this, "Unsupported URI scheme", Toast.LENGTH_SHORT).show();
        }

        // Apply saved color to the progress bar
        int savedColor = sharedPreferences.getInt(COLOR_KEY, Color.BLACK); // Default to black if no color is saved
//        progressBar.setProgressDrawableTintList(android.content.res.ColorStateList.valueOf(savedColor)); // Set color of the progress bar
        progressBar.setProgressTintList(ColorStateList.valueOf(savedColor));
    }

    private void toggleMusic() {
        if (isMusicPlaying) {
            mediaPlayer.pause();
            musicFab.clearAnimation();
            musicFab.setImageResource(R.drawable.ic_music_off); // Music off icon
        } else {
            mediaPlayer.start();
            musicFab.startAnimation(AnimationUtils.loadAnimation(this, R.anim.spin_animation)); // Spin animation
            musicFab.setImageResource(R.drawable.ic_music_note); // Music note icon
        }
        isMusicPlaying = !isMusicPlaying;
    }

    // Start the neutral music (even before extracting text)
    private void startNeutralMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    // Function to dynamically calculate the current paragraph based on scroll position
    private int calculateCurrentParagraph(float position) {
        // Simple mapping: position maps to a paragraph index
        return (int) position; // This is a simplified version
    }

    // Extract text and process it paragraph by paragraph dynamically
    private void extractAndProcessParagraph(Uri pdfUri, int paragraphIndex) {
        executorService.submit(() -> {
            ContentResolver contentResolver = getContentResolver();
            String extractedParagraph = "";

            try {
                InputStream inputStream = contentResolver.openInputStream(pdfUri);
                if (inputStream != null) {
                    PDDocument document = PDDocument.load(inputStream);
                    String author = "by " + document.getDocumentInformation().getAuthor();
                    if(author.equals("by null")) author="(no author information available)";
                    TextView subtitle = findViewById(R.id.document_author);
                    subtitle.setText(author);
                    int progress = (int) (((float) (paragraphIndex + 1) / document.getNumberOfPages()) * 100);
                    progressBar.setProgress(progress, true);
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setStartPage(paragraphIndex);  // Extract the current paragraph (approximated)
                    if (paragraphIndex + 2 > document.getNumberOfPages()) stripper.setEndPage(document.getNumberOfPages());  // Set end to the last page if it exceeds
                    else stripper.setEndPage(paragraphIndex + 2);  // Set end to the next two pages
                    extractedParagraph = stripper.getText(document);
                    document.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // After extracting the text, analyze the paragraph
            if (extractedParagraph != null && !extractedParagraph.isEmpty()) {
                String newMood = analyzeMood(extractedParagraph);
                if (!newMood.equals(currentMood) || currentMood == null) {
                    // Change music only if mood is different or if it's the first paragraph
                    runOnUiThread(() -> changeMusicBasedOnMood(newMood));
                    currentMood = newMood;
                }
            }
        });
    }

    // Mood detection logic (simplified, replace with your actual analysis)
    private String analyzeMood(String paragraph) {
        Log.d("EXTRACTED", paragraph);
        // Here you would implement mood analysis based on paragraph content
        // For simplicity, we're just returning "neutral"
        return "neutral";  // Default mood for now
    }

    private void changeMusicBasedOnMood(String mood) {
        // Set default mood to "neutral" if mood is null or any error occurs
        if (mood == null || mood.isEmpty()) {
            mood = "neutral";
        }

        Log.d("ReadingChange", mood);

        HashMap<String, Integer> songs = new HashMap<String, Integer>();
        songs.put("neutral", R.raw.background_music);
        // Other moods can be added here, but "neutral" will be the fallback

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, songs.getOrDefault(mood, R.raw.background_music)); // Default to neutral if mood is not found
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            // Ensure music starts with neutral if an error occurs
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, R.raw.background_music); // Start neutral music on error
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

        // Shut down the executor service
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
