package com.nickilous.alertification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);;
    // default ip
    public static String SERVERIP = "10.0.2.15";

    // designate a port
    public static final int SERVERPORT = 8080;

    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target
                                                                       // we
                                                                       // publish
                                                                       // for
                                                                       // clients
                                                                       // to
                                                                       // send
                                                                       // messages
                                                                       // to
                                                                       // IncomingHandler.

    private ServerSocket serverSocket;
    private static boolean isRunning = false;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track
                                                                // of all
                                                                // current
                                                                // registered
                                                                // clients.
    int mValue = 0; // Holds last value set by a client.
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;

    public AlertificationService() {

    }

    @Override
    public void onCreate() {

        if (sharedPref.getBoolean("wifi_direct_enabled", false)) {

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyService", "Received start id " + startId + ": " + intent);
        SERVERIP = getLocalIpAddress();

        Thread fst = new Thread(new WifiServerThread());
        fst.start();
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

    private void sendMessageToUI(int intvaluetosend) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                // Send data as an Integer
                mClients.get(i).send(
                        Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend,
                                0));

                // Send data as a String
                Bundle b = new Bundle();
                b.putString("str1", "ab" + intvaluetosend + "cd");
                Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
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
            try {
                if (SERVERIP != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            serverStatus
                                    .setText("Listening on IP: " + SERVERIP);
                        }
                    });
                    serverSocket = new ServerSocket(SERVERPORT);
                    while (true) {
                        // listen for incoming clients
                        Socket client = serverSocket.accept();
                        handler.post(new Runnable() {
                            public void run() {
                                serverStatus.setText("Connected.");
                            }
                        });

                        try {
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                            client.getInputStream()));
                            String line = null;
                            while ((line = in.readLine()) != null) {
                                Log.d("ServerActivity", line);
                                handler.post(new Runnable() {
                                    public void run() {
                                        // do whatever you want to the front end
                                        // this is where you can be creative
                                    }
                                });
                            }
                            break;
                        } catch (Exception e) {
                            handler.post(new Runnable() {
                                public void run() {
                                    serverStatus
                                            .setText("Oops. Connection interrupted. Please reconnect your phones.");
                                }
                            });
                            e.printStackTrace();
                        }
                    }
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            serverStatus
                                    .setText("Couldn't detect internet connection.");
                        }
                    });
                }
            } catch (Exception e) {
                handler.post(new Runnable() {
                    public void run() {
                        serverStatus.setText("Error");
                    }
                });
                e.printStackTrace();
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
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return null;
    }

}
