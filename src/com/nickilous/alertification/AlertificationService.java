package com.nickilous.alertification;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.Toast;

public class AlertificationService extends Service {

    private SharedPreferences sharedPref;

    public AlertificationService() {

    }

    @Override
    public void onCreate() {

        if (sharedPref.getBoolean("wifi_direct_enabled", false)) {

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    public void setIsWifiP2pEnabled(boolean b) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
