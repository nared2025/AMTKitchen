package com.nared2544.AMTKitChenPro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Service สำหรับติดตามตำแหน่ง GPS และส่งข้อมูลไปยังเซิร์ฟเวอร์
 * ทำงานแบบ Background Service เพื่อติดตามตำแหน่งอย่างต่อเนื่อง
 */
public class LocationService extends Service {
    // Tag สำหรับ Log
    private static final String TAG = "LocationService";
    
    // ตั้งค่า Notification Channel และ ID
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1;
    
    // Google Play Services สำหรับการติดตามตำแหน่ง
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false; // สถานะการติดตามตำแหน่ง
    
    // URL ของเซิร์ฟเวอร์ที่จะส่งข้อมูลตำแหน่งไป
    private static final String SERVER_URL = "https://tracking.alliedmetals.com/trackgps/save_location.php";
    
    /**
     * เมธอดที่เรียกเมื่อ Service ถูกสร้าง
     * ใช้สำหรับการเตรียมการต่างๆ เช่น สร้าง Notification Channel และ LocationCallback
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService created");
        
        // เตรียม Google Location Services Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // สร้าง Notification Channel (สำหรับ Android 8.0+)
        createNotificationChannel();
        
        // ตั้งค่า Callback สำหรับรับข้อมูลตำแหน่ง
        setupLocationCallback();
    }
    
    /**
     * เมธอดที่เรียกเมื่อ Service เริ่มทำงาน
     * จะเริ่มทำงานเป็น Foreground Service และเริ่มติดตามตำแหน่ง
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService started");
        
        // เริ่มทำงานเป็น Foreground Service พร้อม Notification
        startForeground(NOTIFICATION_ID, createNotification());
        
        // เริ่มการติดตามตำแหน่ง
        startLocationTracking();
        
        // ส่งค่า START_STICKY เพื่อให้ระบบเริ่ม Service ใหม่หากถูกปิดไป
        return START_STICKY;
    }
    
    /**
     * เมธอดที่เรียกเมื่อ Service ถูกทำลาย
     * ทำการหยุดการติดตามตำแหน่งและทำความสะอาด
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LocationService destroyed");
        stopLocationTracking();
    }
    
    /**
     * เมธอดที่เรียกเมื่อ Task ถูกลบ (เช่น ผู้ใช้ swipe แอปออกจาก recent apps)
     * จะทำการเริ่ม Service ใหม่เพื่อให้การติดตามตำแหน่งต่อเนื่อง
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved called - restarting service");
        
        // สร้าง Intent สำหรับเริ่ม Service ใหม่
        Intent restartServiceIntent = new Intent(getApplicationContext(), LocationService.class);
        restartServiceIntent.setPackage(getPackageName());
        
        // เริ่ม Service ตามเวอร์ชัน Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartServiceIntent);
        } else {
            getApplicationContext().startService(restartServiceIntent);
        }
    }
    
    /**
     * เมธอดสำหรับ Binding (ไม่ใช้งานในกรณีนี้)
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * สร้าง Notification Channel สำหรับ Android 8.0 ขึ้นไป
     * จำเป็นต้องมี Channel ก่อนแสดง Notification
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW // ความสำคัญต่ำเพื่อไม่รบกวนผู้ใช้
            );
            channel.setDescription("Tracks your location in the background");
            channel.setShowBadge(false); // ไม่แสดง Badge บนไอคอนแอป
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * สร้าง Notification สำหรับแสดงใน Status Bar
     * แสดงให้ผู้ใช้ทราบว่า Service กำลังทำงานอยู่
     */
    private Notification createNotification() {
        // Intent สำหรับเมื่อผู้ใช้แตะ Notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AMT Kitchen Location")
            .setContentText("กำลังติดตามตำแหน่งของคุณ")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // ทำให้ Notification ไม่สามารถ swipe ลบได้
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
    
    /**
     * ตั้งค่า LocationCallback สำหรับรับข้อมูลตำแหน่งเมื่อมีการอัปเดต
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "Location result is null");
                    return;
                }
                
                // ดึงตำแหน่งล่าสุดที่ได้รับ
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "Location received: " + location.getLatitude() + ", " + location.getLongitude());
                    // ส่งข้อมูลตำแหน่งไปยังเซิร์ฟเวอร์
                    sendLocationToServer(location);
                }
            }
        };
    }
    
    /**
     * เริ่มการติดตามตำแหน่ง GPS
     * ตรวจสอบ Permission และตั้งค่าการอัปเดตตำแหน่ง
     */
    private void startLocationTracking() {
        if (isTracking) {
            Log.d(TAG, "Location tracking already started");
            return;
        }
        
        // ตรวจสอบ Permission สำหรับการเข้าถึงตำแหน่ง
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }
        
        // ตั้งค่าการขอข้อมูลตำแหน่ง
        LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000) // ความแม่นยำสูง, อัปเดตทุก 5 วินาที
            .setMinUpdateIntervalMillis(3000) // ขั้นต่ำ 3 วินาที
            .setMaxUpdateDelayMillis(10000) // สูงสุด 10 วินาที
            .setWaitForAccurateLocation(false) // ไม่รอตำแหน่งที่แม่นยำ
            .build();
        
        try {
            // เริ่มการขอข้อมูลตำแหน่ง
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // ใช้ Main Thread Looper
            );
            isTracking = true;
            Log.d(TAG, "Location tracking started successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while starting location tracking", e);
        }
    }
    
    /**
     * หยุดการติดตามตำแหน่ง
     */
    private void stopLocationTracking() {
        if (!isTracking) {
            return;
        }
        
        // ยกเลิกการขอข้อมูลตำแหน่ง
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        Log.d(TAG, "Location tracking stopped");
    }
    
    /**
     * ส่งข้อมูลตำแหน่งไปยังเซิร์ฟเวอร์
     * ใช้ HTTP POST method ส่งข้อมูลในรูปแบบ JSON
     * @param location ข้อมูลตำแหน่งที่ต้องการส่ง
     */
    private void sendLocationToServer(Location location) {
        // ใช้ Thread แยกเพื่อไม่ให้ทำงานใน Main Thread
        new Thread(() -> {
            try {
                // ดึง Device ID
                String deviceId = getDeviceIdentifier();
                
                // สร้าง Timestamp ในรูปแบบ UTC
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date thaiTime = new Date();
                String timestamp = sdf.format(thaiTime);
                
                // สร้างข้อมูล JSON
                JSONObject locationData = new JSONObject();
                locationData.put("device_id", deviceId);
                locationData.put("latitude", location.getLatitude());
                locationData.put("longitude", location.getLongitude());
                locationData.put("timestamp", timestamp);
                
                Log.d(TAG, "Sending location: " + locationData.toString());
                
                // เตรียม HTTP Connection
                URL url = new URL(SERVER_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true); // เปิดใช้งานการส่งข้อมูล
                connection.setConnectTimeout(10000); // Timeout 10 วินาที
                connection.setReadTimeout(10000);
                
                // ส่งข้อมูล JSON
                OutputStream os = connection.getOutputStream();
                os.write(locationData.toString().getBytes("UTF-8"));
                os.close();
                
                // ตรวจสอบ Response Code
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Server response code: " + responseCode);
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending location to server", e);
            }
        }).start();
    }
    
    /**
     * ดึง Device Identifier สำหรับใช้เป็น ID ของอุปกรณ์
     * ใช้ Build.FINGERPRINT เป็น Unique ID ของอุปกรณ์
     * @return Device ID ในรูปแบบ String
     */
    private String getDeviceIdentifier() {
        try {
            return Build.FINGERPRINT != null ? Build.FINGERPRINT : "unknown_device";
        } catch (Exception e) {
            Log.e(TAG, "Error getting device ID", e);
            return "unknown_device";
        }
    }
}