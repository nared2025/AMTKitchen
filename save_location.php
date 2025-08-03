<?php
header("Content-Type: application/json; charset=utf-8");
// รับข้อมูล JSON ที่ส่งมาจากแอพมือถือ (ส่งมาทาง HTTP POST แบบ  JSON)
// ใช้ file_get_contents("php://input") เพื่ออ่านเนื้อหา JSON ดิบที่ส่งมา
// แล้วใช้ JSON_decode() เพื่อแปลงเป็น array ใน php
$raw = file_get_contents("php://input");
file_put_contents("log.txt", "RAW: " . $raw . PHP_EOL, FILE_APPEND);

if (empty($raw)) {
    file_put_contents("log.txt", "\u26a0\ufe0f BODY ว่างเปล่า ไม่รับข้อมูล\n", FILE_APPEND);
    echo json_encode(["ERROR: EMPTY BODY"]);
    exit;
}

$data = json_decode($raw, true);
file_put_contents("log.txt", "JSON: " . json_encode($data) . PHP_EOL, FILE_APPEND);

if (!$data || !isset($data['device_id'], $data['latitude'], $data['longitude'], $data['timestamp'])) {
    file_put_contents("log.txt", "⚠️ ข้อมูลไม่ครบหรือ decode ไม่สำเร็จ: " . json_encode($data) . PHP_EOL, FILE_APPEND);
    echo json_encode(["ERROR"]);
    exit;
}

// แยกค่าที่ได้จาก JSON ออกมาเก็บไว้ในตัวแปร
$device_id = $data['device_id'];  // รหัสของอุปกรณ์ เช่น  UUID หรือ ชื่อเครื่อง
$lat = $data['latitude'];         // ค่า latitude จาก GPS
$lng = $data['longitude'];        // ค่า longitude จาก GPS
$time = $data['timestamp'];       // เวลาที่ตำแหน่งถูกส่งมาจากเครื่อง
$time = date("Y-m-d H:i:s", strtotime($data['timestamp']));

// เชื่อมต่อกับฐานข้อมูล SQL
$conn = new mysqli("localhost", "amt","P@ssw0rd!amt","gps_db");

// ตรวจสอบว่าเชื่อมต่อสำเร็จหรือไม่ ถ้าไม่ได้ให้หยุดโปรแกรมและแสดงข้อความผิดพลาด
if ($conn->connect_error) {
    die("เชื่อมต่อไม่สำเร็จ: " . $conn->connect_error);
}

// เตรียมคำสั่ง SQL แบบใช้ Prepared Statement เพื่อป้องกัน SQL Injection
$sql = "INSERT INTO location_logs (device_id, latitude, longitude, timestamp) VALUES ( ?, ?, ?, ?)";

// เตรียมคำสั่งในฐานข้อมูล
$stmt = $conn->prepare($sql);

// ผูกค่าพารามิเตอร์เข้ากับคำสั่ง SQL
$stmt->bind_param("sdds", $device_id, $lat, $lng, $time);

// รันคำสั่ง SQL เพื่อบันทึกข้อมูลลงในฐานข้อมูล
if (!$stmt->execute()) {
    echo " SQL ERROR:" . $stmt->error;
} else {
    // แสดงผลลัพธ์กลับไปยังแอพ ถ้าแอพต้องการรู้ว่าทำงานสำเร็จไหม
    echo "OK";
}

// ปิดการเชื่อมต่อ
$stmt->close();
$conn->close();
?>