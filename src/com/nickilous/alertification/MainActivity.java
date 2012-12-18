package com.nickilous.alertification;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nickilous.alertification.network.NetworkTools;
import com.nickilous.alertification.service.NetworkDiscoveryService;
import com.nickilous.alertification.service.NetworkService;

public class MainActivity extends Activity implements
        OnSharedPreferenceChangeListener {
    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Service members
    Messenger mService = null;
    boolean mIsBound;

    // UI Members
    private static TextView connectionStatus;
    private static TextView serviceConnectionStatus;
    private static TextView serverIP;
    private static TextView serverPort;

    // Preferences
    private SharedPreferences sharedPref;
    private boolean serverEnabled;
    private boolean networkDiscoveryEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "<-----OnCreate()----->");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Shared Pref
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        serverEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);
        networkDiscoveryEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.NETWORK_DISCOVERY_ENABLED,
                false);

        // UI Handles
        connectionStatus = (TextView) findViewById(R.id.connection_status);

        serverIP = (TextView) findViewById(R.id.serverIP);
        serverPort = (TextView) findViewById(R.id.serverPort);

    }

    @Override
    public void onResume() {
        Log.d(TAG, "<-----OnResume()----->");
        super.onResume();
        CheckIfServiceIsRunning();
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        checkVisibilityOnUIElements();

    }

    @Override
    public void onPause() {
        Log.d(TAG, "<-----OnPause()----->");
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "<-----OnDestroy----->");
        super.onDestroy();
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_settings:
            startActivity(new Intent(getApplicationContext(),
                    AlertificationPreferenceActivity.class));

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void stopService(View v) {
        Toast.makeText(getApplicationContext(), "Stopping Server",
                Toast.LENGTH_LONG).show();
        startService(NetworkService.getStopIntent());
        doUnbindService();
    }

    public void startService(View v) {
        Toast.makeText(getApplicationContext(), "Starting Server",
                Toast.LENGTH_LONG).show();

        if (serverEnabled && !networkDiscoveryEnabled) {
            connectionStatus.setText("Server is running on IP:PORT: "
                    + NetworkTools.getLocalIpAddress() + ":"
                    + NetworkTools.DEFAULT_SERVER_PORT);
            startService(NetworkService.getStartServerIntent());
            doBindService(NetworkService.class);
        } else {
            startService(NetworkService.getStartClientIntent(serverIP.getText()
                    .toString(), serverPort.getText().toString()));
            doBindService(NetworkService.class);
        }

        if (serverEnabled && networkDiscoveryEnabled) {
            startService(NetworkDiscoveryService.getStartListenIntent());
            doBindService(NetworkDiscoveryService.class);
        }
        if (!serverEnabled && networkDiscoveryEnabled) {
            startService(NetworkDiscoveryService.getStartDiscoveryIntent());
            doBindService(NetworkDiscoveryService.class);
        }

    }

    private void CheckIfServiceIsRunning() {
        Log.d(TAG, "<-----CheckIfServiceIsRunning()----->");
        // If the service is running when the activity starts, we want to
        // automatically bind to it.
        if (NetworkService.isRunning()) {
            doBindService(NetworkService.class);
        } else if (NetworkDiscoveryService.isRunning()) {
            doBindService(NetworkDiscoveryService.class);
        }
    }

    void doBindService(Class<?> service) {
        Log.d(TAG, "<-----doBindService()----->");
        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(this, service), mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        serviceConnectionStatus.setText("Binding.");
    }

    void doUnbindService() {
        Log.d(TAG, "<-----doUnbindService()----->");
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            serviceConnectionStatus.setText("Unbinding.");
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            serviceConnectionStatus.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            serviceConnectionStatus.setText("Disconnected.");

        }
    };

    public void setServiceConnectionStatus(String string) {
        serviceConnectionStatus.setText(string);
    }

    public void checkVisibilityOnUIElements() {
        serverEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);
        networkDiscoveryEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.NETWORK_DISCOVERY_ENABLED,
                false);

        if (!serverEnabled) {
            serverIP.setVisibility(View.VISIBLE);
            serverPort.setVisibility(View.VISIBLE);
        } else if (networkDiscoveryEnabled) {
            serverIP.setVisibility(View.INVISIBLE);
            serverPort.setVisibility(View.INVISIBLE);
        } else {
            serverIP.setVisibility(View.INVISIBLE);
            serverPort.setVisibility(View.INVISIBLE);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {

        checkVisibilityOnUIElements();

    }

}
