<?php 
header('Access-Control-Allow-Origin: *'); 
header('Content-Type: application/json'); 
date_default_timezone_set('Asia/Bangkok'); 

error_reporting(E_ALL); 
ini_set('display_errors', 1); 

$conn = new mysqli("localhost", "amt", "P@ssw0rd!amt", "gps_db");
if ($conn->connect_error) { 
    http_response_code(500); 
    echo json_encode(["error" => "Connection failed: " . $conn->connect_error]); 
    exit(); 
}

$conn->query("SET time_zone = '+07:00'");

$days = isset($_GET['days']) ? intval($_GET['days']) : 0;

// ✅ ตรวจสอบโหมดการดึงข้อมูล
if ($days === 0) {
    // 🎯 กรณีวันนี้ — ดึงข้อมูลตั้งแต่ 00:00 ถึง 23:59 ของวันนี้
    $sql = "
        SELECT gps.device_id, gps.latitude, gps.longitude, gps.timestamp, emp.employee_name
        FROM location_log AS gps
        LEFT JOIN devices AS emp ON gps.device_id = emp.device_id
        WHERE DATE(gps.timestamp) = CURDATE()
        ORDER BY gps.timestamp ASC
    ";
} else {
    // 🎯 กรณีย้อนหลังตามจำนวนวันที่เลือก (1 = เมื่อวาน, 2 = ย้อนหลัง 2 วัน ฯลฯ)
    $sql = "
        SELECT gps.device_id, gps.latitude, gps.longitude, gps.timestamp, emp.employee_name
        FROM location_log AS gps
        LEFT JOIN devices AS emp ON gps.device_id = emp.device_id
        WHERE DATE(gps.timestamp) = CURDATE() - INTERVAL $days DAY
        ORDER BY gps.timestamp ASC
    ";
}

$result = $conn->query($sql);

// ✅ นับจำนวน device ทั้งหมด
$countSql = "SELECT COUNT(device_id) AS total_devices FROM devices";
$countResult = $conn->query($countSql);
$totalDevices = 0;
if ($countResult && $countResult->num_rows > 0) {
    $row = $countResult->fetch_assoc();
    $totalDevices = $row['total_devices'];
}

// ✅ ดึงรายชื่อพนักงานทั้งหมด
$employees = [];
$empResult = $conn->query("SELECT device_id, employee_name FROM devices");
if ($empResult && $empResult->num_rows > 0) {
    $employees = $empResult->fetch_all(MYSQLI_ASSOC);
}

// ✅ รวมข้อมูลทั้งหมดเป็น JSON
$response = [
    "total_devices" => $totalDevices,
    "locations" => ($result && $result->num_rows > 0) ? $result->fetch_all(MYSQLI_ASSOC) : [],
    "employees" => $employees
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>
