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
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import android.os.PowerManager
import android.net.Uri
import android.provider.Settings

class LocationService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private lateinit var fusedClient: FusedLocationProviderClient
  private lateinit var locationRequest: LocationRequest
  private lateinit var prefs: SharedPreferences
  private lateinit var deviceId: String

  override fun onCreate() {
    super.onCreate()
    Log.i("LocationService", "onCreate")
    // Use device-protected storage if available so we can run before user unlocks after reboot
    val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val dp = createDeviceProtectedStorageContext()
      try {
        // Move prefs from credential-protected to device-protected once
        dp.moveSharedPreferencesFrom(this, "amt_prefs")
      } catch (_: Throwable) {}
      dp
    } else {
      this
    }
    prefs = storageContext.getSharedPreferences("amt_prefs", Context.MODE_PRIVATE)
    // ใช้ device_id ที่สร้างจาก React Native (AsyncStorage)
    deviceId = prefs.getString("unique_device_id", null) ?: run {
      // ถ้าไม่มี unique_device_id ให้สร้างจากข้อมูลอุปกรณ์
      val buildId = Build.ID ?: "unknown"
      val brand = Build.BRAND ?: "unknown"
      val model = Build.MODEL ?: "unknown"
      val deviceName = Build.DEVICE ?: "unknown"
      val osVersion = Build.VERSION.RELEASE ?: "unknown"
      val platform = "android"
      val timestamp = System.currentTimeMillis()
      val random1 = (1000000000..9999999999).random().toString()
      val random2 = (1000000000..9999999999).random().toString()
      val random3 = (1000000000..9999999999).random().toString()
      
      val uniqueId = "${brand}_${model}_${buildId}_${deviceName}_${osVersion}_${platform}_${timestamp}_${random1}_${random2}_${random3}"
      prefs.edit().putString("unique_device_id", uniqueId).apply()
      uniqueId
    }
    fusedClient = LocationServices.getFusedLocationProviderClient(this)
    locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 300_000) // 5 นาที
      .setMinUpdateDistanceMeters(0f) // ส่งตามเวลาแม้ไม่ขยับ
      .build()
    startInForeground()
    startUpdates()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.i("LocationService", "onStartCommand")
    scheduleHeartbeat()
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()
    Log.i("LocationService", "onDestroy")
    fusedClient.removeLocationUpdates(locationCallback)
  }

  private fun scheduleHeartbeat() {
    try {
      val manager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
      val intent = Intent(this, AlarmReceiver::class.java)
      val pending = android.app.PendingIntent.getBroadcast(
        this,
        1001,
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
      )
      val triggerAt = System.currentTimeMillis() + 300_000L // 5 นาที
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        manager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pending)
      } else {
        manager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pending)
      }
      Log.i("LocationService", "Heartbeat scheduled in 5 minutes")
    } catch (_: Throwable) {}
  }

  private fun startInForeground() {
    val channelId = "amt_location_channel"
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(channelId, "Location Tracking", NotificationManager.IMPORTANCE_MIN).apply {
        setSound(null, null)
        enableVibration(false)
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_SECRET
        description = "Background location tracking"
      }
      manager.createNotificationChannel(channel)
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val notification: Notification = NotificationCompat.Builder(this, channelId)
      .setContentTitle(null)
      .setContentText(null)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_MIN)
      .setSilent(true)
      .setOngoing(true)
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
          val tz = TimeZone.getTimeZone("Asia/Bangkok")
          val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { timeZone = tz }
          val timestamp = iso.format(System.currentTimeMillis())
          val payload = "{" +
            "\"device_id\":\"$deviceId\"," +
            "\"latitude\":${location.latitude}," +
            "\"longitude\":${location.longitude}," +
            "\"timestamp\":\"$timestamp\"}"
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


