package com.example.tic_tac_toe_wifi;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MultiplayerActivity extends AppCompatActivity {

    private static final String TAG = "MultiplayerActivity";
    private static final int RESTART_CODE = 99;

    private Button[] buttons = new Button[9];
    private Button btnRestart;
    private int[] gameState = {0, 0, 0, 0, 0, 0, 0, 0, 0};

    private boolean isMyTurn = false;
    private boolean gameActive = true;
    private boolean isHost;
    private int myPlayerCode;

    private TextView tvPlayerX, tvPlayerO, tvTimer;
    private androidx.constraintlayout.widget.ConstraintLayout panelGameStatus;
    private Vibrator vibrator;

    private int countWins = 0, countLosses = 0, countDraws = 0;
    private boolean isStatsExpanded = false;
    private TextView tvWins, tvLosses, tvDraws, tvStatsTitle;
    private android.widget.LinearLayout layoutStatsContent, btnToggleStats;
    // -----------------------------

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Thread networkThread;

    private final int[][] winPositions = {
            {0,1,2}, {3,4,5}, {6,7,8},
            {0,3,6}, {1,4,7}, {2,5,8},
            {0,4,8}, {2,4,6}
    };


    private ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_board);

        tvPlayerX = findViewById(R.id.tvPlayerX);
        tvPlayerO = findViewById(R.id.tvPlayerO);
        tvTimer = findViewById(R.id.tvTimer);
        panelGameStatus = findViewById(R.id.panelGameStatus);
        btnRestart = findViewById(R.id.btnRestart);

        tvTimer.setText("Wi-Fi");

        tvWins = findViewById(R.id.tvWins);
        tvLosses = findViewById(R.id.tvLosses);
        tvDraws = findViewById(R.id.tvDraws);
        tvStatsTitle = findViewById(R.id.tvStatsTitle);
        layoutStatsContent = findViewById(R.id.layoutStatsContent);
        btnToggleStats = findViewById(R.id.btnToggleStats);

        updateStatsText();

        btnToggleStats.setOnClickListener(v -> {
            isStatsExpanded = !isStatsExpanded;
            layoutStatsContent.setVisibility(isStatsExpanded ? android.view.View.VISIBLE : android.view.View.GONE);
            tvStatsTitle.setText(getString(isStatsExpanded ? R.string.stats_expanded : R.string.stats_collapsed));
        });
        // --------------------------------

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        socket = GameConnectionManager.getInstance().getSocket();
        isHost = GameConnectionManager.getInstance().isHost();

        if (socket == null || !socket.isConnected()) {
            Toast.makeText(this, "Помилка з'єднання!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myPlayerCode = isHost ? 1 : 2;
        isMyTurn = isHost;

        tvPlayerX.setText(isHost ? "Ви (X)" : "Опонент (X)");
        tvPlayerO.setText(isHost ? "Опонент (O)" : "Ви (O)");

        for (int i = 0; i < 9; i++) {
            String btnID = "btn_" + i;
            int resID = getResources().getIdentifier(btnID, "id", getPackageName());
            buttons[i] = findViewById(resID);
            final int index = i;
            buttons[i].setOnClickListener(v -> onPlayerMove(index));
        }

        btnRestart.setEnabled(false);
        btnRestart.setOnClickListener(v -> sendRestartRequest());

        setupNetworkStreams();
        updateTurnIndicator();
    }

    private void updateStatsText() {
        tvWins.setText(getString(R.string.stats_wins, countWins));
        tvDraws.setText(getString(R.string.stats_draws, countDraws));
        tvLosses.setText(getString(R.string.stats_losses, countLosses));
    }

    private void setupNetworkStreams() {
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            networkThread = new Thread(() -> {
                try {
                    while (true) {
                        int moveIndex = dis.readInt();
                        runOnUiThread(() -> processOpponentMove(moveIndex));
                    }
                } catch (IOException e) {
                    if (gameActive || !isFinishing()) {
                        runOnUiThread(() -> {
                            Toast.makeText(MultiplayerActivity.this, "Опонент відключився!", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                }
            });
            networkThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Network setup error", e);
        }
    }

    private void onPlayerMove(int index) {
        if (gameState[index] == 0 && isMyTurn && gameActive) {
            makeMove(index, myPlayerCode);
            isMyTurn = false;
            updateTurnIndicator();

            networkExecutor.execute(() -> {
                try {
                    dos.writeInt(index);
                    dos.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Error sending move", e);
                }
            });
        }
    }

    private void processOpponentMove(int index) {
        if (index == RESTART_CODE) {
            Toast.makeText(this, "Опонент почав нову гру", Toast.LENGTH_SHORT).show();
            restartLocalGame();
            return;
        }

        int opponentCode = isHost ? 2 : 1;
        makeMove(index, opponentCode);
        if (gameActive) {
            isMyTurn = true;
            updateTurnIndicator();
        }
    }

    private void makeMove(int index, int playerCode) {
        gameState[index] = playerCode;

        String symbol = (playerCode == 1) ? "X" : "O";
        int color = ContextCompat.getColor(this, (playerCode == 1) ? R.color.color_x : R.color.color_o);

        buttons[index].setText(symbol);
        buttons[index].setTextColor(color);

        buttons[index].animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction(() ->
                buttons[index].animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        ).start();

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(50);
        }

        checkWinOrDraw();
    }

    private void updateTurnIndicator() {
        int colorX = ContextCompat.getColor(this, R.color.color_x);
        int colorO = ContextCompat.getColor(this, R.color.color_o);
        int colorGray = ContextCompat.getColor(this, R.color.text_secondary);

        if (!gameActive) {
            tvPlayerX.setTextColor(colorGray);
            tvPlayerO.setTextColor(colorGray);
            return;
        }

        boolean isXTurn = (isMyTurn && isHost) || (!isMyTurn && !isHost);
        tvPlayerX.setTextColor(isXTurn ? colorX : colorGray);
        tvPlayerO.setTextColor(isXTurn ? colorGray : colorO);
    }

    private void checkWinOrDraw() {
        boolean winnerFound = false;
        for (int[] win : winPositions) {
            if (gameState[win[0]] == gameState[win[1]] &&
                    gameState[win[1]] == gameState[win[2]] &&
                    gameState[win[0]] != 0) {

                winnerFound = true;
                gameActive = false;

                int winnerCode = gameState[win[0]];
                if (winnerCode == myPlayerCode) {
                    countWins++; // ДОДАНО ПІДРАХУНОК СТАТИСТИКИ
                    Toast.makeText(this, "Ви перемогли!", Toast.LENGTH_LONG).show();
                    highlightWinnerPanel(myPlayerCode);
                } else {
                    countLosses++;
                    Toast.makeText(this, "Ви програли!", Toast.LENGTH_LONG).show();
                    highlightWinnerPanel(isHost ? 2 : 1);
                }
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
                countDraws++;
                Toast.makeText(this, getString(R.string.msg_draw), Toast.LENGTH_LONG).show();
            }
        }

        if (!gameActive) {
            updateStatsText();
            updateTurnIndicator();
            btnRestart.setEnabled(true);
        }
    }

    private void highlightWinnerPanel(int playerCode) {
        int colorWin = (playerCode == 1)
                ? ContextCompat.getColor(this, R.color.highlight_x)
                : ContextCompat.getColor(this, R.color.highlight_o);

        panelGameStatus.setBackgroundTintList(ColorStateList.valueOf(colorWin));

        if ((playerCode == 1 && isHost) || (playerCode == 2 && !isHost)) {
            tvPlayerX.setBackgroundTintList(ColorStateList.valueOf(colorWin));
        } else {
            tvPlayerO.setBackgroundTintList(ColorStateList.valueOf(colorWin));
        }
    }

    private void sendRestartRequest() {
        btnRestart.setEnabled(false);

        networkExecutor.execute(() -> {
            try {
                dos.writeInt(RESTART_CODE);
                dos.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending move", e);
            }
        });

        restartLocalGame();
    }

    private void restartLocalGame() {
        gameActive = true;
        btnRestart.setEnabled(false);

        panelGameStatus.setBackgroundTintList(null);
        tvPlayerX.setBackgroundTintList(null);
        tvPlayerO.setBackgroundTintList(null);

        for (int i = 0; i < 9; i++) {
            gameState[i] = 0;
            buttons[i].setText("");
        }

        isMyTurn = isHost;
        updateTurnIndicator();

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(50);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameActive = false;

        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}