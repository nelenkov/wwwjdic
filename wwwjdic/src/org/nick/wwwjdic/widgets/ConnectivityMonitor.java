package org.nick.wwwjdic.widgets;

import org.nick.wwwjdic.WwwjdicPreferences;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

public class ConnectivityMonitor extends BroadcastReceiver {

    private static final String TAG = ConnectivityMonitor.class.getSimpleName();

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            // only on IS04 2.2.2?
            if (extras == null) {
                return;
            }
            boolean noConnectivity = extras.getBoolean(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.d(TAG, "CONNECTIVITY_ACTION::noConnectivity : "
                    + noConnectivity);
            if (noConnectivity) {
                return;
            }

            NetworkInfo ni = extras.getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (ni != null && ni.isConnected()) {
                Log.d(TAG, ni.getTypeName() + " is connecting");
                if (WwwjdicPreferences.getLastKodUpdateError(context) != 0) {
                    Log.d(TAG,
                            "KOD widget is in error state, trying to update...");
                    context.startService(new Intent(context,
                            GetKanjiService.class));
                }
            }
        }
    }

    public static void toggle(Context context, boolean enable) {
        int flag = enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName receiver = new ComponentName(context,
                ConnectivityMonitor.class);

        String action = enable ? "Starting" : "Stopping";
        Log.d(TAG, action + " ConnectivityMonitor...");
        context.getPackageManager().setComponentEnabledSetting(receiver, flag,
                PackageManager.DONT_KILL_APP);
        Log.d(TAG, "done");
    }

    public static void start(Context context) {
        toggle(context, true);
    }

    public static void stop(Context context) {
        toggle(context, false);
    }

}
