package com.example.cancerimager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // --- Configuration ---
    private static final String DEVICE_SSID = "CancerCam";
    private static final String DEVICE_PASSWORD = "raspberry";
    // TODO: Update these URLs for your two video streams
    private static final String DEVICE_URL_1 = "http://192.168.4.1:8000";
    // private static final String DEVICE_URL_2 = "http://192.168.4.200:8000"; // Example for second WebView
    // --------------------

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String KEY_WEBVIEW_CONTAINER_VISIBLE = "is_webview_container_visible";
    private static final String KEY_WEBVIEW1_STATE = "webview1_state";
    // private static final String KEY_WEBVIEW2_STATE = "webview2_state";

    // UI Views
    private WebView webView1;
    // private WebView webView2;
    private LinearLayout webviewContainer;
    private View divider;
    private View initialView;
    private View connectingView;
    private View errorView;
    private View topTaskbar;
    private Button connectButton;
    private TextView statusText;
    private TextView errorText;
    private ImageView connectionStatusIcon;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // Initialize Views
        initializeViews();

        // Configure WebViews
        configureWebView(webView1);
        // configureWebView(webView2);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            showErrorUI("Fatal Error: Could not get Connectivity Service.");
            return;
        }

        setupButtonListeners();

        // Restore state on rotation or recreate
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            showInitialUI(); // If no saved state, start fresh
        }
        // Set initial orientation
        updateLayoutForOrientation(getResources().getConfiguration());

        // Register the new back press handler
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webviewContainer.getVisibility() == View.VISIBLE) {
                    showInitialUI(); // Go back to home screen from webview
                } else {
                    finish(); // Exit the app from the home screen
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayoutForOrientation(newConfig);
    }

    private void updateLayoutForOrientation(Configuration config) {
        LinearLayout.LayoutParams webView1Params = (LinearLayout.LayoutParams) webView1.getLayoutParams();
        // LinearLayout.LayoutParams webView2Params = (LinearLayout.LayoutParams) webView2.getLayoutParams();
        // LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) divider.getLayoutParams();

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            webviewContainer.setOrientation(LinearLayout.HORIZONTAL);
            webView1Params.width = 0;
            webView1Params.height = LinearLayout.LayoutParams.MATCH_PARENT;
            webView1Params.weight = 1;
            // webView2Params.width = 0;
            // webView2Params.height = LinearLayout.LayoutParams.MATCH_PARENT;
            // webView2Params.weight = 1;
            // dividerParams.width = 2;
            // dividerParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
        } else {
            webviewContainer.setOrientation(LinearLayout.VERTICAL);
            webView1Params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            webView1Params.height = 0;
            webView1Params.weight = 1;
            // webView2Params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            // webView2Params.height = 0;
            // webView2Params.weight = 1;
            // dividerParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            // dividerParams.height = 2;
        }
        webView1.setLayoutParams(webView1Params);
        // webView2.setLayoutParams(webView2Params);
        // divider.setLayoutParams(dividerParams);
    }

    private void initializeViews() {
        webView1 = findViewById(R.id.webview1);
        WebView webView2 = findViewById(R.id.webview2);
        webviewContainer = findViewById(R.id.webview_container);
        divider = findViewById(R.id.divider);
        initialView = findViewById(R.id.initial_view);
        connectingView = findViewById(R.id.connecting_view);
        errorView = findViewById(R.id.error_view);
        topTaskbar = findViewById(R.id.top_taskbar);
        connectButton = findViewById(R.id.connect_button);
        statusText = findViewById(R.id.status_text);
        errorText = findViewById(R.id.error_text);
        connectionStatusIcon = findViewById(R.id.connection_status_icon);

        if (webView2 != null) {
            webView2.setVisibility(View.GONE);
        }
        if (divider != null) {
            divider.setVisibility(View.GONE);
        }
    }

    private void configureWebView(WebView webView) {
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
    }

    private void setupButtonListeners() {
        Button retryButton = findViewById(R.id.retry_button);
        ImageButton homeButton = findViewById(R.id.home_button);
        ImageButton refreshButton = findViewById(R.id.refresh_button);
        ImageButton galleryButton = findViewById(R.id.gallery_button);

        retryButton.setOnClickListener(v -> handleConnectClick());
        connectButton.setOnClickListener(v -> handleConnectClick());
        homeButton.setOnClickListener(v -> showInitialUI());
        refreshButton.setOnClickListener(v -> {
            if (webviewContainer.getVisibility() == View.VISIBLE) {
                webView1.reload();
                // webView2.reload();
                Toast.makeText(MainActivity.this, "Refreshing streams...", Toast.LENGTH_SHORT).show();
            }
        });

        galleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        });
    }

    private void restoreState(@NonNull Bundle savedInstanceState) {
        if (savedInstanceState.getBundle(KEY_WEBVIEW1_STATE) != null) {
            webView1.restoreState(savedInstanceState.getBundle(KEY_WEBVIEW1_STATE));
        }
        /*
        if (savedInstanceState.getBundle(KEY_WEBVIEW2_STATE) != null) {
            webView2.restoreState(savedInstanceState.getBundle(KEY_WEBVIEW2_STATE));
        }
        */

        if (savedInstanceState.getBoolean(KEY_WEBVIEW_CONTAINER_VISIBLE)) {
            showWebViewUI(); // If it was visible, show it again without reloading
        } else {
            showInitialUI();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WEBVIEW_CONTAINER_VISIBLE, webviewContainer.getVisibility() == View.VISIBLE);

        Bundle webView1Bundle = new Bundle();
        webView1.saveState(webView1Bundle);
        outState.putBundle(KEY_WEBVIEW1_STATE, webView1Bundle);

        /*
        Bundle webView2Bundle = new Bundle();
        webView2.saveState(webView2Bundle);
        outState.putBundle(KEY_WEBVIEW2_STATE, webView2Bundle);
        */
    }

    private void handleConnectClick() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            showErrorUI("Cannot access Wi-Fi service on this device.");
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(this, "Please enable Wi-Fi, then try connecting again.", Toast.LENGTH_LONG).show();
                Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                startActivity(panelIntent);
            } else {
                wifiManager.setWifiEnabled(true);
                startConnectionProcess();
            }
        } else {
            startConnectionProcess();
        }
    }

    private void startConnectionProcess() {
//        loadWebViews();
        showConnectingUI("Initializing...");
        if (checkAndRequestPermissions()) {
            proceedWithWifiConnection();
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
            } else {
                allPermissionsGranted = false;
            }

            if (allPermissionsGranted) {
                proceedWithWifiConnection();
            } else {
                showErrorUI("Permissions are required to connect to the device.");
            }
        }
    }

    private void proceedWithWifiConnection() {
        showConnectingUI("Searching for " + DEVICE_SSID + "...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(DEVICE_SSID)
                    .setWpa2Passphrase(DEVICE_PASSWORD)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                    runOnUiThread(() -> {
                        loadWebViews();
                        updateConnectionStatus(true, "Connected");
                    });
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    runOnUiThread(() -> {
                        showErrorUI("Connection to device lost. Please retry.");
                        updateConnectionStatus(false, "Connection Lost");
                    });
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    runOnUiThread(() -> {
                        showErrorUI("Could not find '" + DEVICE_SSID + "'. Please ensure the device is on and you are nearby.");
                        updateConnectionStatus(false, "Unavailable");
                    });
                }
            };
            connectivityManager.requestNetwork(request, networkCallback);
        } else {
            showErrorUI("This app requires Android 10+ for reliable connection.");
        }
    }

    private void loadWebViews() {
        showWebViewUI();
        webView1.loadUrl(DEVICE_URL_1);
        // webView2.loadUrl(DEVICE_URL_2);
    }

    // --- UI State Management ---

    private void updateConnectionStatus(boolean isConnected, String status) {
        if (isConnected) {
            connectionStatusIcon.setImageResource(R.drawable.ic_wifi_connected);
            connectionStatusIcon.clearColorFilter();
            if (status != null) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            }
        } else {
            connectionStatusIcon.setImageResource(R.drawable.ic_wifi_disconnected);
            connectionStatusIcon.clearColorFilter();
            if (status != null) {
                Toast.makeText(this, status, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showInitialUI() {
        initialView.setVisibility(View.VISIBLE);
        topTaskbar.setVisibility(View.GONE);
        connectingView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        webviewContainer.setVisibility(View.GONE);
        updateConnectionStatus(false, null); // Set status to disconnected on home screen
    }

    private void showConnectingUI(String message) {
        initialView.setVisibility(View.GONE);
        topTaskbar.setVisibility(View.GONE);
        connectingView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        webviewContainer.setVisibility(View.GONE);
        statusText.setText(message);
    }

    private void showWebViewUI() {
        initialView.setVisibility(View.GONE);
        topTaskbar.setVisibility(View.VISIBLE);
        connectingView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        webviewContainer.setVisibility(View.VISIBLE);
    }

    private void showErrorUI(String message) {
        initialView.setVisibility(View.GONE);
        topTaskbar.setVisibility(View.GONE);
        connectingView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        webviewContainer.setVisibility(View.GONE);
        errorText.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null) {
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.bindProcessToNetwork(null);
            }
        }
    }
}
