package com.example.tic_tac_toe_wifi;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "TicTacPrefs";
    public static final String KEY_PLAYER_NAME = "playerName";
    public static final String KEY_GAME_STYLE = "gameStyle";
    public static final String KEY_DIFFICULTY = "difficulty";
    public static final String KEY_VOLUME = "volume";
    public static final String KEY_VIBRATION = "vibration";


    public static final String STYLE_CLASSIC = "classic";
    public static final String STYLE_PETS = "pets";

    private TextInputEditText etPlayerName;
    private MaterialButtonToggleGroup toggleStyle;
    private Slider sliderDifficulty;
    private Slider sliderVolume;
    private SwitchMaterial switchVibration;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etPlayerName = findViewById(R.id.etPlayerName);
        toggleStyle = findViewById(R.id.toggleStyle);
        sliderDifficulty = findViewById(R.id.sliderDifficulty);
        sliderVolume = findViewById(R.id.sliderVolume);
        switchVibration = findViewById(R.id.switchVibration);

        MaterialButton btnSaveSettings = findViewById(R.id.btnSaveSettings);

        loadSettings();

        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }
    private void loadSettings() {
        // etPlayerName = findViewById(R.id.etPlayerName);
        String savedName = preferences.getString(KEY_PLAYER_NAME, String.valueOf(R.string.txt_default_player));

    }
    private void saveSettings() {

    }
}
