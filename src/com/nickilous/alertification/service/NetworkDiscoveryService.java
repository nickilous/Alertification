package com.nickilous.alertification.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.nickilous.alertification.network.NsdHelper;

public class NetworkDiscoveryService extends Service {
    // Debugging
    private static final String TAG = "NetworkDiscoveryService";
    private static final boolean D = true;

    private Context mContext;
    private NsdHelper mNsdHelper;

    // Service stop and start commands
    public static final String START_SERVICE = "START_SERVICE";
    public static final String STOP_SERVICE = "STOP_SERVICE";

    public NetworkDiscoveryService() {
        mContext = getApplicationContext();
        NsdHelper mNsdHelper = new NsdHelper(mContext);
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

}
