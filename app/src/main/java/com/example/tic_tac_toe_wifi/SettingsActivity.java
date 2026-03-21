package com.example.tic_tac_toe_wifi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.slider.Slider;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etPlayerName = findViewById(R.id.etPlayerName);
        toggleStyle = findViewById(R.id.toggleStyle);
        sliderDifficulty = findViewById(R.id.sliderDifficulty);
        sliderVolume = findViewById(R.id.sliderVolume);
        switchVibration = findViewById(R.id.switchVibration);
        MaterialButton btnSaveSettings = findViewById(R.id.btnSaveSettings);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loadSettings();

        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        String savedName = preferences.getString(KEY_PLAYER_NAME, getString(R.string.txt_default_player));
        etPlayerName.setText(savedName);

        String savedStyle = preferences.getString(KEY_GAME_STYLE, STYLE_CLASSIC);
        if (savedStyle.equals(STYLE_PETS)) {
            toggleStyle.check(R.id.btnStylePets);
        } else {
            toggleStyle.check(R.id.btnStyleClassic);
        }

        float savedDifficulty = preferences.getFloat(KEY_DIFFICULTY, 3.0f);
        sliderDifficulty.setValue(savedDifficulty);

        float savedVolume = preferences.getFloat(KEY_VOLUME, 50.0f);
        sliderVolume.setValue(savedVolume);

        boolean isVibrationOn = preferences.getBoolean(KEY_VIBRATION, true);
        switchVibration.setChecked(isVibrationOn);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();

        String newName = etPlayerName.getText().toString().trim();
        if (newName.isEmpty()) {
            newName = getString(R.string.txt_default_player);
        }
        editor.putString(KEY_PLAYER_NAME, newName);

        int checkedId = toggleStyle.getCheckedButtonId();
        if (checkedId == R.id.btnStylePets) {
            editor.putString(KEY_GAME_STYLE, STYLE_PETS);
        } else {
            editor.putString(KEY_GAME_STYLE, STYLE_CLASSIC);
        }

        editor.putFloat(KEY_DIFFICULTY, sliderDifficulty.getValue());
        editor.putFloat(KEY_VOLUME, sliderVolume.getValue());

        editor.putBoolean(KEY_VIBRATION, switchVibration.isChecked());

        editor.apply();

        Toast.makeText(this, R.string.txt_settings_saved, Toast.LENGTH_SHORT).show();

        finish();
    }
}
