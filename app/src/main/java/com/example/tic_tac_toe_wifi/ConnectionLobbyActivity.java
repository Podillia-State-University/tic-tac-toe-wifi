package com.example.tic_tac_toe_wifi;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.Map;
import android.annotation.SuppressLint;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionLobbyActivity extends AppCompatActivity {

    private static final String TAG = "ConnectionLobbyActivity";
    private static final int SERVER_PORT = 8888;

    private View pulseView1, pulseView2;
    private AnimatorSet fullAnimatorSet;
    private boolean isAnimating = false;
    private View panelServerInfo;
    private TextView tvServerName;
    private TextView tvServerDetails;
    private String myDeviceName = Build.MODEL;

    private boolean isServerRunning = false;
    private boolean isScanning = false;
    private boolean isTransitioningToGame = false;

    private TextView tvSearchStatus;
    private RadioGroup rgConnectionMethod;
    private RadioButton rbWifiDirect;
    private MaterialButton btnCreateGame, btnScan;
    private RecyclerView rvDevices;
    private DeviceAdapter deviceAdapter;

    private List<String> dummyDevicesList;
    private List<WifiP2pDevice> peersList = new ArrayList<>();

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private GameServerThread serverThread;
    private GameClientThread clientThread;

    private IntentFilter intentFilter;
    private WiFiDirectBroadcastReceiver receiver;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    if (!entry.getValue()) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Toast.makeText(this, getString(R.string.perms_granted_wifi_direct), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.perms_denied_wifi_direct), Toast.LENGTH_LONG).show();
                }
            });

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_lobby);

        pulseView1 = findViewById(R.id.pulseView1);
        pulseView2 = findViewById(R.id.pulseView2);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        rgConnectionMethod = findViewById(R.id.rgConnectionMethod);
        rbWifiDirect = findViewById(R.id.rbWifiDirect);
        rvDevices = findViewById(R.id.rvDevices);
        btnCreateGame = findViewById(R.id.btnCreateGame);
        btnScan = findViewById(R.id.btnScan);
        panelServerInfo = findViewById(R.id.panelServerInfo);
        tvServerName = findViewById(R.id.tvServerName);
        tvServerDetails = findViewById(R.id.tvServerDetails);

        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        pulseView1.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        pulseView2.setBackgroundTintList(ColorStateList.valueOf(primaryColor));

        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        dummyDevicesList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(dummyDevicesList);
        rvDevices.setAdapter(deviceAdapter);

        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager != null) {
            channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        }

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        btnCreateGame.setOnClickListener(v -> {
            if (isServerRunning) stopServerLogic();
            else startServerLogic();
        });

        btnScan.setOnClickListener(v -> {
            if (isServerRunning) stopServerLogic();
            if (isScanning) stopClientScanLogic();
            else startClientScanLogic();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @SuppressLint("MissingPermission")
    private void startClientScanLogic() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, getString(R.string.scanning_permissions_needed), Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
            deviceAdapter.clearSelection();
            return;
        }

        isScanning = true;
        String method = rbWifiDirect.isChecked() ? getString(R.string.radio_wifi_direct) : getString(R.string.radio_hotspot);
        tvSearchStatus.setText(getString(R.string.status_searching, method));

        btnScan.setText(getString(R.string.btn_stop_scanning));
        int redColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
        btnScan.setBackgroundTintList(ColorStateList.valueOf(redColor));

        startRadarAnimation();
        dummyDevicesList.clear();
        peersList.clear();
        deviceAdapter.notifyDataSetChanged();

        if (rbWifiDirect.isChecked() && wifiP2pManager != null) {
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ConnectionLobbyActivity.this, getString(R.string.toast_search_started), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(ConnectionLobbyActivity.this, getString(R.string.discover_failure, reason), Toast.LENGTH_SHORT).show();
                    stopClientScanLogic();
                }
            });
        } else {
            dummyDevicesList.add(getString(R.string.device_hotspot_sample));
            deviceAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("MissingPermission")
    private void stopClientScanLogic() {
        isScanning = false;
        tvSearchStatus.setText(getString(R.string.status_idle));

        btnScan.setText(getString(R.string.btn_scan));
        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        btnScan.setBackgroundTintList(ColorStateList.valueOf(primaryColor));

        stopRadarAnimation();

        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.stopPeerDiscovery(channel, null);
        }
    }

    @SuppressLint("MissingPermission")
    private void startServerLogic() {
        if (isScanning) stopClientScanLogic();

        updateServerUI(true);
        startRadarAnimation();

        dummyDevicesList.clear();
        peersList.clear();
        deviceAdapter.notifyDataSetChanged();

        if (rbWifiDirect.isChecked()) {
            if (!hasRequiredPermissions()) {
                checkAndRequestPermissions();
                stopServerLogic();
                return;
            }

            if (wifiP2pManager != null && channel != null) {
                wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ConnectionLobbyActivity.this, getString(R.string.p2p_group_created), Toast.LENGTH_SHORT).show();
                        startServerThread();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(ConnectionLobbyActivity.this, getString(R.string.p2p_group_create_failed, reason), Toast.LENGTH_SHORT).show();
                        stopServerLogic();
                    }
                });
            }
        } else {
            startServerThread();
        }
    }

    @SuppressLint("MissingPermission")
    private void stopServerLogic() {
        if (serverThread != null) {
            serverThread.stopServer();
            serverThread = null;
        }
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.removeGroup(channel, null);
        }
        stopRadarAnimation();
        updateServerUI(false);
    }

    private void updateServerUI(boolean isRunning) {
        isServerRunning = isRunning;
        String method = rbWifiDirect.isChecked() ? getString(R.string.radio_wifi_direct) : getString(R.string.radio_hotspot);

        if (isRunning) {
            rgConnectionMethod.setVisibility(View.GONE);
            tvSearchStatus.setText(getString(R.string.status_hosting, method));

            btnCreateGame.setText(getString(R.string.btn_cancel));
            int redColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
            btnCreateGame.setTextColor(redColor);
            btnCreateGame.setStrokeColor(ColorStateList.valueOf(redColor));

            panelServerInfo.setVisibility(View.VISIBLE);
            tvServerName.setText(getString(R.string.server_name_format, myDeviceName));

            if (rbWifiDirect.isChecked()) {
                tvServerDetails.setText(getString(R.string.hint_wifi_direct_accept));
            } else {
                tvServerDetails.setText(getString(R.string.hint_hotspot_settings));
            }
        } else {
            rgConnectionMethod.setVisibility(View.VISIBLE);
            tvSearchStatus.setText(getString(R.string.status_idle));

            btnCreateGame.setText(getString(R.string.btn_create_game));
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            btnCreateGame.setTextColor(primaryColor);
            btnCreateGame.setStrokeColor(ColorStateList.valueOf(primaryColor));

            panelServerInfo.setVisibility(View.GONE);
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void startRadarAnimation() {
        if (isAnimating) return;
        pulseView1.setAlpha(1f); pulseView2.setAlpha(1f);

        ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(pulseView1, "scaleX", 0.16f, 1f);
        ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(pulseView1, "scaleY", 0.16f, 1f);
        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(pulseView1, "alpha", 1f, 0f);

        ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(pulseView2, "scaleX", 0.16f, 1f);
        ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(pulseView2, "scaleY", 0.16f, 1f);
        ObjectAnimator alpha2 = ObjectAnimator.ofFloat(pulseView2, "alpha", 1f, 0f);

        scaleX1.setRepeatCount(ObjectAnimator.INFINITE); scaleY1.setRepeatCount(ObjectAnimator.INFINITE); alpha1.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX2.setRepeatCount(ObjectAnimator.INFINITE); scaleY2.setRepeatCount(ObjectAnimator.INFINITE); alpha2.setRepeatCount(ObjectAnimator.INFINITE);

        AnimatorSet animatorSet1 = new AnimatorSet(); animatorSet1.playTogether(scaleX1, scaleY1, alpha1);
        AnimatorSet animatorSet2 = new AnimatorSet(); animatorSet2.playTogether(scaleX2, scaleY2, alpha2);
        animatorSet2.setStartDelay(1000);

        fullAnimatorSet = new AnimatorSet();
        fullAnimatorSet.playTogether(animatorSet1, animatorSet2);
        fullAnimatorSet.setDuration(2000);
        fullAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        fullAnimatorSet.start();

        isAnimating = true;
    }

    private void stopRadarAnimation() {
        if (fullAnimatorSet != null && isAnimating) {
            fullAnimatorSet.cancel();
            pulseView1.setAlpha(0f);
            pulseView2.setAlpha(0f);
            isAnimating = false;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRadarAnimation();

        if (!isTransitioningToGame) {
            if (isScanning) stopClientScanLogic();
            if (isServerRunning) stopServerLogic();
            if (clientThread != null) clientThread.stopClient();
        }

        if (channel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            channel.close();
        }
    }

    private WifiP2pManager.PeerListListener peerListListener = peerList -> {
        if (!peerList.getDeviceList().equals(peersList)) {
            peersList.clear();
            peersList.addAll(peerList.getDeviceList());

            dummyDevicesList.clear();
            for (WifiP2pDevice device : peersList) {
                dummyDevicesList.add(device.deviceName + getString(R.string.suffix_wifi_direct));
            }
            deviceAdapter.notifyDataSetChanged();
        }
    };

    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = info -> {
        final InetAddress groupOwnerAddress = info.groupOwnerAddress;

        if (info.groupFormed && info.isGroupOwner) {
            Log.d(TAG, "Я Сервер (Group Owner)");
        } else if (info.groupFormed) {
            Log.d(TAG, "Я Клієнт. IP Сервера: " + groupOwnerAddress.getHostAddress());
            tvSearchStatus.setText(getString(R.string.connected_starting_game));

            if (clientThread == null || !clientThread.isAlive()) {
                clientThread = new GameClientThread(this, groupOwnerAddress);
                clientThread.start();
            }
        }
    };

    class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
        private final List<String> devices;
        private int selectedPosition = -1;
        private boolean isReadyToStart = false;

        public DeviceAdapter(List<String> devices) { this.devices = devices; }

        public void clearSelection() {
            selectedPosition = -1;
            isReadyToStart = false;
            notifyDataSetChanged();
        }

        public void setReadyToStart() {
            isReadyToStart = true;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
            return new DeviceViewHolder(view);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            String deviceName = devices.get(position);
            holder.tvDeviceName.setText(deviceName);

            if (position == selectedPosition) {
                holder.ivDeviceIcon.setVisibility(View.GONE);
                if (isReadyToStart) {
                    holder.tvDeviceStatus.setText("Підключено!");
                    holder.btnStartGame.setVisibility(View.VISIBLE);
                } else {
                    holder.tvDeviceStatus.setText("Підключення...");
                    holder.btnStartGame.setVisibility(View.GONE);
                }
            } else {
                holder.ivDeviceIcon.setVisibility(View.VISIBLE);
                holder.tvDeviceStatus.setText("Доступний для гри");
                holder.btnStartGame.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (position == selectedPosition) return;

                selectedPosition = position;
                isReadyToStart = false;
                notifyDataSetChanged();

                Toast.makeText(ConnectionLobbyActivity.this, getString(R.string.connecting_to_device, deviceName), Toast.LENGTH_SHORT).show();
                stopRadarAnimation();
                tvSearchStatus.setText(getString(R.string.status_connecting));

                if (rbWifiDirect.isChecked() && position < peersList.size()) {
                    WifiP2pDevice selectedDevice = peersList.get(position);
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = selectedDevice.deviceAddress;

                    wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {}

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(ConnectionLobbyActivity.this, getString(R.string.connection_failure), Toast.LENGTH_SHORT).show();
                            tvSearchStatus.setText(getString(R.string.status_searching, getString(R.string.radio_wifi_direct)));
                            startRadarAnimation();
                            clearSelection();
                        }
                    });
                }
            });

            holder.btnStartGame.setOnClickListener(v -> {
                isTransitioningToGame = true; // Запобігаємо розриву сокета

                GameConnectionManager.getInstance().setSocket(clientThread.getSocket());
                GameConnectionManager.getInstance().setHost(false); // Ми клієнт

                Intent intent = new Intent(ConnectionLobbyActivity.this, MultiplayerActivity.class);
                startActivity(intent);
                finish();
            });
        }

        @Override
        public int getItemCount() { return devices.size(); }

        class DeviceViewHolder extends RecyclerView.ViewHolder {
            ImageView ivDeviceIcon; TextView tvDeviceName, tvDeviceStatus; MaterialButton btnStartGame;
            public DeviceViewHolder(@NonNull View itemView) {
                super(itemView);
                ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
                tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
                tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
                btnStartGame = itemView.findViewById(R.id.btnStartGame);
            }
        }
    }

    private class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (wifiP2pManager != null) wifiP2pManager.requestPeers(channel, peerListListener);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (wifiP2pManager == null) return;
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    runOnUiThread(() -> Toast.makeText(ConnectionLobbyActivity.this, getString(R.string.wifi_direct_disabled), Toast.LENGTH_LONG).show());
                    stopClientScanLogic();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                if (device != null) {
                    myDeviceName = device.deviceName;
                    if (isServerRunning) tvServerName.setText(getString(R.string.server_name_format, myDeviceName));
                }
            }
        }
    }

    private void startServerThread() {
        if (serverThread == null || !serverThread.isAlive()) {
            serverThread = new GameServerThread(this);
            serverThread.start();
        }
    }

    private static class GameServerThread extends Thread {
        private final WeakReference<ConnectionLobbyActivity> activityRef;
        private ServerSocket serverSocket;
        private boolean isRunning = true;
        public GameServerThread(ConnectionLobbyActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();

                    ConnectionLobbyActivity activity = activityRef.get();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        activity.runOnUiThread(() -> {
                            activity.stopRadarAnimation();
                            activity.updateServerUI(false);
                            activity.tvSearchStatus.setText(activity.getString(R.string.player_joined));

                            activity.isTransitioningToGame = true;
                            GameConnectionManager.getInstance().setSocket(clientSocket);
                            GameConnectionManager.getInstance().setHost(true);

                            Intent intent = new Intent(activity, MultiplayerActivity.class);
                            activity.startActivity(intent);
                            activity.finish();
                        });
                    }
                    break;
                }
            } catch (IOException e) {
                if (isRunning) Log.e(TAG, "Неочікувана помилка", e);
            }
        }

        public void stopServer() {
            isRunning = false;
            if (serverSocket != null) {
                try { serverSocket.close(); } catch (IOException ignored) {}
            }
        }
    }

    private static class GameClientThread extends Thread {
        private final WeakReference<ConnectionLobbyActivity> activityRef;
        private Socket socket;
        private final String hostAddress;

        public GameClientThread(ConnectionLobbyActivity activity, InetAddress hostAddress) {
            this.activityRef = new WeakReference<>(activity);
            this.hostAddress = hostAddress.getHostAddress();
        }

        public Socket getSocket() { return socket; }

        @Override
        public void run() {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostAddress, SERVER_PORT), 10000);

                ConnectionLobbyActivity activity = activityRef.get();
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, activity.getString(R.string.game_started_client), Toast.LENGTH_SHORT).show();
                        if (activity.deviceAdapter != null) activity.deviceAdapter.setReadyToStart();
                        activity.tvSearchStatus.setText("З'єднання встановлено. Готово до гри!");
                    });
                }

            } catch (IOException e) {
                ConnectionLobbyActivity activity = activityRef.get();
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, activity.getString(R.string.socket_connection_failed), Toast.LENGTH_LONG).show();
                        activity.tvSearchStatus.setText(activity.getString(R.string.status_idle));
                        if (activity.deviceAdapter != null) activity.deviceAdapter.clearSelection();
                    });
                }
            }
        }

        public void stopClient() {
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

}