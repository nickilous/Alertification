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
    private static boolean isRunning;

    private Context mContext;
    private NsdHelper mNsdHelper;
    private SharedPreferences mSharedPref;
    private NotificationManager mNotificationManager;
    private NetworkThreading mNetworkThreading;

    private boolean bServerEnabled;
    private static final int NOTIFICATION_SERVICE_ID = 11;

    // Service stop and start commands
    public static final String START_LISTEN_SERVICE = "com.nickilous.START_NETWORK_DISCOVERY_LISTEN_SERVICE";
    public static final String START_DISCOVERY_SERVICE = "com.nickilous.START_NETWORK_DISCOVERY_DISCOVERY_SERVICE";
    public static final String STOP_SERVICE = "com.nickilous.STOP_NETWORK_DISCOVERY_SERVICE";

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

        if (intent.getAction().equals(START_LISTEN_SERVICE)) {

            mNsdHelper.registerService(NetworkTools.DEFAULT_SERVER_PORT);

            buildForeGroundNotification("Network Service Started");

        } else if (intent.getAction().equals(START_DISCOVERY_SERVICE)) {
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

        } else if (intent.getAction().equals(STOP_SERVICE)) {
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

    @SuppressWarnings("deprecation")
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

    public static boolean isRunning() {
        return isRunning;

    }

    static public Intent getStartListenIntent() {
        Intent intent = new Intent(START_LISTEN_SERVICE);
        return intent;
    }

    static public Intent getStartDiscoveryIntent() {
        Intent intent = new Intent(START_DISCOVERY_SERVICE);
        return intent;
    }

    static public Intent getStopIntent() {
        Intent intent = new Intent(STOP_SERVICE);
        return intent;
    }

}
