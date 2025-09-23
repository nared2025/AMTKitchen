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

/**
 * React Native Native Module สำหรับการจัดการ Location Service
 * เป็นตัวเชื่อมระหว่าง JavaScript (React Native) กับ Native Android Code
 * ใช้สำหรับเริ่ม หยุด และตรวจสอบสถานะการติดตาม GPS
 */
public class LocationModule extends ReactContextBaseJavaModule {
    // ชื่อของ Module ที่จะใช้เรียกจาก JavaScript
    private static final String MODULE_NAME = "LocationModule";
    
    // TAG สำหรับ Log ในการ Debug
    private static final String TAG = "LocationModule";
    
    /**
     * Constructor สำหรับสร้าง LocationModule
     * @param reactContext Context ของ React Native Application
     */
    public LocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    
    /**
     * ส่งคืนชื่อของ Module ที่จะใช้ใน JavaScript
     * จำเป็นต้องมีสำหรับ React Native Module
     * @return ชื่อของ Module
     */
    @NonNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }
    
    /**
     * Method สำหรับเริ่มการติดตาม GPS
     * เรียกได้จาก JavaScript ผ่าน NativeModules.LocationModule.startLocationTracking()
     * @param promise Promise object สำหรับส่งผลลัพธ์กลับไปยัง JavaScript
     */
    @ReactMethod
    public void startLocationTracking(Promise promise) {
        try {
            // ดึง Context ของแอปพลิเคชัน
            Context context = getReactApplicationContext();
            
            // สร้าง Intent สำหรับเริ่ม LocationService
            Intent serviceIntent = new Intent(context, LocationService.class);
            
            // ตรวจสอบเวอร์ชัน Android และเริ่ม Service ตามที่เหมาะสม
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Android 8.0+ ต้องใช้ startForegroundService
                context.startForegroundService(serviceIntent);
            } else {
                // Android เวอร์ชันเก่าใช้ startService
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "Location tracking started");
            
            // ส่งผลลัพธ์สำเร็จกลับไปยัง JavaScript
            promise.resolve("Location tracking started successfully");
            
        } catch (Exception e) {
            // หากเกิดข้อผิดพลาด บันทึก Log และส่ง Error กลับไปยัง JavaScript
            Log.e(TAG, "Error starting location tracking", e);
            promise.reject("START_ERROR", e.getMessage());
        }
    }
    
    /**
     * Method สำหรับหยุดการติดตาม GPS
     * เรียกได้จาก JavaScript ผ่าน NativeModules.LocationModule.stopLocationTracking()
     * @param promise Promise object สำหรับส่งผลลัพธ์กลับไปยัง JavaScript
     */
    @ReactMethod
    public void stopLocationTracking(Promise promise) {
        try {
            // ดึง Context ของแอปพลิเคชัน
            Context context = getReactApplicationContext();
            
            // สร้าง Intent สำหรับหยุด LocationService
            Intent serviceIntent = new Intent(context, LocationService.class);
            
            // หยุดการทำงานของ Service
            context.stopService(serviceIntent);
            
            Log.d(TAG, "Location tracking stopped");
            
            // ส่งผลลัพธ์สำเร็จกลับไปยัง JavaScript
            promise.resolve("Location tracking stopped successfully");
            
        } catch (Exception e) {
            // หากเกิดข้อผิดพลาด บันทึก Log และส่ง Error กลับไปยัง JavaScript
            Log.e(TAG, "Error stopping location tracking", e);
            promise.reject("STOP_ERROR", e.getMessage());
        }
    }
    
    /**
     * Method สำหรับตรวจสอบสถานะการติดตาม GPS
     * เรียกได้จาก JavaScript ผ่าน NativeModules.LocationModule.isLocationTrackingActive()
     * 
     * หมายเหตุ: ปัจจุบันเป็น Implementation แบบง่าย ส่งค่า true เสมอ
     * ในการใช้งานจริงควรตรวจสอบว่า Service กำลังทำงานจริงหรือไม่
     * 
     * @param promise Promise object สำหรับส่งผลลัพธ์กลับไปยัง JavaScript
     */
    @ReactMethod
    public void isLocationTrackingActive(Promise promise) {
        try {
            // สร้าง WritableMap สำหรับส่งข้อมูลกลับไปยัง JavaScript
            WritableMap result = Arguments.createMap();
            
            // ตั้งค่าสถานะ (ปัจจุบันเป็น true แบบคงที่)
            // TODO: ในอนาคตควรตรวจสอบสถานะจริงของ Service
            result.putBoolean("isActive", true);
            
            // ส่งผลลัพธ์กลับไปยัง JavaScript
            promise.resolve(result);
            
        } catch (Exception e) {
            // หากเกิดข้อผิดพลาด บันทึก Log และส่ง Error กลับไปยัง JavaScript
            Log.e(TAG, "Error checking location tracking status", e);
            promise.reject("STATUS_ERROR", e.getMessage());
        }
    }
}