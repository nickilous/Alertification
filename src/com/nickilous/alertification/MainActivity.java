package com.nickilous.alertification;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nickilous.alertification.AlertificationService.LocalBinder;

public class MainActivity extends Activity {
    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;
    public static final String START_SERVICE = "com.nickilous.START_SERVICE";
    public static final String STOP_SERVICE = "com.nickilous.STOP_SERVICE";

    AlertificationService mService = null;
    boolean mIsBound;

    private static TextView threadStatus;
    private TextView serviceConnectionStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "<-----OnCreate()----->");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        threadStatus = (TextView) findViewById(R.id.threadStatus);
        serviceConnectionStatus = (TextView) findViewById(R.id.serviceConnectionStatus);

    }

    @Override
    public void onResume() {
        Log.i(TAG, "<-----OnResume()----->");
        super.onResume();
        CheckIfServiceIsRunning();
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
            Log.i(TAG, "<-----OnServiceConnected()----->");
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();

        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "<-----onServiceDisconnected()----->");
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            serviceConnectionStatus.setText("Disconnected.");

        }
    };

    public void setThreadStatus(String string) {
        threadStatus.setText(string);

    }

    public void setServiceConnectionStatus(String string) {
        serviceConnectionStatus.setText(string);
    }

}
