package com.nickilous.alertification;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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

    // default ip
    public static String SERVER_IP = "10.0.2.15";

    // designate a port
    public static final int SERVERPORT = 8080;

    private ServerSocket serverSocket;
    private static boolean isRunning = false;
    private static boolean isConnected = false;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    int mValue = 0; // Holds last value set by a client.
    private AlertificationThreading alertificationThreading;

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;
    static final int MSG_SET_THREAD_STATUS = 5;
    static final int MSG_SET_VALUE = 6;

    public AlertificationService() {

    }

    @Override
    public void onCreate() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        isRunning = true;
        alertificationThreading = new AlertificationThreading();

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
                alertificationThreading.connect();
            }
        } else if (intent.getAction().equals(MainActivity.STOP_SERVICE)) {
            stop();
            stopSelf();
        } else if (intent.getAction().equals(
                "android.provider.Telephony.SMS_RECEIVED")
                && isConnected) {
            sendMessageToServer(handleSMSMessage(intent));

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
        isRunning = false;
    }

    private String handleSMSMessage(Intent intent) {
        // Get the SMS map from Intent
        Bundle extras = intent.getExtras();

        String messages = "";

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

    private void sendMessageToUI(int messageType, String message) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                // Send data as a String
                Bundle b = new Bundle();
                b.putString("str1", message);
                Message msg = Message.obtain(null, messageType);
                msg.setData(b);
                mClients.get(i).send(msg);

            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going
                // through the list from back to front so this is safe to do
                // inside the loop.
                mClients.remove(i);
            }
        }
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

    private void sendMessageToServer(String message) {
        Log.i(TAG, "<-----sendMessageToServer()----->");
        PrintWriter out;
        try {

            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    clientSocket.getOutputStream())), true);
            // where you issue the commands
            Log.i(TAG, "Sending message: " + message);
            out.println(message);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.i(TAG, "message sent");

    }

    // gets the ip address of your phone's network
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                String networkName = intf.getName();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && networkName.equals("wlan0")) {
                        inetAddress = enumIpAddr.nextElement();
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

}
