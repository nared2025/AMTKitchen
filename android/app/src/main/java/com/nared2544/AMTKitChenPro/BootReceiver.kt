package com.nared2544.AMTKitChenPro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    Log.i("BootReceiver", "onReceive action=$action")
    
    if (
      action == Intent.ACTION_BOOT_COMPLETED ||
      (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && action == Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
      action == Intent.ACTION_MY_PACKAGE_REPLACED
    ) {
      Log.i("BootReceiver", "Starting LocationService after boot")
      
      // Acquire wake lock to ensure service starts properly
      var wakeLock: PowerManager.WakeLock? = null
      try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
          PowerManager.PARTIAL_WAKE_LOCK,
          "AMT:BootReceiverWakeLock"
        ).apply {
          setReferenceCounted(false)
          acquire(30_000L) // 30 seconds
        }
        Log.i("BootReceiver", "Wake lock acquired")
      } catch (e: Exception) {
        Log.e("BootReceiver", "Failed to acquire wake lock", e)
      }
      
      try {
        val serviceIntent = Intent(context, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(serviceIntent)
          Log.i("BootReceiver", "Started foreground service")
        } else {
          context.startService(serviceIntent)
          Log.i("BootReceiver", "Started service")
        }
      } catch (e: Exception) {
        Log.e("BootReceiver", "Failed to start LocationService", e)
      } finally {
        try {
          wakeLock?.release()
          Log.i("BootReceiver", "Wake lock released")
        } catch (e: Exception) {
          Log.e("BootReceiver", "Failed to release wake lock", e)
        }
      }
    } else {
      Log.w("BootReceiver", "Unhandled action: $action")
    }
  }
}


