package com.example.tic_tac_toe_wifi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton btnPlayHotspot = findViewById(R.id.btnPlayHotspot);
        MaterialButton btnPlaySingle = findViewById(R.id.btnPlaySingle);
        MaterialButton btnSettings = findViewById(R.id.btnSettings);

        btnPlayHotspot.setOnClickListener(v -> {
            startActivity(new Intent(this, ConnectionLobbyActivity.class));
        });

        btnPlaySingle.setOnClickListener(v -> {
            startActivity(new Intent(this, SinglePlayerActivity.class));
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }
}