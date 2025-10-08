package com.nared2544.AMTKitChenPro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
      val serviceIntent = Intent(context, LocationService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
      } else {
        context.startService(serviceIntent)
      }
    }
  }
}


