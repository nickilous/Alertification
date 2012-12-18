package com.nickilous.alertification;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class AlertificationPreferenceActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {
    public static final String SERVER_ENABLED = "server_enabled";
    public static final String NETWORK_DISCOVERY_ENABLED = "network_discovery";
    public static final String SERVER_IP_ADDRESS = "server_ip_address";

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
