package com.example.rhythmrecommend;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;

public class SettingsActivity extends AppCompatActivity {

    private Switch musicSourceSwitch;
    private SegmentedButtonGroup directionToggle;

    private ToggleButton modeToggle;
    private SeekBar volumeSeekBar;
    private Button colorPickerButton;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "AppPrefs";
    private static final String DARK_MODE_KEY = "dark_mode_enabled";
    private static final String DIRECTION_KEY = "direction_horizontal";
    private static final String COLOR_KEY = "saved_color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        modeToggle = findViewById(R.id.theme_toggle);
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);;
        modeToggle.setChecked(isDarkMode);

        modeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(DARK_MODE_KEY, isChecked);
            editor.commit();
        });

        directionToggle = findViewById(R.id.direction);
        directionToggle.setOnPositionChangedListener(position -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(DIRECTION_KEY, position==0);
            editor.apply();
        });

        volumeSeekBar = findViewById(R.id.music_volume_seekbar);
        volumeSeekBar.setMax(100);  // Max volume 100
        final AudioManager[] audioManager = {(AudioManager) getSystemService(Context.AUDIO_SERVICE)};
        int maxVolume = audioManager[0] != null ? audioManager[0].getStreamMaxVolume(AudioManager.STREAM_MUSIC) : 100;
        final int[] currentVolume = {audioManager[0] != null ? audioManager[0].getStreamVolume(AudioManager.STREAM_MUSIC) : 0};

        final int[] volumeProgress = {(int) ((currentVolume[0] / (float) maxVolume) * 100)};
        volumeSeekBar.setProgress(volumeProgress[0]);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (audioManager[0] != null) {
                    int newVolume = (int) ((progress / 100.0f) * maxVolume);
                    audioManager[0].setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Register a broadcast receiver to listen to changes in the system volume and update the SeekBar
        volumeSeekBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            // Listen to volume changes from the hardware buttons
            if (audioManager[0] != null) {
                currentVolume[0] = audioManager[0].getStreamVolume(AudioManager.STREAM_MUSIC);
                volumeProgress[0] = (int) ((currentVolume[0] / (float) audioManager[0].getStreamMaxVolume(AudioManager.STREAM_MUSIC)) * 100);
                volumeSeekBar.setProgress(volumeProgress[0]); // Update SeekBar with the current system volume
            }
        });

        colorPickerButton = findViewById(R.id.color_picker_button);

        colorPickerButton.setOnClickListener(v -> {
            int color = sharedPreferences.getInt(COLOR_KEY, Color.RED);
            ColorPickerDialogBuilder
                    .with(SettingsActivity.this)
                    .setTitle("Choose color")
                    .initialColor(color)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setOnColorSelectedListener(selectedColor -> Toast.makeText(SettingsActivity.this, "onColorSelected: 0x" + Integer.toHexString(selectedColor), Toast.LENGTH_SHORT).show())
                    .setPositiveButton("Select", new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt(COLOR_KEY, selectedColor);
                            editor.apply();
                        }
                    })
                    .setNegativeButton("Cancel", (DialogInterface dialog, int which) -> {}).build().show();
        });
    }
}
