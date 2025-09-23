package com.nared2544.AMTKitChenPro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            
            Log.d(TAG, "Device boot completed, starting LocationService");
            
            // Start the location service
            Intent serviceIntent = new Intent(context, LocationService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}
