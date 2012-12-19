package com.nickilous.alertification.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nickilous.alertification.AlertificationPreferenceActivity;
import com.nickilous.alertification.MainActivity;
import com.nickilous.alertification.R;
import com.nickilous.alertification.TextMessage;
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
    private IntentFilter theFilter;
    private BroadcastReceiver mSmsReceiver;
    private static final int NOTIFICATION_TEXT_MESSAGE_ID = 12;
    private static final int NOTIFICATION_SERVICE_ID = 11;

    // Service stop and start commands
    public static final String START_LISTEN_SERVICE = "com.nickilous.START_NETWORK_DISCOVERY_LISTEN_SERVICE";
    public static final String START_DISCOVERY_SERVICE = "com.nickilous.START_NETWORK_DISCOVERY_DISCOVERY_SERVICE";
    public static final String STOP_SERVICE = "com.nickilous.STOP_NETWORK_DISCOVERY_SERVICE";

    public NetworkDiscoveryService() {

    }

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        mNsdHelper = new NsdHelper(mContext);
        mNetworkThreading = new NetworkThreading(mContext);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createAndRegisterBroadcastReceiver();
        isRunning = true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);

        bServerEnabled = mSharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);

        if (intent.getAction().equals(START_LISTEN_SERVICE)) {

            mNsdHelper.registerService(NetworkTools.DEFAULT_SERVER_PORT);
            mNetworkThreading.start();

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

            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNsdHelper.tearDown();
        Log.d(TAG, "Service Stopped.");
        // Do not forget to unregister the receiver!!!
        this.unregisterReceiver(this.mSmsReceiver);
        isRunning = false;
        mNetworkThreading.stop();

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

    private void createAndRegisterBroadcastReceiver() {
        theFilter = new IntentFilter();
        theFilter.addAction(TextMessage.TEXT_MESSAGE_RECEIVED);
        theFilter.addAction(TextMessage.SMS_RECEIVED);

        this.mSmsReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Do whatever you need it to do when it receives the broadcast
                Log.d(TAG, "SMS Received");

                if (!bServerEnabled) {
                    Log.d(TAG, "Message sent to server");
                    TextMessage textMessage = new TextMessage(intent);
                    sendMessageToServer(textMessage);
                } else {
                    Log.d(TAG, "Message Received from server");
                    TextMessage textMessage = new TextMessage(
                            intent.getStringExtra("textmessage"));
                    buildSMSNotification(textMessage);
                }

            }
        };
        // Registers the receiver so that your service will listen for
        // broadcasts
        this.registerReceiver(this.mSmsReceiver, theFilter);
    }

    private void sendMessageToServer(TextMessage textMessage) {
        Log.d(TAG, "<-----sendMessageToServer()----->");

        mNetworkThreading.write(textMessage.toString().getBytes());

        Log.d(TAG, "message sent");

    }

    public void buildSMSNotification(TextMessage textMessage) {
        Notification.Builder builder = new Notification.Builder(
                getApplicationContext());

        builder.setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle("Text Message Received")
                .setContentText(
                        "Sender: " + textMessage.getSender() + " Message: "
                                + textMessage.getMessage());

        Notification n = builder.getNotification();
        mNotificationManager.notify(NOTIFICATION_TEXT_MESSAGE_ID, n);

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
