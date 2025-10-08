# 🔧 คู่มือแก้ไขปัญหาการติดตามตำแหน่งหยุดทำงานเมื่อหน้าจอดับ

## 🚨 ปัญหาที่พบ
การติดตามตำแหน่งหยุดทำงานเมื่อหน้าจอดับ แม้ว่าจะตั้งค่าให้ติดตามทุก 5 นาทีแล้ว

## 🔍 สาเหตุหลัก
1. **Battery Optimization (Doze Mode)** - Android จะปิดการทำงานของแอปในพื้นหลังเพื่อประหยัดแบตเตอรี่
2. **การตั้งค่าแอป** - ผู้ใช้อาจไม่ได้ตั้งค่าให้แอปทำงานในพื้นหลังได้อย่างเต็มที่
3. **Permissions** - อาจขาดสิทธิ์บางอย่างที่จำเป็นสำหรับการทำงานในพื้นหลัง

## 🛠️ วิธีแก้ไขปัญหา

### 1. การตั้งค่า Battery Optimization (สำคัญที่สุด)

#### สำหรับ Android ทั่วไป:
```
Settings > Apps > AMT Kitchen > Battery > Don't optimize
```

#### สำหรับ Samsung:
```
Settings > Device care > Battery > App power management > AMT Kitchen > Don't optimize
```

#### สำหรับ Xiaomi/MIUI:
```
Settings > Apps > Manage apps > AMT Kitchen > Battery saver > No restrictions
```

#### สำหรับ Huawei/EMUI:
```
Settings > Apps > AMT Kitchen > Battery > Launch manually
```

### 2. การตั้งค่า Background Activity

```
Settings > Apps > AMT Kitchen > Battery > Background activity > Allow
```

### 3. การตั้งค่า Location Permissions

```
Settings > Apps > AMT Kitchen > Permissions > Location > Allow all the time
```

### 4. การตั้งค่า Auto-start (สำหรับบางยี่ห้อ)

#### Samsung:
```
Settings > Apps > AMT Kitchen > Battery > Allow background activity
```

#### Xiaomi/MIUI:
```
Settings > Apps > Manage apps > AMT Kitchen > Autostart > Enable
```

#### Huawei/EMUI:
```
Settings > Apps > AMT Kitchen > Battery > Launch manually > Enable
```

### 5. การตั้งค่าเพิ่มเติม

#### ปิดการจำกัดข้อมูลในพื้นหลัง:
```
Settings > Apps > AMT Kitchen > Mobile data > Background data > Allow
```

#### ปิดการจำกัด WiFi ในพื้นหลัง:
```
Settings > Apps > AMT Kitchen > WiFi > Background data > Allow
```

## 🔧 การแก้ไขผ่านโค้ด (สำหรับ Developer)

### 1. เพิ่มฟังก์ชันขอสิทธิ์ Battery Optimization

```javascript
import useLocation from './hooks/useLocation';

const MyComponent = () => {
  const { requestBatteryOptimizationExemption } = useLocation();

  const handleRequestBatteryOptimization = async () => {
    try {
      const result = await requestBatteryOptimizationExemption();
      console.log('Battery optimization exemption:', result);
    } catch (error) {
      console.error('Error requesting battery optimization:', error);
    }
  };

  return (
    <Button onPress={handleRequestBatteryOptimization}>
      ขอสิทธิ์ Battery Optimization
    </Button>
  );
};
```

### 2. ตรวจสอบสถานะการติดตาม

```javascript
const checkLocationStatus = async () => {
  const status = await checkBackgroundLocationStatus();
  console.log('Location tracking active:', status);
};
```

## 📱 การทดสอบ

### 1. ทดสอบการทำงานในพื้นหลัง:
1. เปิดแอปและเริ่มการติดตามตำแหน่ง
2. ปิดหน้าจอ (ไม่ปิดแอป)
3. รอ 5-10 นาที
4. เปิดแอปและตรวจสอบว่ามีการส่งตำแหน่งใหม่หรือไม่

### 2. ทดสอบหลังรีสตาร์ทเครื่อง:
1. รีสตาร์ทเครื่อง
2. รอให้เครื่องบูตเสร็จ
3. ตรวจสอบว่าแอปเริ่มการติดตามตำแหน่งอัตโนมัติหรือไม่

## 🚨 สิ่งที่ต้องระวัง

1. **แบตเตอรี่** - การติดตามตำแหน่งต่อเนื่องจะใช้แบตเตอรี่มากขึ้น
2. **ข้อมูลมือถือ** - การส่งข้อมูลตำแหน่งจะใช้ข้อมูลมือถือ
3. **ความเป็นส่วนตัว** - แอปจะติดตามตำแหน่งตลอดเวลา

## 📞 การติดต่อขอความช่วยเหลือ

หากยังมีปัญหา กรุณาติดต่อทีมพัฒนา พร้อมข้อมูล:
- ยี่ห้อและรุ่นมือถือ
- เวอร์ชัน Android
- ขั้นตอนที่ทำแล้ว
- ข้อความ error (ถ้ามี)

## 🔄 การอัปเดต

โค้ดได้รับการปรับปรุงให้:
- ขอสิทธิ์ Battery Optimization อัตโนมัติ
- ใช้ Foreground Service ที่แข็งแกร่งขึ้น
- มีระบบ Heartbeat เพื่อตรวจสอบการทำงาน
- รองรับการทำงานหลังรีสตาร์ทเครื่อง
