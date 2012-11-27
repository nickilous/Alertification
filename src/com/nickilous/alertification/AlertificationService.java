package com.nickilous.alertification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class AlertificationService extends Service {
    // Debugging
    private static final String TAG = "AlertificationService";
    private static final boolean D = true;
    private Binder mBinder = new LocalBinder();
    // Preference Settings
    private SharedPreferences sharedPref;
    private boolean clientEnabled = false;
    private boolean serverEnabled = false;

    private boolean serverShouldRun = false;
    private boolean clientShouldRun = false;

    // default ip
    public static String SERVERIP = "10.0.2.15";

    // designate a port
    public static final int SERVERPORT = 8080;

    private ServerSocket serverSocket;
    private static boolean isRunning = false;
    private static boolean isConnected = false;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    int mValue = 0; // Holds last value set by a client.
    private Socket clientSocket;
    private WifiServerThread wifiServerThread;
    private WifiClientThread wifiClientThread;

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

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        serverEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);

        if (intent.getAction().equals(MainActivity.START_SERVICE)) {
            if (serverEnabled) {
                Log.i(TAG, "Enabling Server");
                SERVERIP = getLocalIpAddress();
                serverShouldRun = true;
                wifiServerThread = new WifiServerThread();
                wifiServerThread.start();
            } else {
                Log.i(TAG, "Enabling Client");
                wifiClientThread = new WifiClientThread();
                clientShouldRun = true;
                wifiClientThread.start();

                isConnected = true;

            }
        } else if (intent.getAction().equals(MainActivity.STOP_SERVICE)) {
            serverShouldRun = false;
            stopSelf();
        } else if (intent.getAction().equals(
                "android.provider.Telephony.SMS_RECEIVED")
                && isConnected) {
            wifiClientThread.sendMessageToServer(handleSMSMessage(intent));

        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        try {
            // make sure you close the socket upon exiting
            serverSocket.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();

        Log.i(TAG, "Service Stopped.");
        isConnected = false;
        isRunning = false;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        AlertificationService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return AlertificationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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

    public class WifiServerThread extends Thread {

        @Override
        public void run() {
            Log.i(TAG, "Running WifiServerThread");
            try {
                if (SERVERIP != null) {

                    sendMessageToUI(MSG_SET_THREAD_STATUS, "Listening on IP: "
                            + SERVERIP);

                    serverSocket = new ServerSocket(SERVERPORT);
                    while (serverShouldRun) {
                        // listen for incoming clients
                        Socket client = serverSocket.accept();
                        sendMessageToUI(MSG_SET_THREAD_STATUS, "Connected.");

                        try {
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                            client.getInputStream()));
                            String line = null;
                            while ((line = in.readLine()) != null) {
                                Log.d("ServerActivity", line);

                                // do whatever you want to the front end
                                // this is where you can be creative

                            }
                            break;
                        } catch (Exception e) {
                            sendMessageToUI(MSG_SET_THREAD_STATUS,
                                    "Oops. Connection interrupted. Please reconnect your phones.");
                            e.printStackTrace();
                        }
                    }
                } else {
                    sendMessageToUI(MSG_SET_THREAD_STATUS,
                            "Couldn't detect internet connection.");

                }
            } catch (Exception e) {
                sendMessageToUI(MSG_SET_THREAD_STATUS, "Error");

                e.printStackTrace();
            }
        }

    }

    public class WifiClientThread extends Thread {

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);
                Log.d(TAG, "C: Connecting...");
                clientSocket = new Socket(serverAddr,
                        AlertificationService.SERVERPORT);
                isConnected = true;

            } catch (Exception e) {
                Log.e(TAG, "C: Error", e);
                isConnected = false;
            }
        }

        private void sendMessageToServer(String message) {
            Log.i(TAG, "<-----sendMessageToServer()----->");
            PrintWriter out;
            try {
                out = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(
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
