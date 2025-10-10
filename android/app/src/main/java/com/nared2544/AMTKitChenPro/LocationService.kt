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
import com.google.android.gms.tasks.Tasks
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
import java.io.IOException
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import android.os.PowerManager
import android.net.Uri
import android.provider.Settings
import android.app.AlarmManager
import android.os.SystemClock
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.concurrent.TimeUnit

class LocationService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private lateinit var fusedClient: FusedLocationProviderClient
  private lateinit var locationRequest: LocationRequest
  private lateinit var prefs: SharedPreferences
  private lateinit var deviceId: String
  private var wakeLock: PowerManager.WakeLock? = null
  private var isServiceRunning = false
  private var lastLocationTime = 0L
  private var alarmManager: AlarmManager? = null

  override fun onCreate() {
    super.onCreate()
    Log.i("LocationService", "onCreate")
    
    // Log system info for debugging
    Log.i("LocationService", "Android version: ${Build.VERSION.SDK_INT}")
    Log.i("LocationService", "Device: ${Build.BRAND} ${Build.MODEL}")
    
    // Initialize alarm manager
    alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    // Check if device is in Doze mode
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
      Log.i("LocationService", "Battery optimization ignored: $isIgnoringBatteryOptimizations")
      
      if (!isIgnoringBatteryOptimizations) {
        Log.w("LocationService", "App is not exempt from battery optimization - this may cause issues")
      }
    }
    
    // Acquire wake lock to prevent CPU from sleeping
    try {
      wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "AMT:LocationServiceWakeLock"
      ).apply {
        setReferenceCounted(false)
        acquire(10 * 60 * 1000L) // 10 minutes
      }
      Log.i("LocationService", "Wake lock acquired")
    } catch (e: Exception) {
      Log.e("LocationService", "Failed to acquire wake lock", e)
    }
    
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
      .setMinUpdateIntervalMillis(300_000) // บังคับอย่างน้อยทุก 5 นาที
      .setMinUpdateDistanceMeters(0f) // ส่งตามเวลาแม้ไม่ขยับ
      .setMaxUpdateDelayMillis(600_000) // สูงสุด 10 นาที
      .setWaitForAccurateLocation(false) // ไม่รอความแม่นยำสูงสุด
      .build()
    startInForeground()
    startUpdates()
    isServiceRunning = true
    Log.i("LocationService", "Service started successfully")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.i("LocationService", "onStartCommand")
    
    // Ensure service is running
    if (!isServiceRunning) {
      Log.i("LocationService", "Restarting service from onStartCommand")
      startUpdates()
      isServiceRunning = true
    }
    
    val action = intent?.action
    val isBackup = intent?.getBooleanExtra("backup", false) ?: false
    val restart = intent?.getBooleanExtra("restart_service", false) ?: false
    Log.i("LocationService", "Intent flags action=$action backup=$isBackup restart=$restart")

    // If asked to restart, re-request updates
    if (restart) {
      try {
        fusedClient.removeLocationUpdates(locationCallback)
      } catch (_: Throwable) {}
      startUpdates()
    }

    // If heartbeat or backup alarm fired, proactively do a one-shot location and send
    if (action == "com.nared2544.AMTKitChenPro.ACTION_HEARTBEAT" || isBackup) {
      scope.launch {
        try {
          requestOneShotLocationAndSend()
        } catch (e: Exception) {
          Log.w("LocationService", "One-shot location failed: ${e.message}")
        }
      }
    }

    // Schedule multiple alarms for redundancy
    scheduleHeartbeat()
    scheduleBackupAlarm()
    
    // Schedule service restart alarm
    scheduleServiceRestart()
    
    return START_STICKY
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    Log.i("LocationService", "onTaskRemoved - restarting service")
    val restartIntent = Intent(applicationContext, LocationService::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      applicationContext.startForegroundService(restartIntent)
    } else {
      applicationContext.startService(restartIntent)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()
    Log.i("LocationService", "onDestroy")
    isServiceRunning = false
    fusedClient.removeLocationUpdates(locationCallback)
    
    // Release wake lock
    try {
      wakeLock?.release()
      Log.i("LocationService", "Wake lock released")
    } catch (e: Exception) {
      Log.e("LocationService", "Failed to release wake lock", e)
    }
  }

  private fun scheduleHeartbeat() {
    try {
      val manager = alarmManager ?: return
      val intent = Intent(this, AlarmReceiver::class.java).apply {
        action = "com.nared2544.AMTKitChenPro.ACTION_HEARTBEAT"
      }
      val pending = android.app.PendingIntent.getBroadcast(
        this,
        1001,
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
      )
      val triggerAtElapsed = android.os.SystemClock.elapsedRealtime() + 300_000L // 5 นาที
      
      // Cancel existing alarm first
      manager.cancel(pending)
      
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        manager.setExactAndAllowWhileIdle(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pending)
      } else {
        manager.setExact(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pending)
      }
      Log.i("LocationService", "Heartbeat scheduled in 5 minutes (ELAPSED_REALTIME_WAKEUP)")
    } catch (e: Exception) {
      Log.e("LocationService", "Failed to schedule heartbeat", e)
    }
  }

  private fun scheduleBackupAlarm() {
    try {
      val manager = alarmManager ?: return
      val intent = Intent(this, AlarmReceiver::class.java).apply {
        action = "com.nared2544.AMTKitChenPro.ACTION_BACKUP"
        putExtra("backup", true)
      }
      val pending = android.app.PendingIntent.getBroadcast(
        this,
        1002,
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
      )
      val triggerAtElapsed = android.os.SystemClock.elapsedRealtime() + 600_000L // 10 นาที
      
      manager.cancel(pending)
      
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        manager.setExactAndAllowWhileIdle(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pending)
      } else {
        manager.setExact(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pending)
      }
      Log.i("LocationService", "Backup alarm scheduled in 10 minutes")
    } catch (e: Exception) {
      Log.e("LocationService", "Failed to schedule backup alarm", e)
    }
  }

  private fun scheduleServiceRestart() {
    try {
      val manager = alarmManager ?: return
      val intent = Intent(this, AlarmReceiver::class.java).apply {
        action = "com.nared2544.AMTKitChenPro.ACTION_RESTART_SERVICE"
        putExtra("restart_service", true)
      }
      val pending = android.app.PendingIntent.getBroadcast(
        this,
        1003,
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
      )
      val triggerAtElapsed = android.os.SystemClock.elapsedRealtime() + 1800_000L // 30 นาที
      
      manager.cancel(pending)
      
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        manager.setExactAndAllowWhileIdle(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pending)
      } else {
        manager.setExact(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pending)
      }
      Log.i("LocationService", "Service restart alarm scheduled in 30 minutes")
    } catch (e: Exception) {
      Log.e("LocationService", "Failed to schedule service restart alarm", e)
    }
  }

  private fun startInForeground() {
    val channelId = "amt_location_channel"
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(channelId, "Location Tracking", NotificationManager.IMPORTANCE_LOW).apply {
        setSound(null, null)
        enableVibration(false)
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_SECRET
        description = "Background location tracking"
        setBypassDnd(false)
        setLockscreenVisibility(Notification.VISIBILITY_SECRET)
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
      .setContentTitle("AMT Kitchen")
      .setContentText("กำลังติดตามตำแหน่ง")
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setSilent(true)
      .setOngoing(true)
      .setAutoCancel(false)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()

    startForeground(1001, notification)
    Log.i("LocationService", "Foreground service started with notification")
  }

  private fun startUpdates() {
    try {
      fusedClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
      Log.i("LocationService", "Location updates started")
    } catch (e: Exception) {
      Log.e("LocationService", "Failed to start location updates", e)
    }
  }

  private fun requestOneShotLocationAndSend() {
    try {
      val provider = fusedClient
      val task = provider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
      val current = try {
        Tasks.await(task, 10, TimeUnit.SECONDS)
      } catch (t: Throwable) { null }
      if (current != null) {
        Log.i("LocationService", "One-shot location: ${current.latitude}, ${current.longitude}")
        // Reuse sending logic by simulating a callback
        val fakeResult = LocationResult.create(listOf(current))
        locationCallback.onLocationResult(fakeResult)
      } else {
        Log.w("LocationService", "One-shot location returned null")
      }
    } catch (t: Throwable) {
      Log.w("LocationService", "One-shot location error", t)
    }
  }

  private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
      val location = result.lastLocation ?: return
      lastLocationTime = System.currentTimeMillis()
      Log.i("LocationService", "Location: ${location.latitude}, ${location.longitude}")
      
      scope.launch {
        try {
          // Check connectivity
          if (!isNetworkAvailable()) {
            Log.w("LocationService", "No active network; will retry later")
            scheduleHeartbeat()
            return@launch
          }

          val tz = TimeZone.getTimeZone("Asia/Bangkok")
          val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { timeZone = tz }
          val timestamp = iso.format(System.currentTimeMillis())
          val payload = "{" +
            "\"device_id\":\"$deviceId\"," +
            "\"latitude\":${location.latitude}," +
            "\"longitude\":${location.longitude}," +
            "\"timestamp\":\"$timestamp\"}"

          val url = URL("https://tracking.alliedmetals.com/trackgps/save_location.php")

          var attempt = 1
          val maxAttempts = 3
          var sent = false
          var lastError: Throwable? = null

          while (attempt <= maxAttempts && !sent) {
            try {
              val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10_000
                readTimeout = 10_000
              }
              conn.outputStream.use { it.write(payload.toByteArray()) }
              val code = conn.responseCode
              if (code in 200..299) {
                conn.inputStream.use { it.readBytes() }
                sent = true
                Log.i("LocationService", "Location sent successfully (attempt $attempt)")
              } else {
                val errorText = try { conn.errorStream?.readBytes()?.decodeToString() } catch (_: Throwable) { null }
                Log.w("LocationService", "Server responded with $code ${errorText ?: ""}")
              }
              conn.disconnect()
            } catch (e: IOException) {
              lastError = e
              Log.w("LocationService", "HTTP attempt $attempt failed: ${e.message}")
            }

            if (!sent) {
              attempt += 1
              if (attempt <= maxAttempts) {
                try { Thread.sleep(2000L * attempt) } catch (_: InterruptedException) {}
              }
            }
          }

          if (!sent) {
            Log.e("LocationService", "Failed to send location after $maxAttempts attempts", lastError)
            scheduleHeartbeat()
            return@launch
          }

          // Reschedule alarms after successful location update
          scheduleHeartbeat()
          scheduleBackupAlarm()
          
        } catch (t: Throwable) {
          Log.e("LocationService", "Post error", t)
          scheduleHeartbeat()
        }
      }
    }
    
    override fun onLocationAvailability(availability: LocationAvailability) {
      Log.i("LocationService", "Location availability: ${availability.isLocationAvailable}")
      if (!availability.isLocationAvailable) {
        Log.w("LocationService", "Location not available - rescheduling alarms")
        scheduleHeartbeat()
        scheduleBackupAlarm()
      }
    }
  }
}

private fun Context.isNetworkAvailable(): Boolean {
  val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val network = cm.activeNetwork ?: return false
  val caps = cm.getNetworkCapabilities(network) ?: return false
  val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  return hasInternet && validated
}


