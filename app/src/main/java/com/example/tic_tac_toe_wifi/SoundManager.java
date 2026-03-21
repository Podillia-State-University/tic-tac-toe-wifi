package com.example.tic_tac_toe_wifi;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundManager {
    private SoundPool soundPool;
    private int moveSoundId;
    private int winSoundId;
    private int loseSoundId;

    private float volume;

    public SoundManager(Context context, float volumeFromSettings) {
        this.volume = volumeFromSettings / 100f;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5) // Одночасно може грати до 5 звуків
                .setAudioAttributes(audioAttributes)
                .build();

        moveSoundId = soundPool.load(context, R.raw.move, 1);
        winSoundId = soundPool.load(context, R.raw.win, 1);
        loseSoundId = soundPool.load(context, R.raw.lose, 1);
    }

    public void playMoveSound() {
        if (volume > 0) {
            soundPool.play(moveSoundId, volume, volume, 0, 0, 1);
        }
    }

    public void playWinSound() {
        if (volume > 0) {
            soundPool.play(winSoundId, volume, volume, 0, 0, 1);
        }
    }

    public void playLoseSound() {
        if (volume > 0) {
            soundPool.play(loseSoundId, volume, volume, 0, 0, 1);
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}