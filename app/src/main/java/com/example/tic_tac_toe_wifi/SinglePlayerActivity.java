package com.example.tic_tac_toe_wifi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class SinglePlayerActivity extends AppCompatActivity {

    private String playerName;
    private boolean useVibration;
    private String gameStyle;
    private int aiLevel = 3;

    private int countWins = 0, countLosses = 0, countDraws = 0;
    private boolean isStatsExpanded = false;
    private int defaultTimerColor;

    private TextView tvWins, tvLosses, tvDraws, tvStatsTitle;
    private android.widget.LinearLayout layoutStatsContent, btnToggleStats;
    private androidx.constraintlayout.widget.ConstraintLayout panelGameStatus;

    private Button[] buttons = new Button[9];
    private Button btnRestart;
    private int[] gameState = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    private boolean isPlayerTurn = true;
    private boolean gameActive = true;
    private boolean isPlayerX = true;

    private TextView tvTimer, tvPlayerX, tvPlayerO;
    private CountDownTimer timer;
    private Vibrator vibrator;
    private GestureDetector gestureDetector;
    private TicTacToeBot bot;
    private SoundManager soundManager;

    private final int[][] winPositions = {
            {0,1,2}, {3,4,5}, {6,7,8},
            {0,3,6}, {1,4,7}, {2,5,8},
            {0,4,8}, {2,4,6}
    };

    private Runnable aiMoveRunnable = this::aiMove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_board);

        bot = new TicTacToeBot();

        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        playerName = prefs.getString(SettingsActivity.KEY_PLAYER_NAME, getString(R.string.txt_default_player));
        useVibration = prefs.getBoolean(SettingsActivity.KEY_VIBRATION, true);
        aiLevel = (int) prefs.getFloat(SettingsActivity.KEY_DIFFICULTY, 3.0f);
        gameStyle = prefs.getString(SettingsActivity.KEY_GAME_STYLE, "classic");

        panelGameStatus = findViewById(R.id.panelGameStatus);
        tvTimer = findViewById(R.id.tvTimer);
        tvPlayerX = findViewById(R.id.tvPlayerX);
        tvPlayerO = findViewById(R.id.tvPlayerO);
        defaultTimerColor = tvTimer.getTextColors().getDefaultColor();

        tvWins = findViewById(R.id.tvWins);
        tvLosses = findViewById(R.id.tvLosses);
        tvDraws = findViewById(R.id.tvDraws);
        tvStatsTitle = findViewById(R.id.tvStatsTitle);
        layoutStatsContent = findViewById(R.id.layoutStatsContent);
        btnToggleStats = findViewById(R.id.btnToggleStats);

        soundManager = new SoundManager(this, prefs.getFloat(SettingsActivity.KEY_VOLUME, 50.0f));
        updateStatsText();

        btnToggleStats.setOnClickListener(v -> {
            isStatsExpanded = !isStatsExpanded;
            layoutStatsContent.setVisibility(isStatsExpanded ? android.view.View.VISIBLE : android.view.View.GONE);
            tvStatsTitle.setText(getString(isStatsExpanded ? R.string.stats_expanded : R.string.stats_collapsed));
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator = ((VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        for (int i = 0; i < 9; i++) {
            int resID = getResources().getIdentifier("btn_" + i, "id", getPackageName());
            buttons[i] = findViewById(resID);
            final int index = i;
            buttons[i].setOnClickListener(v -> onPlayerMove(index));
        }

        btnRestart = findViewById(R.id.btnRestart);
        btnRestart.setEnabled(false);
        btnRestart.setOnClickListener(v -> restartGame());

        restartGame();
    }

    private void updateStatsText() {
        tvWins.setText(getString(R.string.stats_wins, countWins));
        tvDraws.setText(getString(R.string.stats_draws, countDraws));
        tvLosses.setText(getString(R.string.stats_losses, countLosses));
    }

    private void onPlayerMove(int index) {
        if (gameState[index] == 0 && isPlayerTurn && gameActive) {
            makeMove(index, 1);
            if (gameActive) {
                isPlayerTurn = false;
                updateTurnIndicator();
                tvTimer.postDelayed(aiMoveRunnable, 500);
            }
        }
    }

    private void aiMove() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!gameActive) return;
        int moveIndex = bot.getBestMove(gameState, aiLevel);
        if (moveIndex != -1) {
            makeMove(moveIndex, 2);
            if (gameActive) {
                isPlayerTurn = true;
                updateTurnIndicator();
            }
        }
    }

    private void makeMove(int index, int player) {
        gameState[index] = player;
        boolean isPets = gameStyle.equals("pets");

        String symbolX = isPets ? "🐶" : "X";
        String symbolO = isPets ? "🐱" : "O";
        String symbol = (player == 1) ? (isPlayerX ? symbolX : symbolO) : (isPlayerX ? symbolO : symbolX);

        int color = ContextCompat.getColor(this, (symbol.equals("X") || symbol.equals("🐶")) ? R.color.color_x : R.color.color_o);

        buttons[index].setText(symbol);
        buttons[index].setTextColor(color);
        buttons[index].animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction(() ->
                buttons[index].animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        ).start();

        vibrate(50);
        soundManager.playMoveSound();
        checkWinOrDraw();

        if (gameActive) startTimer();
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        tvTimer.setTextColor(defaultTimerColor);

        timer = new CountDownTimer(15000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvTimer.setText(String.valueOf(secondsLeft));
                if (secondsLeft <= 5) tvTimer.setTextColor(ContextCompat.getColor(SinglePlayerActivity.this, R.color.error));
            }
            public void onFinish() {
                if (gameActive) {
                    gameActive = false;
                    vibrate(500);
                    countLosses++;
                    updateStatsText();
                    highlightWinnerPanel(2);
                    Toast.makeText(SinglePlayerActivity.this, getString(R.string.msg_timeout), Toast.LENGTH_LONG).show();
                    updateTurnIndicator();
                    btnRestart.setEnabled(true);
                }
            }
        }.start();
    }

    private void updateTurnIndicator() {
        int colorX = ContextCompat.getColor(this, R.color.color_x);
        int colorO = ContextCompat.getColor(this, R.color.color_o);
        int colorGray = ContextCompat.getColor(this, R.color.text_secondary);

        int colorPlayer = isPlayerX ? colorX : colorO;
        int colorAI = isPlayerX ? colorO : colorX;

        if (!gameActive) {
            tvPlayerX.setTextColor(colorGray); tvPlayerO.setTextColor(colorGray);
        } else if (isPlayerTurn) {
            tvPlayerX.setTextColor(colorPlayer); tvPlayerO.setTextColor(colorGray);
        } else {
            tvPlayerX.setTextColor(colorGray); tvPlayerO.setTextColor(colorAI);
        }
    }

    private void checkWinOrDraw() {
        boolean winnerFound = false;
        for (int[] win : winPositions) {
            if (gameState[win[0]] == gameState[win[1]] && gameState[win[1]] == gameState[win[2]] && gameState[win[0]] != 0) {
                winnerFound = true;
                gameActive = false;
                if (timer != null) timer.cancel();
                vibrate(500);

                if (gameState[win[0]] == 1) {
                    countWins++;
                    soundManager.playWinSound();
                    highlightWinnerPanel(1);
                    Toast.makeText(this, getString(R.string.msg_you_win), Toast.LENGTH_LONG).show();
                } else {
                    countLosses++;
                    soundManager.playLoseSound();
                    highlightWinnerPanel(2);
                    Toast.makeText(this, getString(R.string.msg_ai_win), Toast.LENGTH_LONG).show();
                }
                updateStatsText();
                break;
            }
        }

        if (!winnerFound) {
            boolean isDraw = true;
            for (int state : gameState) {
                if (state == 0) { isDraw = false; break; }
            }
            if (isDraw) {
                gameActive = false;
                if (timer != null) timer.cancel();
                countDraws++;
                updateStatsText();
                Toast.makeText(this, getString(R.string.msg_draw), Toast.LENGTH_LONG).show();
            }
        }

        if (!gameActive) {
            updateTurnIndicator();
            btnRestart.setEnabled(true);
        }
    }

    private void highlightWinnerPanel(int player) {
        int colorWin = (player == 1)
                ? ContextCompat.getColor(this, isPlayerX ? R.color.highlight_x : R.color.highlight_o)
                : ContextCompat.getColor(this, isPlayerX ? R.color.highlight_o : R.color.highlight_x);

        panelGameStatus.setBackgroundTintList(ColorStateList.valueOf(colorWin));
        if (player == 1) tvPlayerX.setBackgroundTintList(ColorStateList.valueOf(colorWin));
        else tvPlayerO.setBackgroundTintList(ColorStateList.valueOf(colorWin));
    }

    private void vibrate(int durationMs) {
        if (useVibration && vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(durationMs);
            }
        }
    }

    private void restartGame() {
        gameActive = true;
        btnRestart.setEnabled(false);

        panelGameStatus.setBackgroundTintList(null);
        tvPlayerX.setBackgroundTintList(null);
        tvPlayerO.setBackgroundTintList(null);

        isPlayerTurn = new Random().nextBoolean();
        isPlayerX = new Random().nextBoolean();

        tvPlayerX.setText(playerName + (isPlayerX ? " (X)" : " (O)"));
        tvPlayerO.setText("Бот " + (isPlayerX ? "(O)" : "(X)"));

        for (int i = 0; i < 9; i++) {
            gameState[i] = 0;
            buttons[i].setText("");
        }

        updateTurnIndicator();
        startTimer();
        vibrate(50);

        if (!isPlayerTurn) tvTimer.postDelayed(aiMoveRunnable, 600);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (soundManager != null) soundManager.release();

        // ДОДАНО: Видаляємо завдання з черги UI-потоку
        if (tvTimer != null) {
            tvTimer.removeCallbacks(aiMoveRunnable);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 != null && e2 != null) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY) && diffX > 100 && Math.abs(velocityX) > 100) {
                    finish();
                    return true;
                }
            }
            return false;
        }
    }
}