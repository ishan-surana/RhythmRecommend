package com.example.rhythmrecommend;

import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

public class SettingsActivity extends AppCompatActivity {

    private Switch darkModeSwitch;
    private SeekBar volumeSeekBar;  // SeekBar to control volume
    private Button colorPickerButton;  // Button for launching color picker
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "AppPrefs";
    private static final String DARK_MODE_KEY = "dark_mode_enabled";
    private static final String VOLUME_KEY = "saved_volume"; // Key for volume preference
    private static final String COLOR_KEY = "saved_color";  // Key for color preference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Retrieve and apply the dark mode preference
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        darkModeSwitch = findViewById(R.id.theme_switch);
        darkModeSwitch.setChecked(isDarkMode);

        // Set listener for the dark mode switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Switch to Dark Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                // Switch to Light Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Save the dark mode preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(DARK_MODE_KEY, isChecked);
            editor.apply();
        });

        // Initialize AudioManager to control the volume
        volumeSeekBar = findViewById(R.id.music_volume_seekbar);
        volumeSeekBar.setMax(100);  // Max volume 100
        volumeSeekBar.setProgress(sharedPreferences.getInt(VOLUME_KEY, 50));  // Set saved volume

        // Set listener for volume SeekBar
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Save volume value
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(VOLUME_KEY, progress);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initialize Color Picker Button
        colorPickerButton = findViewById(R.id.color_picker_button);

        // Set listener for the color picker button
        colorPickerButton.setOnClickListener(v -> {
            int color = sharedPreferences.getInt(COLOR_KEY, Color.RED); // Get saved color or default to black
            ColorPickerDialogBuilder
                    .with(SettingsActivity.this.peekAvailableContext())
                    .setTitle("Choose color")
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setOnColorSelectedListener(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(int selectedColor) {
                            Toast.makeText(SettingsActivity.this, "onColorSelected: 0x" + Integer.toHexString(selectedColor), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setPositiveButton("ok", new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt(COLOR_KEY, selectedColor);
                            editor.apply();
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .build()
                    .show();
        });
    }
}
