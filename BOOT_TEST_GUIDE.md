# 🔍 คู่มือทดสอบการทำงานหลังรีสตาร์ทเครื่อง

## 📋 **ขั้นตอนการทดสอบ**

### **1. Build และติดตั้งแอพใหม่:**
```bash
eas build --platform android --profile apk
```

### **2. ตั้งค่าสำคัญก่อนทดสอบ:**
- Battery Optimization: `Don't optimize`
- Location Permission: `Allow all the time`
- Background Activity: `Allow`
- Auto-start: เปิดสำหรับยี่ห้อจีน

### **3. ทดสอบการรีสตาร์ท:**

#### **วิธีที่ 1: ทดสอบด้วย ADB (แนะนำ)**
```bash
# เชื่อมต่อมือถือกับคอมพิวเตอร์
adb devices

# รีสตาร์ทเครื่อง
adb reboot

# รอให้เครื่องบูตเสร็จ (ประมาณ 2-3 นาที)

# ตรวจสอบ logs
adb logcat | grep -E "(BootReceiver|LocationService|AlarmReceiver)"
```

#### **วิธีที่ 2: ทดสอบด้วยมือ**
1. รีสตาร์ทเครื่องด้วยตนเอง
2. รอให้เครื่องบูตเสร็จ
3. รอ 5-10 นาที
4. ตรวจสอบเซิร์ฟเวอร์ว่ามีตำแหน่งใหม่หรือไม่

### **4. Logs ที่ควรเห็นหลังรีสตาร์ท:**

```
BootReceiver: onReceive action=android.intent.action.BOOT_COMPLETED
BootReceiver: Starting LocationService after boot
BootReceiver: Wake lock acquired
BootReceiver: Started foreground service
BootReceiver: Wake lock released
LocationService: onCreate
LocationService: Android version: 33
LocationService: Device: samsung SM-G991B
LocationService: Wake lock acquired
LocationService: Service started successfully
LocationService: Foreground service started with notification
LocationService: Location updates started
LocationService: Heartbeat scheduled in 5 minutes
```

### **5. ตรวจสอบการทำงาน:**

#### **ตรวจสอบ Service Status:**
```bash
adb shell dumpsys activity services | grep LocationService
```

#### **ตรวจสอบ Wake Locks:**
```bash
adb shell dumpsys power | grep -i wake
```

#### **ตรวจสอบ Alarms:**
```bash
adb shell dumpsys alarm | grep AlarmReceiver
```

## 🚨 **การแก้ไขปัญหาหากไม่ทำงาน**

### **ปัญหาที่พบบ่อย:**

#### **1. BootReceiver ไม่ทำงาน:**
- ตรวจสอบว่าแอพมีสิทธิ์ `RECEIVE_BOOT_COMPLETED`
- บางยี่ห้อต้องเปิด Auto-start แยกต่างหาก

#### **2. Service ไม่เริ่ม:**
- ตรวจสอบ Battery Optimization
- ตรวจสอบ Background Activity

#### **3. Location ไม่ส่ง:**
- ตรวจสอบ Location Permission
- ตรวจสอบ Network Connection

### **การ Debug เพิ่มเติม:**

#### **ตรวจสอบ Broadcast Receivers:**
```bash
adb shell dumpsys package com.nared2544.AMTKitChenPro | grep -A 10 -B 10 receiver
```

#### **ตรวจสอบ Permissions:**
```bash
adb shell dumpsys package com.nared2544.AMTKitChenPro | grep permission
```

## 📱 **การตั้งค่าสำหรับยี่ห้อต่างๆ**

### **Samsung:**
- Settings > Apps > AMT Kitchen > Battery > Don't optimize
- Settings > Device care > Battery > App power management > AMT Kitchen > Don't optimize

### **Xiaomi/MIUI:**
- Settings > Apps > Manage apps > AMT Kitchen > Battery saver > No restrictions
- Settings > Apps > Manage apps > AMT Kitchen > Autostart > Enable

### **Huawei/EMUI:**
- Settings > Apps > AMT Kitchen > Battery > Launch manually > Enable
- Settings > Apps > AMT Kitchen > Battery > Background activity > Allow

### **OPPO/ColorOS:**
- Settings > Apps > AMT Kitchen > Battery > Don't optimize
- Settings > Apps > AMT Kitchen > Autostart > Enable

### **realme/realme UI:**
- Settings > Apps > AMT Kitchen > Battery > Don't optimize
- Settings > Apps > AMT Kitchen > Autostart > Enable

## ✅ **ผลลัพธ์ที่คาดหวัง**

หลังรีสตาร์ทเครื่อง:
1. ✅ BootReceiver จะทำงานทันทีหลังบูตเสร็จ
2. ✅ LocationService จะเริ่มทำงานอัตโนมัติ
3. ✅ จะมี notification แสดงสถานะการติดตาม
4. ✅ จะส่งตำแหน่งทุก 5 นาทีโดยอัตโนมัติ
5. ✅ ไม่ต้องเปิดแอพอีก

หากยังไม่ทำงาน แจ้งยี่ห้อ/รุ่นมือถือมาได้เลย จะเพิ่มการแก้ไขเฉพาะยี่ห้อ!
