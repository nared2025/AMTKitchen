import { StyleSheet, Text, View } from 'react-native'
import React, { useEffect, useState} from 'react'
import * as Location from "expo-location"
import * as TaskManager from 'expo-task-manager';
import * as Device from 'expo-device';

// ชื่อ task สำหรับ background location
const BACKGROUND_LOCATION_TASK = 'background-location-task';

// กำหนด task สำหรับ background location tracking - ใช้วิธีการเดียวกับโค้ดต้นฉบับ
TaskManager.defineTask(BACKGROUND_LOCATION_TASK, ({ data, error }) => {
    if (error) {
        console.error('Background location error:', error);
        return;
    }
    
    if (data) {
        const location = data.locations[0]; // ดึงตำแหน่งล่าสุดจาก data
        
        if (!location) {
            console.log("⚠️ ยังไม่มี location ใน data");
            return;
        }
        
        console.log('📍 Background location received:', location.coords.latitude, location.coords.longitude);
        
        // ส่งตำแหน่งไปยัง server
        sendLocationToServer(location.coords);
    }
});

// ฟังก์ชันส่งข้อมูลตำแหน่งไปยัง server
const sendLocationToServer = async (coords) => {
    try {
        // ดึงข้อมูลอุปกรณ์ (เฉพาะ device_id) - ใช้วิธีเดียวกับโค้ดต้นฉบับ
        const deviceId = Device.osInternalBuildId || Device.deviceName || 'unknown_device';

        // สร้างเวลาประเทศไทย (UTC+7)
        const thaiTime = new Date();
        const utcTime = thaiTime.getTime() + (thaiTime.getTimezoneOffset() * 60000);
        const thaiTimestamp = new Date(utcTime + (7 * 3600000));

        const locationData = {
            device_id: deviceId, // รหัสเฉพาะของอุปกรณ์ (ดึงจาก expo-device)
            latitude: coords.latitude, // ค่า Latitude
            longitude: coords.longitude, // ค่า Longitude
            timestamp: thaiTimestamp.toISOString() // เวลาประเทศไทย (UTC+7) ในรูปแบบ ISO
        };

        console.log('📍 ตำแหน่งล่าสุด:', coords.latitude, coords.longitude);
        console.log('Sending location data:', locationData);

        // ใช้วิธีการส่งข้อมูลแบบเดียวกับโค้ดต้นฉบับ
        const response = await fetch('http://119.46.60.16/TrackGPS/save_location.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(locationData)
        });

        const result = await response.text();
        console.log("Server response:", result);

        if (!response.ok) {
            console.error('Failed to send location. Status:', response.status);
        }
    } catch (error) {
        console.error('Fetch error:', error);
    }
};

// Custom hook สำหรับดึงตำแหน่งปัจจุบันของผู้ใช้
const useLocation = () => {
    // สร้าง state สำหรับเก็บ error, ละติจูด และลองจิจูด
    const [errorMsg, setErrorMsg] = useState("");
    const [longitude, setLongitude] = useState("");
    const [latitude, setLatitude] = useState("");
    const [isBackgroundLocationActive, setIsBackgroundLocationActive] = useState(false);
    const [deviceInfo, setDeviceInfo] = useState(null);

    // ฟังก์ชันดึงข้อมูลอุปกรณ์
    const getDeviceInfo = async () => {
        try {
            // ใช้วิธีเดียวกับโค้ดต้นฉบับ
            const deviceId = Device.osInternalBuildId || Device.deviceName || 'unknown_device';
            setDeviceInfo({ device_id: deviceId });
            console.log('Device ID:', deviceId);
            return deviceId;
        } catch (error) {
            console.error('Error getting device info:', error);
            return 'unknown_device';
        }
    };

    // ฟังก์ชันสำหรับขอ permission และดึงตำแหน่งผู้ใช้
    const getUserLocation = async () => {
        try {
            // ขอ permission การเข้าถึง location foreground
            let {status} = await Location.requestForegroundPermissionsAsync();
        
            // ถ้าไม่ได้รับอนุญาต
            if(status !== 'granted') {
                setErrorMsg('Permission to location was not granted');
                return;
            }

            // ดึงตำแหน่งปัจจุบัน
            let {coords} = await Location.getCurrentPositionAsync();

            // ถ้ามีข้อมูล coords
            if(coords) {
                const {latitude, longitude} = coords;
                console.log('lat and long is', latitude, longitude);
                setLatitude(latitude);
                setLongitude(longitude);
                
                // ส่งตำแหน่งไปยัง server
                await sendLocationToServer(coords);
                
                // reverse geocode หาตำแหน่งที่อยู่จาก lat long
                let response = await Location.reverseGeocodeAsync({
                    latitude,
                    longitude
                });

                console.log("USER LOCATION IS", response);
            }
        } catch (error) {
            console.error('Error getting user location:', error);
            setErrorMsg('Error getting location: ' + error.message);
        }
    };

    // ฟังก์ชันเริ่ม background location tracking
    const startBackgroundLocation = async () => {
        try {
            // ขอ permission สำหรับ background location
            const { status } = await Location.requestBackgroundPermissionsAsync();
            
            if (status !== 'granted') {
                setErrorMsg('Background location permission not granted');
                return false;
            }

            // เริ่ม background location tracking
            await Location.startLocationUpdatesAsync(BACKGROUND_LOCATION_TASK, {
                accuracy: Location.Accuracy.High,
                timeInterval: 1800000, // อัพเดททุก 30 นาที
                distanceInterval: 100, // อัพเดทเมื่อเคลื่อนที่ 100 เมตร
                showsBackgroundLocationIndicator: true, // แสดง indicator บน iOS
                foregroundService: {
                    notificationTitle: 'Location Tracking',
                    notificationBody: 'Tracking your location in background',
                    notificationColor: '#fff'
                }
            });

            setIsBackgroundLocationActive(true);
            console.log('Background location tracking started');
            return true;
        } catch (error) {
            console.error('Error starting background location:', error);
            setErrorMsg('Error starting background location: ' + error.message);
            return false;
        }
    };

    // ฟังก์ชันหยุด background location tracking
    const stopBackgroundLocation = async () => {
        try {
            const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
            if (isRegistered) {
                await Location.stopLocationUpdatesAsync(BACKGROUND_LOCATION_TASK);
                setIsBackgroundLocationActive(false);
                console.log('Background location tracking stopped');
            }
        } catch (error) {
            console.error('Error stopping background location:', error);
        }
    };

    // ฟังก์ชันตรวจสอบสถานะ background location
    const checkBackgroundLocationStatus = async () => {
        try {
            const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
            setIsBackgroundLocationActive(isRegistered);
            return isRegistered;
        } catch (error) {
            console.error('Error checking background location status:', error);
            return false;
        }
    };

    // เรียกใช้เมื่อ component ถูก mount
    useEffect(() => {
        const initializeLocation = async () => {
            await getDeviceInfo();
            await getUserLocation();
            await checkBackgroundLocationStatus();
            await startBackgroundLocation();
        };

        initializeLocation();

        // Cleanup function
        return () => {
            stopBackgroundLocation();
        };
    }, []);

    // คืนค่าข้อมูลและฟังก์ชันต่างๆ
    return {
        latitude, 
        longitude, 
        errorMsg,
        deviceInfo,
        isBackgroundLocationActive,
        startBackgroundLocation,
        stopBackgroundLocation,
        getUserLocation,
        checkBackgroundLocationStatus
    };
};

export default useLocation;

// สไตล์ (ยังไม่ได้ใช้งาน)
const styles = StyleSheet.create({});