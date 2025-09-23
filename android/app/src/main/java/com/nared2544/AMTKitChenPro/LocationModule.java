package com.nared2544.AMTKitChenPro;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

public class LocationModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "LocationModule";
    private static final String TAG = "LocationModule";
    
    public LocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    
    @NonNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }
    
    @ReactMethod
    public void startLocationTracking(Promise promise) {
        try {
            Context context = getReactApplicationContext();
            Intent serviceIntent = new Intent(context, LocationService.class);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "Location tracking started");
            promise.resolve("Location tracking started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting location tracking", e);
            promise.reject("START_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void stopLocationTracking(Promise promise) {
        try {
            Context context = getReactApplicationContext();
            Intent serviceIntent = new Intent(context, LocationService.class);
            context.stopService(serviceIntent);
            
            Log.d(TAG, "Location tracking stopped");
            promise.resolve("Location tracking stopped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location tracking", e);
            promise.reject("STOP_ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void isLocationTrackingActive(Promise promise) {
        try {
            // This is a simple implementation
            // In a real app, you might want to check if the service is actually running
            WritableMap result = Arguments.createMap();
            result.putBoolean("isActive", true); // You can implement proper service checking here
            promise.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error checking location tracking status", e);
            promise.reject("STATUS_ERROR", e.getMessage());
        }
    }
}
