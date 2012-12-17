package com.nickilous.alertification.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nickilous.alertification.AlertificationPreferenceActivity;
import com.nickilous.alertification.MainActivity;
import com.nickilous.alertification.R;
import com.nickilous.alertification.network.NetworkThreading;
import com.nickilous.alertification.network.NetworkTools;
import com.nickilous.alertification.network.NsdHelper;

public class NetworkDiscoveryService extends Service {
    // Debugging
    private static final String TAG = "NetworkDiscoveryService";
    private static final boolean D = true;

    private Context mContext;
    private NsdHelper mNsdHelper;
    private SharedPreferences mSharedPref;
    private NotificationManager mNotificationManager;
    private String mServerIP;
    private int mServerPort;
    private NetworkThreading mNetworkThreading;

    private boolean bServerEnabled;

    // Service stop and start commands
    public static final String START_SERVICE = "START_SERVICE";
    public static final String STOP_SERVICE = "STOP_SERVICE";

    public NetworkDiscoveryService() {
        mContext = getApplicationContext();
        mNsdHelper = new NsdHelper(mContext);
        mNetworkThreading = new NetworkThreading(mContext);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);

        bServerEnabled = mSharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);

        if (intent.getAction().equals(MainActivity.START_SERVICE)) {
            if (bServerEnabled) {
                // mNetworkThreading.start();
                mNsdHelper.registerService(NetworkTools.DEFAULT_SERVER_PORT);

            } else {
                boolean discoverying = true;

                mNsdHelper.discoverService();
                do {
                    NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
                    if (service != null) {
                        Log.d(TAG, "Connecting.");
                        discoverying = false;
                        buildForeGroundNotification("Connected to: "
                                + service.getHost().getHostAddress() + ":"
                                + service.getPort());
                        mNetworkThreading.connect(service.getHost()
                                .getHostAddress(), service.getPort());
                    } else {
                        Log.d(TAG, "No service to connect to!");
                    }
                } while (discoverying);

            }
        } else if (intent.getAction().equals(MainActivity.STOP_SERVICE)) {
            mNetworkThreading.stop();
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mNsdHelper.tearDown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void buildForeGroundNotification(String contentText) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        PendingIntent notificationIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Builder builder = new Notification.Builder(
                getApplicationContext());

        builder.setContentIntent(notificationIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis()).setAutoCancel(true)
                .setContentTitle("Alertification Service")
                .setContentText(contentText);

        Notification n = builder.getNotification();

        startForeground(NOTIFICATION_SERVICE_ID, n);
    }

}
