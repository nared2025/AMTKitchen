<?php
$conn = new mysqli("localhost","amt","P@ssw0rd!amt","gps_db");
// เรียงลำดับจากล่าสุด (timestamp จากมากไปน้อย) จำกัดแค่ 50 รายการล่าสุด
$result = $conn->query("SELECT * FROM location_logs ORDER BY timestamp DESC LIMIT 50");
// แปลงผลลัพธ์ที่ได้จาก Mysql ให้เป็น json และส่งกลับไปยัง client  (เช่น JavaScript บนเว็บ)
echo json_encode($result->fetch_all(MYSQLI_ASSOC));
?>