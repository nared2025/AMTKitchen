package com.nared2544.AMTKitChenPro

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments

class LocationModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "LocationModule"
    }

    @ReactMethod
    fun startLocationTracking(promise: Promise) {
        try {
            Log.i("LocationModule", "Starting location tracking service")
            val serviceIntent = Intent(reactApplicationContext, LocationService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reactApplicationContext.startForegroundService(serviceIntent)
            } else {
                reactApplicationContext.startService(serviceIntent)
            }
            
            promise.resolve("Location tracking started successfully")
        } catch (e: Exception) {
            Log.e("LocationModule", "Error starting location tracking", e)
            promise.reject("START_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun stopLocationTracking(promise: Promise) {
        try {
            Log.i("LocationModule", "Stopping location tracking service")
            val serviceIntent = Intent(reactApplicationContext, LocationService::class.java)
            reactApplicationContext.stopService(serviceIntent)
            promise.resolve("Location tracking stopped successfully")
        } catch (e: Exception) {
            Log.e("LocationModule", "Error stopping location tracking", e)
            promise.reject("STOP_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun isLocationTrackingActive(promise: Promise) {
        try {
            val powerManager = reactApplicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(reactApplicationContext.packageName)
            } else {
                true
            }
            
            val result = Arguments.createMap()
            result.putBoolean("isActive", isIgnoringBatteryOptimizations)
            result.putBoolean("isIgnoringBatteryOptimizations", isIgnoringBatteryOptimizations)
            
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e("LocationModule", "Error checking location tracking status", e)
            promise.reject("STATUS_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun requestBatteryOptimizationExemption(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = reactApplicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                
                if (!powerManager.isIgnoringBatteryOptimizations(reactApplicationContext.packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${reactApplicationContext.packageName}")
                    }
                    
                    if (intent.resolveActivity(reactApplicationContext.packageManager) != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        reactApplicationContext.startActivity(intent)
                        promise.resolve("Battery optimization exemption requested")
                    } else {
                        promise.resolve("Battery optimization settings not available")
                    }
                } else {
                    promise.resolve("Already exempt from battery optimization")
                }
            } else {
                promise.resolve("Battery optimization not applicable for this Android version")
            }
        } catch (e: Exception) {
            Log.e("LocationModule", "Error requesting battery optimization exemption", e)
            promise.reject("BATTERY_OPTIMIZATION_ERROR", e.message, e)
        }
    }
}
