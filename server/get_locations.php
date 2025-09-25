<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json'); // บอกให้ client รู้ว่า response เป็น JSON
date_default_timezone_set('Asia/bangkok');

// เชื่อมต่อ MySQL
$conn = new mysqli("localhost", "amt", "P@ssw0rd!amt", "gps_db");

// ตรวจสอบการเชื่อมต่อ
if ($conn->connect_error) {
    http_response_code(500); // ส่งสถานะผิดพลาดกลับ
    echo json_encode(["error" => "Connection failed: " . $conn->connect_error]);
    exit();
}

// query ดึง 50 พิกัดล่าสุด
$sql = "SELECT * FROM location_logs ORDER BY timestamp DESC LIMIT 50";
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
