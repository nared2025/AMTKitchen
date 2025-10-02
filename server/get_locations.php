<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
date_default_timezone_set('Asia/Bangkok');

// Debug (แสดง error ตอนพัฒนา)
error_reporting(E_ALL);
ini_set('display_errors', 1);

// เชื่อมต่อ MySQL
$conn = new mysqli("localhost", "amt", "P@ssw0rd!amt", "gps_db");

// ตรวจสอบการเชื่อมต่อ
if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode(["error" => "Connection failed: " . $conn->connect_error]);
    exit();
}

// query ดึง 50 พิกัดล่าสุด
$sql = "SELECT 
            gps.device_id,
            gps.latitude,
            gps.longitude,
            gps.`timestamp`,
            emp.employee_name
        FROM location_log AS gps
        LEFT JOIN devices AS emp ON gps.device_id = emp.device_id
        ORDER BY gps.`timestamp` DESC
        LIMIT 50";

$result = $conn->query($sql);

// ตรวจสอบว่ามีข้อมูลหรือไม่
if ($result && $result->num_rows > 0) {
    echo json_encode($result->fetch_all(MYSQLI_ASSOC));
} else {
    echo json_encode([]);
}

// ปิดการเชื่อมต่อ
$conn->close();
?>

