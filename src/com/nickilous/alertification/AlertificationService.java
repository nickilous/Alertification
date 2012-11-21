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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlertificationService extends Service {
    // Debugging
    private static final String TAG = "AlertificationService";
    private static final boolean D = true;
    // Preference Settings
    private SharedPreferences sharedPref;
    private boolean serverEnabled;

    // default ip
    public static String SERVERIP = "10.0.2.15";

    // designate a port
    public static final int SERVERPORT = 8080;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServerSocket serverSocket;
    private static boolean isRunning = false;
    private static boolean isConnected = false;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    int mValue = 0; // Holds last value set by a client.

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
        Log.i("MyService", "Received start id " + startId + ": " + intent);

        serverEnabled = sharedPref.getBoolean(
                AlertificationPreferenceActivity.SERVER_ENABLED, false);

        if (serverEnabled) {
            SERVERIP = getLocalIpAddress();
            Thread wifiServerThread = new Thread(new WifiServerThread());
            wifiServerThread.start();
        } else {
            Thread wifiClientThread = new Thread(new WifiClientThread());
            wifiClientThread.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        try {
            // make sure you close the socket upon exiting
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();

        Log.i("MyService", "Service Stopped.");
        isRunning = false;
    }

    public void setIsWifiP2pEnabled(boolean b) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
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

    private void sendMessageToUI(int messageType, String valuetosend) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                // Send data as a String
                Bundle b = new Bundle();
                b.putString("str1", valuetosend);
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

    public static boolean isRunning() {
        return isRunning;
    }

    public class WifiServerThread implements Runnable {

        public void run() {
            Log.i(TAG, "Running WifiServerThread");
            try {
                if (SERVERIP != null) {

                    sendMessageToUI(MSG_SET_THREAD_STATUS, "Listening on IP: "
                            + SERVERIP);

                    serverSocket = new ServerSocket(SERVERPORT);
                    while (true) {
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

    public class WifiClientThread implements Runnable {

        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
                Log.d(TAG, "C: Connecting...");
                Socket socket = new Socket(serverAddr,
                        AlertificationService.SERVERPORT);
                isConnected = true;
                while (isConnected) {
                    try {
                        Log.d(TAG, "C: Sending command.");
                        PrintWriter out = new PrintWriter(
                                new BufferedWriter(new OutputStreamWriter(
                                        socket.getOutputStream())), true);
                        // where you issue the commands
                        out.println("Hey Server!");
                        Log.d(TAG, "C: Sent.");
                    } catch (Exception e) {
                        Log.e(TAG, "S: Error", e);
                    }
                }
                socket.close();
                Log.d(TAG, "C: Closed.");
            } catch (Exception e) {
                Log.e(TAG, "C: Error", e);
                isConnected = false;
            }
        }
    }

    // gets the ip address of your phone's network
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return null;
    }

}
