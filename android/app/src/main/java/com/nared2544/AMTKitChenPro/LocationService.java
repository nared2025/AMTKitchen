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

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false;
    
    // Server endpoint
    private static final String SERVER_URL = "https://tracking.alliedmetals.com/trackgps/save_location.php";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService created");
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        setupLocationCallback();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService started");
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Start location tracking
        startLocationTracking();
        
        return START_STICKY; // Service will be restarted if killed
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LocationService destroyed");
        stopLocationTracking();
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved called - restarting service");
        // Restart the service if the task is removed (e.g., user swipes app away)
        Intent restartServiceIntent = new Intent(getApplicationContext(), LocationService.class);
        restartServiceIntent.setPackage(getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartServiceIntent);
        } else {
            getApplicationContext().startService(restartServiceIntent);
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tracks your location in the background");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
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
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
    
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "Location result is null");
                    return;
                }
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "Location received: " + location.getLatitude() + ", " + location.getLongitude());
                    sendLocationToServer(location);
                }
            }
        };
    }
    
    private void startLocationTracking() {
        if (isTracking) {
            Log.d(TAG, "Location tracking already started");
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }
        
        LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5 seconds
            .setMinUpdateIntervalMillis(3000) // 3 seconds minimum
            .setMaxUpdateDelayMillis(10000) // 10 seconds maximum
            .setWaitForAccurateLocation(false)
            .build();
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
            isTracking = true;
            Log.d(TAG, "Location tracking started successfully");
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while starting location tracking", e);
        }
    }
    
    private void stopLocationTracking() {
        if (!isTracking) {
            return;
        }
        
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        Log.d(TAG, "Location tracking stopped");
    }
    
    private void sendLocationToServer(Location location) {
        new Thread(() -> {
            try {
                // Get device ID
                String deviceId = getDeviceIdentifier();
                
                // Create Thai time (UTC+7)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date thaiTime = new Date();
                String timestamp = sdf.format(thaiTime);
                
                // Create JSON data
                JSONObject locationData = new JSONObject();
                locationData.put("device_id", deviceId);
                locationData.put("latitude", location.getLatitude());
                locationData.put("longitude", location.getLongitude());
                locationData.put("timestamp", timestamp);
                
                Log.d(TAG, "Sending location: " + locationData.toString());
                
                // Send to server
                URL url = new URL(SERVER_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                OutputStream os = connection.getOutputStream();
                os.write(locationData.toString().getBytes("UTF-8"));
                os.close();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Server response code: " + responseCode);
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending location to server", e);
            }
        }).start();
    }
    
    private String getDeviceIdentifier() {
        try {
            return Build.FINGERPRINT != null ? Build.FINGERPRINT : "unknown_device";
        } catch (Exception e) {
            Log.e(TAG, "Error getting device ID", e);
            return "unknown_device";
        }
    }
}
