package com.nared2544.AMTKitChenPro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    // TAG สำหรับใช้ในการ Log เพื่อง่ายต่อการติดตามและ Debug
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
         // ตรวจสอบว่า Intent ที่ได้รับเป็น Intent ใดใน 3 กรณี:
        // 1. ACTION_BOOT_COMPLETED - เมื่อระบบเปิดเครื่องเสร็จสิ้น
        // 2. ACTION_MY_PACKAGE_REPLACED - เมื่อแอปพลิเคชันของเราถูกอัปเดต
        // 3. ACTION_PACKAGE_REPLACED - เมื่อแพ็กเกจถูกแทนที่ (อัปเดต)
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {

            // บันทึก Log เพื่อติดตามการทำงาน
            Log.d(TAG, "Device boot completed, starting LocationService");
            
            // Start the location service
            Intent serviceIntent = new Intent(context, LocationService.class);
            // เริ่มต้น Service แบบ Foreground Service
            // ใช้ startForegroundService เพื่อให้ Service สามารถทำงานได้แม้แอปไม่อยู่ใน Foreground
            // (จำเป็นสำหรับ Android 8.0+ เมื่อต้องการเริ่ม Service จาก Background)
            context.startForegroundService(serviceIntent);
        }
    }
}
