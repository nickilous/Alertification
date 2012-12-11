package com.nickilous.alertification;

import java.net.ServerSocket;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class AlertificationService extends Service {
    // Debugging
    private static final String TAG = "AlertificationService";
    private static final boolean D = true;

    // Preference Settings
    private SharedPreferences sharedPref;
    private boolean serverEnabled = false;

    private static final String SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    // designate a port
    public static final int SERVERPORT = 8080;

    private ServerSocket serverSocket;
    private static boolean isRunning = false;
    private static boolean isConnected = false;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    int mValue = 0; // Holds last value set by a client.
    private AlertificationThreading alertificationThreading;
    private BroadcastReceiver smsReceiver;

    // Server Members
    private String mServerIP;
    private int mServerPort;
    private NotificationManager mNotificationManager;
    private IntentFilter theFilter;
    private static final int NOTIFCATION_ID = 1;

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;
    static final int MSG_SET_THREAD_STATUS = 5;
    static final int MSG_SET_VALUE = 6;

    @Override
    public void onCreate() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        isRunning = true;
        alertificationThreading = new AlertificationThreading(
                getApplicationContext());

        mNotificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        theFilter = new IntentFilter();
        theFilter.addAction(SMS_ACTION);

        this.smsReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Do whatever you need it to do when it receives the broadcast
                Log.i(TAG, "SMS Received");
                if (!serverEnabled) {

                    Log.i(TAG, "Message sent to server");
                    sendMessageToServer(intent);
                } else {
                    Log.i(TAG, "Message Received from server");
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                            getApplicationContext());
                    alertBuilder.setTitle("SMS Received").setMessage(
                            handleSMSMessage(intent));

                    alertBuilder.show();

                }

            }
        };
        // Registers the receiver so that your service will listen for
        // broadcasts
        this.registerReceiver(this.smsReceiver, theFilter);

    }

    public void buildNotification(String contentText) {
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

        startForeground(NOTIFCATION_ID, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        serverEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);

        if (intent.getAction().equals(MainActivity.START_SERVICE)) {
            if (serverEnabled) {
                alertificationThreading.start();

            } else {
                mServerIP = intent.getStringExtra(MainActivity.SERVER_IP);
                mServerPort = intent.getIntExtra(MainActivity.SERVER_PORT, 0);
                buildNotification("Connected to: " + mServerIP + ":"
                        + mServerPort);
                alertificationThreading.connect(mServerIP, mServerPort);

            }
        } else if (intent.getAction().equals(MainActivity.STOP_SERVICE)) {
            alertificationThreading.stop();
            stopSelf();
        }
        return START_STICKY;
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.i(TAG, "Service Stopped.");
        // Do not forget to unregister the receiver!!!
        this.unregisterReceiver(this.smsReceiver);
        isRunning = false;
    }

    private String handleSMSMessage(Intent intent) {
        // Get the SMS map from Intent
        Bundle extras = intent.getExtras();

        String messages = "";
        Uri data = intent.getData();

        if (extras != null) {
            // Get received SMS array
            Object[] smsExtra = (Object[]) extras.get("pdus");

            for (Object element : smsExtra) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) element);

                String body = sms.getMessageBody().toString();
                String address = sms.getOriginatingAddress();

                messages += "SMS from " + address + " :\n";
                messages += body + "\n";

            }

        }
        return messages;
    }

    public static boolean isRunning() {
        return isRunning;

    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MSG_SET_VALUE:
                mValue = msg.arg1;
                for (int i = mClients.size() - 1; i >= 0; i--) {
                    try {
                        mClients.get(i).send(
                                Message.obtain(null, MSG_SET_VALUE, mValue, 0));
                    } catch (RemoteException e) {
                        // The client is dead. Remove it from the list;
                        // we are going through the list from back to front
                        // so this is safe to do inside the loop.
                        mClients.remove(i);
                    }
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    private void sendMessageToServer(Intent message) {
        Log.i(TAG, "<-----sendMessageToServer()----->");

        alertificationThreading.write(message.toUri(0).getBytes());

        Log.i(TAG, "message sent");

    }

}
