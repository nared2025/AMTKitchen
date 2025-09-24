package com.nared2544.AMTKitChenPro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

class LocationService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private lateinit var fusedClient: FusedLocationProviderClient
  private lateinit var locationRequest: LocationRequest

  override fun onCreate() {
    super.onCreate()
    Log.i("LocationService", "onCreate")
    fusedClient = LocationServices.getFusedLocationProviderClient(this)
    locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000)
      .setMinUpdateDistanceMeters(1000f)
      .build()
    startInForeground()
    startUpdates()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.i("LocationService", "onStartCommand")
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()
    Log.i("LocationService", "onDestroy")
    fusedClient.removeLocationUpdates(locationCallback)
  }

  private fun startInForeground() {
    val channelId = "amt_location_channel"
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(channelId, "Location Tracking", NotificationManager.IMPORTANCE_LOW)
      manager.createNotificationChannel(channel)
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val notification: Notification = NotificationCompat.Builder(this, channelId)
      .setContentTitle("AMT Kitchen Location")
      .setContentText("กำลังติดตามตำแหน่งของคุณ")
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(pendingIntent)
      .build()

    startForeground(1001, notification)
  }

  private fun startUpdates() {
    fusedClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
  }

  private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
      val location = result.lastLocation ?: return
      Log.i("LocationService", "Location: ${'$'}{location.latitude}, ${'$'}{location.longitude}")
      scope.launch {
        try {
          val url = URL("https://tracking.alliedmetals.com/trackgps/save_location.php")
          val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
          }
          val payload = "{" +
            "\"latitude\":${location.latitude}," +
            "\"longitude\":${location.longitude}," +
            "\"timestamp\":\"" + System.currentTimeMillis() + "\"}"
          conn.outputStream.use { it.write(payload.toByteArray()) }
          conn.inputStream.use { it.readBytes() }
          conn.disconnect()
        } catch (t: Throwable) {
          Log.e("LocationService", "Post error", t)
        }
      }
    }
  }
}


