package com.nickilous.alertification.network;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

public class NsdHelper {
    // Debugging
    private static final String TAG = "NetworkDiscoveryHelper";
    private static final boolean D = true;

    // Context of Application
    private Context mContext;

    // Network Service Fields
    protected String mServiceName;
    protected NsdServiceInfo mService;
    private NsdManager mNsdManager;
    private RegistrationListener mRegistrationListener;
    private DiscoveryListener mDiscoveryListener;
    private ResolveListener mResolveListener;

    public static final String SERVICE_NAME = "AlertificationTextService";
    public static final String SERVICE_TYPE = "_http._tcp.";

    public NsdHelper(Context context) {

        mContext = context;
        mNsdManager = (NsdManager) mContext
                .getSystemService(Context.NSD_SERVICE);
        initializeNsd();
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();

        // mNsdManager.init(mContext.getMainLooper(), this);

    }

    /*
     * Note that this method is asynchronous, so any code that needs to run
     * after the service has been registered must go in the
     * onServiceRegistered() method.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD,
                mRegistrationListener);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void discoverService() {
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                mDiscoveryListener);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name. Android may have changed it in order
                // to
                // resolve a conflict, so update the name you initially
                // requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
            }

            public void onRegistrationFailed(NsdServiceInfo serviceInfo,
                    int errorCode) {
                // Registration failed! Put debugging code here to determine
                // why.
            }

            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered. This only happens when you
                // call
                // NsdManager.unregisterService() and pass in this listener.
            }

            public void onUnregistrationFailed(NsdServiceInfo serviceInfo,
                    int errorCode) {
                // Unregistration failed. Put debugging code here to determine
                // why.
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG,
                            "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains(SERVICE_NAME)) {
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            public void onResolveFailed(NsdServiceInfo serviceInfo,
                    int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;

            }
        };
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

    // NsdHelper's tearDown method
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void tearDown() {
        mNsdManager.unregisterService(mRegistrationListener);
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }
}
