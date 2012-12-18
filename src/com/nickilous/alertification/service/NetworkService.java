package com.nickilous.alertification.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nickilous.alertification.AlertificationPreferenceActivity;
import com.nickilous.alertification.MainActivity;
import com.nickilous.alertification.R;
import com.nickilous.alertification.TextMessage;
import com.nickilous.alertification.network.NetworkThreading;

public class NetworkService extends Service {
    // Debugging
    private static final String TAG = "AlertificationService";
    private static final boolean D = true;

    // Preference Settings
    private SharedPreferences mSharedPref;
    private boolean bServerEnabled = false;

    private static boolean isRunning = false;

    private NetworkThreading mNetworkThreading;
    private BroadcastReceiver mSmsReceiver;

    // Server Members
    private String mServerIP;
    private int mServerPort;

    private IntentFilter theFilter;
    private Context mContext;
    private NotificationManager mNotificationManager;
    private static final int NOTIFICATION_SERVICE_ID = 1;
    private static final int NOTIFICATION_TEXT_MESSAGE_ID = 2;

    // Static final String Identifiers
    public static final String START_SERVER_SERVICE = "com.nickilous.START_SERVER_SERVICE";
    public static final String START_CLIENT_SERVICE = "com.nickilous.START_CLIENT_SERVICE";
    public static final String STOP_SERVICE = "com.nickilous.STOP_SERVICE";
    public static final String SERVER_IP = "SERVER_IP";
    public static final String SERVER_PORT = "SERVER_PORT";

    @Override
    public void onCreate() {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mContext = getApplicationContext();
        isRunning = true;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNetworkThreading = new NetworkThreading(mContext);

        createAndRegisterBroadcastReceiver();

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);

        bServerEnabled = mSharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);

        if (intent.getAction().equals(START_SERVER_SERVICE)) {
            mNetworkThreading.start();
            buildForeGroundNotification("Server On");
        } else if (intent.getAction().equals(START_CLIENT_SERVICE)) {
            mServerIP = intent.getStringExtra(SERVER_IP);
            mServerPort = intent.getIntExtra(SERVER_PORT, 0);
            buildForeGroundNotification("Connected to: " + mServerIP + ":"
                    + mServerPort);
            mNetworkThreading.connect(mServerIP, mServerPort);
        } else if (intent.getAction().equals(STOP_SERVICE)) {
            mNetworkThreading.stop();
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.d(TAG, "Service Stopped.");
        // Do not forget to unregister the receiver!!!
        this.unregisterReceiver(this.mSmsReceiver);
        isRunning = false;
    }

    public static boolean isRunning() {
        return isRunning;

    }

    private void sendMessageToServer(TextMessage textMessage) {
        Log.d(TAG, "<-----sendMessageToServer()----->");

        mNetworkThreading.write(textMessage.toString().getBytes());

        Log.d(TAG, "message sent");

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

    public static Intent getStartServerIntent() {
        Intent intent = new Intent(START_SERVER_SERVICE);
        return intent;

    }

    public static Intent getStartClientIntent(String ipAddress,
            String portNumber) {
        Intent intent = new Intent(START_CLIENT_SERVICE);
        intent.putExtra(SERVER_IP, ipAddress);
        intent.putExtra(SERVER_PORT, Integer.parseInt(portNumber));
        return intent;
    }

    public static Intent getStopIntent() {
        Intent intent = new Intent(STOP_SERVICE);
        return intent;

    }
}
