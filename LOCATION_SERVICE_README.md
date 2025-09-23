# AMT Kitchen Location Service

## ภาพรวม
ระบบติดตามตำแหน่ง GPS ที่ใช้ React Native + Java Foreground Service สำหรับ Android เพื่อให้สามารถติดตามตำแหน่งได้อย่างต่อเนื่องแม้ว่าแอปจะถูกปิดหรืออุปกรณ์ถูกรีสตาร์ท

## คุณสมบัติ
✅ ทำงานได้ตอนเปิดแอพ (foreground)  
✅ ทำงานได้ตอนปิดจอ (background)  
✅ ทำงานต่อแม้ ออกจากแอพ (swipe ออกจาก recent apps)  
✅ ทำงานต่อแม้ รีสตาร์ทเครื่อง (auto-start หลัง boot เสร็จ)  

## ไฟล์ที่สร้างใหม่

### Java Files
- `android/app/src/main/java/com/nared2544/AMTKitChenPro/LocationService.java` - Foreground Service หลัก
- `android/app/src/main/java/com/nared2544/AMTKitChenPro/BootReceiver.java` - Auto-start หลังรีสตาร์ท
- `android/app/src/main/java/com/nared2544/AMTKitChenPro/LocationModule.java` - React Native Bridge
- `android/app/src/main/java/com/nared2544/AMTKitChenPro/LocationPackage.java` - Package Registration

### JavaScript Files
- `LocationModule.js` - React Native Module Wrapper

### ไฟล์ที่แก้ไข
- `hooks/useLocation.js` - อัปเดตให้ใช้ Native Service
- `android/app/src/main/AndroidManifest.xml` - เพิ่ม permissions และ services
- `android/app/build.gradle` - เพิ่ม Google Play Services dependencies
- `android/app/src/main/java/com/nared2544/AMTKitChenPro/MainApplication.kt` - Register package

## การติดตั้ง

1. **ติดตั้ง Dependencies**
   ```bash
   npm install
   ```

2. **Build และ Run**
   ```bash
   npx expo run:android
   ```

## การใช้งาน

```javascript
import useLocation from './hooks/useLocation';

const MyComponent = () => {
  const {
    latitude,
    longitude,
    errorMsg,
    isBackgroundLocationActive,
    startBackgroundLocation,
    stopBackgroundLocation,
    checkBackgroundLocationStatus
  } = useLocation();

  // เริ่มการติดตามตำแหน่ง
  const handleStartTracking = async () => {
    const success = await startBackgroundLocation();
    if (success) {
      console.log('Location tracking started');
    }
  };

  // หยุดการติดตามตำแหน่ง
  const handleStopTracking = async () => {
    await stopBackgroundLocation();
    console.log('Location tracking stopped');
  };

  return (
    // Your UI components
  );
};
```

## Permissions ที่จำเป็น

### Android Manifest
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

## การตั้งค่าเพิ่มเติม

### 1. Battery Optimization
แนะนำให้ผู้ใช้ปิดการเพิ่มประสิทธิภาพแบตเตอรี่สำหรับแอป:
- Settings > Apps > AMT Kitchen > Battery > Don't optimize

### 2. Background App Refresh
ตรวจสอบให้แน่ใจว่าแอปสามารถทำงานในพื้นหลังได้:
- Settings > Apps > AMT Kitchen > Battery > Background activity > Allow

### 3. Location Permissions
ตรวจสอบให้แน่ใจว่าผู้ใช้ให้สิทธิ์การเข้าถึงตำแหน่ง:
- Settings > Apps > AMT Kitchen > Permissions > Location > Allow all the time

## การ Debug

### ดู Logs
```bash
npx expo run:android
# หรือ
adb logcat | grep -E "(LocationService|LocationModule|AMTKitchen)"
```

### ตรวจสอบ Service Status
```javascript
const status = await checkBackgroundLocationStatus();
console.log('Location tracking active:', status);
```

## ข้อจำกัด

1. **Android Only**: Native Service ทำงานได้เฉพาะ Android
2. **iOS Fallback**: ใช้ Expo Location เป็น fallback สำหรับ iOS
3. **Battery Usage**: การติดตามตำแหน่งต่อเนื่องจะใช้แบตเตอรี่มากขึ้น
4. **Permissions**: ต้องขอสิทธิ์จากผู้ใช้ก่อนใช้งาน

## การแก้ไขปัญหา

### Service ไม่เริ่มทำงาน
1. ตรวจสอบ permissions
2. ตรวจสอบ battery optimization settings
3. ดู logs ใน Android Studio หรือ adb logcat

### ตำแหน่งไม่ถูกส่งไปยัง server
1. ตรวจสอบ network connection
2. ตรวจสอบ server endpoint
3. ดู logs ใน LocationService.java

### Service หยุดทำงานหลังรีสตาร์ท
1. ตรวจสอบ BootReceiver registration
2. ตรวจสอบ RECEIVE_BOOT_COMPLETED permission
3. ดู logs ใน BootReceiver.java
