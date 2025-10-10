package com.nared2544.AMTKitChenPro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action
    val isBackup = intent?.getBooleanExtra("backup", false) ?: false
    val restartService = intent?.getBooleanExtra("restart_service", false) ?: false
    
    Log.i("AlarmReceiver", "Alarm fired - backup: $isBackup, restart: $restartService")

    // Acquire a short wake lock to ensure CPU is on while starting service
    var wakeLock: PowerManager.WakeLock? = null
    try {
      val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
      wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AMT:LocationWakeLock").apply {
        setReferenceCounted(false)
        acquire(60_000L) // hold up to 60s; service should release earlier by starting FG
      }
      Log.i("AlarmReceiver", "Wake lock acquired")
    } catch (e: Exception) {
      Log.e("AlarmReceiver", "Failed to acquire wake lock", e)
    }

    try {
      val serviceIntent = Intent(context, LocationService::class.java).apply {
        // Forward action flags to service so it can decide how to refresh
        putExtra("backup", isBackup)
        putExtra("restart_service", restartService)
        action = action
      }
      
      if (restartService) {
        Log.i("AlarmReceiver", "Restarting service due to restart alarm")
        // Stop existing service first
        context.stopService(serviceIntent)
        Thread.sleep(1000) // Wait 1 second
      }
      
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
      } else {
        context.startService(serviceIntent)
      }
      Log.i("AlarmReceiver", "LocationService started")
    } catch (e: Exception) {
      Log.e("AlarmReceiver", "Failed to start LocationService", e)
    } finally {
      try { 
        wakeLock?.release() 
        Log.i("AlarmReceiver", "Wake lock released")
      } catch (e: Exception) {
        Log.e("AlarmReceiver", "Failed to release wake lock", e)
      }
    }
  }
}


