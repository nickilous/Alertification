package com.nickilous.alertification;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
        Log.i(TAG, "<-----OnCreate()----->");
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
        Log.i(TAG, "<-----OnResume()----->");
        super.onResume();
        CheckIfServiceIsRunning();
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        if (serverEnabled) {
            serverIP.setVisibility(View.VISIBLE);
            serverPort.setVisibility(View.VISIBLE);
        } else {
            serverIP.setVisibility(View.INVISIBLE);
            serverPort.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onPause() {
        Log.i(TAG, "<-----OnPause()----->");
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "<-----OnDestroy----->");
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
            startServerIntent.putExtra(SERVER_IP, serverIP.getText());
            startServerIntent
                    .putExtra(SERVER_PORT, Integer.parseInt(SERVER_IP));
        }
        startService(startServerIntent);
        doBindService();
    }

    private void CheckIfServiceIsRunning() {
        Log.i(TAG, "<-----CheckIfServiceIsRunning()----->");
        // If the service is running when the activity starts, we want to
        // automatically bind to it.
        if (AlertificationService.isRunning()) {
            doBindService();
        }
    }

    void doBindService() {
        Log.i(TAG, "<-----doBindService()----->");
        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(this, AlertificationService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        serviceConnectionStatus.setText("Binding.");
    }

    void doUnbindService() {
        Log.i(TAG, "<-----doUnbindService()----->");
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            AlertificationService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            serviceConnectionStatus.setText("Unbinding.");
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

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
            try {
                Message msg = Message.obtain(null,
                        AlertificationService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null, AlertificationService.MSG_SET_VALUE,
                        this.hashCode(), 0);
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            serviceConnectionStatus.setText("Disconnected.");

        }
    };

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case AlertificationService.MSG_SET_THREAD_STATUS:
                threadStatus.setText(msg.getData().getString("str1"));
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    public void setThreadStatus(String string) {
        threadStatus.setText(string);

    }

    public void setServiceConnectionStatus(String string) {
        serviceConnectionStatus.setText(string);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(AlertificationPreferenceActivity.SERVER_ENABLED)) {
            serverEnabled = sharedPref.getBoolean(
                    AlertificationPreferenceActivity.SERVER_ENABLED, false);
            if (serverEnabled) {
                serverIP.setVisibility(View.VISIBLE);
                serverPort.setVisibility(View.VISIBLE);
            } else {
                serverIP.setVisibility(View.INVISIBLE);
                serverPort.setVisibility(View.INVISIBLE);
            }
        }
    }

}
