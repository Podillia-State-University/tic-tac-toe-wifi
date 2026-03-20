package com.example.tic_tac_toe_wifi;

import java.net.Socket;

public class GameConnectionManager {
    private static final GameConnectionManager instance = new GameConnectionManager();
    private Socket socket;
    private boolean isHost;

    private GameConnectionManager() {}

    public static GameConnectionManager getInstance() {
        return instance;
    }

    public Socket getSocket() { return socket; }
    public void setSocket(Socket socket) { this.socket = socket; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }
}