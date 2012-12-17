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
import com.nickilous.alertification.service.AlertificationService;

public class MainActivity extends Activity implements
        OnSharedPreferenceChangeListener {
    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Static final String Identifiers
    public static final String START_SERVICE = "com.nickilous.START_SERVICE";
    public static final String STOP_SERVICE = "com.nickilous.STOP_SERVICE";
    public static final String SERVER_IP = "SERVER_IP";
    public static final String SERVER_PORT = "SERVER_PORT";

    // Service members
    Messenger mService = null;
    boolean mIsBound;

    // UI Members
    private static TextView threadStatus;
    private static TextView serviceConnectionStatus;
    private static TextView serverIP;
    private static TextView serverPort;

    // Preferences
    private SharedPreferences sharedPref;
    private boolean serverEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "<-----OnCreate()----->");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Shared Pref
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        serverEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);

        // UI Handles
        threadStatus = (TextView) findViewById(R.id.threadStatus);
        serviceConnectionStatus = (TextView) findViewById(R.id.serviceConnectionStatus);
        serverIP = (TextView) findViewById(R.id.serverIP);
        serverPort = (TextView) findViewById(R.id.serverPort);

    }

    @Override
    public void onResume() {
        Log.d(TAG, "<-----OnResume()----->");
        super.onResume();
        CheckIfServiceIsRunning();
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        if (!serverEnabled) {
            serverIP.setVisibility(View.VISIBLE);
            serverPort.setVisibility(View.VISIBLE);
        } else {
            serverIP.setVisibility(View.INVISIBLE);
            serverPort.setVisibility(View.INVISIBLE);
        }

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
        Intent stopServerIntent = new Intent();
        stopServerIntent.setAction(STOP_SERVICE);
        startService(stopServerIntent);
        doUnbindService();
    }

    public void startService(View v) {
        Toast.makeText(getApplicationContext(), "Starting Server",
                Toast.LENGTH_LONG).show();
        Intent startServerIntent = new Intent();
        startServerIntent.setAction(START_SERVICE);
        if (serverEnabled) {
            threadStatus.setText("Server is running on IP:PORT: "
                    + NetworkTools.getLocalIpAddress() + ":"
                    + NetworkTools.SERVER_PORT);
        } else {
            startServerIntent
                    .putExtra(SERVER_IP, serverIP.getText().toString());
            startServerIntent.putExtra(SERVER_PORT,
                    Integer.parseInt(serverPort.getText().toString()));
        }
        startService(startServerIntent);
        doBindService();
    }

    private void CheckIfServiceIsRunning() {
        Log.d(TAG, "<-----CheckIfServiceIsRunning()----->");
        // If the service is running when the activity starts, we want to
        // automatically bind to it.
        if (AlertificationService.isRunning()) {
            doBindService();
        }
    }

    void doBindService() {
        Log.d(TAG, "<-----doBindService()----->");
        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(this, AlertificationService.class), mConnection,
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

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(AlertificationPreferenceActivity.SERVER_ENABLED)) {
            serverEnabled = sharedPref.getBoolean(
                    AlertificationPreferenceActivity.SERVER_ENABLED, false);
            if (!serverEnabled) {
                serverIP.setVisibility(View.VISIBLE);
                serverPort.setVisibility(View.VISIBLE);
            } else {
                serverIP.setVisibility(View.INVISIBLE);
                serverPort.setVisibility(View.INVISIBLE);
            }
        }
    }

}
