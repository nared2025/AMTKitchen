<?php
// กำหนด header ให้ตอบกลับเป็น JSON และตั้งค่ารหัสตัวอักษรเป็น UTF-8
header('Content-Type: application/json; charset=utf-8');
// ปิดการแสดง error บนหน้าจอ (แต่ยังสามารถ log ได้)
error_reporting(0);

// รับข้อมูล JSON ที่ส่งมาทาง HTTP POST
$body = file_get_contents('php://input');
// แปลงข้อมูล JSON เป็น array
$data = json_decode($body, true);

// ถ้าไม่ได้รับข้อมูลหรือ decode ไม่สำเร็จ ให้ตอบกลับ error
if (!$data) {
    echo json_encode(["error" => "EMPTY BODY"]);
    exit;
}

// ดึงค่าต่าง ๆ จาก array ที่ได้จาก JSON
$device_id = $data['device_id'] ?? '';
$latitude = $data['latitude'] ?? '';
$longitude = $data['longitude'] ?? '';
$timestamp = $data['timestamp'] ?? '';

// บันทึก log ข้อมูลที่รับเข้ามา (raw และ json) ลงไฟล์ log.txt
file_put_contents("log.txt", "RAW: " . $body . PHP_EOL, FILE_APPEND);
file_put_contents("log.txt", "JSON: " . json_encode($data) . PHP_EOL, FILE_APPEND);

// ตรวจสอบว่าข้อมูลที่จำเป็นครบหรือไม่ ถ้าไม่ครบให้ log และตอบกลับ error
if (!$data || !isset($data['device_id'], $data['latitude'], $data['longitude'], $data['timestamp'])) {
    file_put_contents("log.txt", "⚠️ ข้อมูลไม่ครบหรือ decode ไม่สำเร็จ: " . json_encode($data) . PHP_EOL, FILE_APPEND);
    echo json_encode(["ERROR"]);
    exit;
}

// แปลงค่าที่ได้จาก JSON ให้อยู่ในรูปแบบที่เหมาะสม
$device_id = $data['device_id'];  // รหัสอุปกรณ์ เช่น UUID หรือชื่อเครื่อง
$lat = isset($data['latitude']) ? (float)$data['latitude'] : null; // แปลง latitude เป็น float
$lng = isset($data['longitude']) ? (float)$data['longitude'] : null; // แปลง longitude เป็น float
$time = isset($data['timestamp']) ? strtotime($data['timestamp']) : false; // แปลง timestamp เป็น unix timestamp
if ($time === false) {
    echo json_encode(["error" => "Invalid timestamp"]);
    exit;
}

// ตั้ง timezone เป็น Asia/Bangkok และแปลง timestamp เป็น string รูปแบบ Y-m-d H:i:s
date_default_timezone_set('Asia/Bangkok');
$time = date("Y-m-d H:i:s", $time);

// เชื่อมต่อกับฐานข้อมูล MySQL
$conn = new mysqli("localhost", "amt","P@ssw0rd!amt","gps_db");

// ตรวจสอบการเชื่อมต่อ ถ้าเชื่อมต่อไม่ได้ให้หยุดและแสดงข้อความผิดพลาด
if ($conn->connect_error) {
    die("เชื่อมต่อไม่สำเร็จ: " . $conn->connect_error);
}

// เตรียมคำสั่ง SQL สำหรับเพิ่มหรืออัปเดตข้อมูล (ถ้ามี device_id ซ้ำจะอัปเดตค่าใหม่)
$sql = "INSERT INTO location_logs (device_id, latitude, longitude, timestamp)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            latitude = VALUES(latitude),
            longitude = VALUES(longitude),
            timestamp = VALUES(timestamp)";

// เตรียม statement สำหรับ SQL (ป้องกัน SQL Injection)
$stmt = $conn->prepare($sql);

// ผูกค่าพารามิเตอร์กับ statement (s = string, d = double)
$stmt->bind_param("sdds", $device_id, $lat, $lng, $time);

// รันคำสั่ง SQL เพื่อบันทึกหรืออัปเดตข้อมูล
if (!$stmt->execute()) {
    echo " SQL ERROR:" . $stmt->error;
} else {
    // ตอบกลับไปยัง client ว่าสำเร็จ
    echo json_encode(["success" => true]);
}

// ปิด statement และการเชื่อมต่อฐานข้อมูล
$stmt->close();
$conn->close();
?>