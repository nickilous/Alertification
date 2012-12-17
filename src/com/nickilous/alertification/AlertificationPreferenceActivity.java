package com.nickilous.alertification;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class AlertificationPreferenceActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {
    public static final String SERVER_ENABLED = "server_enabled";
    public static final String SERVER_IP_ADDRESS = "server_ip_address";

    private SharedPreferences sharedPref;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.alertification_preferences);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {

    }

}
