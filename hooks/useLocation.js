import { StyleSheet, Text, View, Alert, Linking, Platform } from 'react-native'
import React, { useEffect, useState} from 'react'
import * as Location from "expo-location"
import * as TaskManager from 'expo-task-manager';
import * as Device from 'expo-device';
import LocationService from '../LocationModule';

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
        console.log('📍 Device ID:', deviceId);
        console.log('📍 Sending location data:', JSON.stringify(locationData, null, 2));

        // ใช้วิธีการส่งข้อมูลแบบเดียวกับโค้ดต้นฉบับ
        const response = await fetch('https://tracking.alliedmetals.com/trackgps/save_location.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(locationData)
        });

        const result = await response.text();
        console.log("📍 Server response:", result);
        console.log("📍 Response status:", response.status);

        if (!response.ok) {
            console.error('❌ Failed to send location. Status:', response.status);
            console.error('❌ Response text:', result);
            // เพิ่มการบันทึก error ลง local storage
            try {
                const errorLog = {
                    timestamp: new Date().toISOString(),
                    status: response.status,
                    error: result,
                    location: { latitude: coords.latitude, longitude: coords.longitude }
                };
                console.log('❌ Error logged:', errorLog);
            } catch (e) {
                console.error('❌ Failed to log error:', e);
            }
        } else {
            console.log('✅ Location sent successfully');
        }
    } catch (error) {
        console.error('❌ Fetch error:', error);
        console.error('❌ Error details:', error.message);
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
            console.log('📍 Requesting location permissions...');
            
            // ขอ permission การเข้าถึง location foreground
            let {status} = await Location.requestForegroundPermissionsAsync();
            console.log('📍 Foreground permission status:', status);
        
            // ถ้าไม่ได้รับอนุญาต
            if(status !== 'granted') {
                console.error('❌ Foreground location permission not granted');
                setErrorMsg('Permission to location was not granted');
                return;
            }

            // ขอ background permission
            let {status: backgroundStatus} = await Location.requestBackgroundPermissionsAsync();
            console.log('📍 Background permission status:', backgroundStatus);
            
            if(backgroundStatus !== 'granted') {
                console.warn('⚠️ Background location permission not granted, but continuing...');
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

    // ฟังก์ชันเริ่ม background location tracking (ใช้ Native Service)
    const startBackgroundLocation = async () => {
        try {
            if (Platform.OS === 'android') {
                // ใช้ Native Service สำหรับ Android
                console.log('🚀 Starting native location service...');
                
                // ขอ permission ก่อน
                const { status } = await Location.requestBackgroundPermissionsAsync();
                if (status !== 'granted') {
                    console.log('❌ Background location permission not granted');
                    setErrorMsg('Background location permission not granted');
                    return false;
                }
                
                // เริ่ม native service
                const result = await LocationService.startLocationTracking();
                console.log('✅ Native location service started:', result);
                
                setIsBackgroundLocationActive(true);
                return true;
            } else {
                // ใช้ Expo Location สำหรับ iOS (fallback)
                console.log('🍎 Using Expo Location for iOS...');
                
                const { status } = await Location.requestBackgroundPermissionsAsync();
                if (status !== 'granted') {
                    console.log('❌ Background location permission not granted');
                    setErrorMsg('Background location permission not granted');
                    return false;
                }

                console.log('✅ Background location permission granted');

                const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
                if (isRegistered) {
                    console.log('✅ Background location task already registered');
                    setIsBackgroundLocationActive(true);
                    return true;
                }

                await Location.startLocationUpdatesAsync(BACKGROUND_LOCATION_TASK, {
                    accuracy: Location.Accuracy.BestForNavigation,
                    timeInterval: 30000,
                    distanceInterval: 1000,
                    showsBackgroundLocationIndicator: true,
                    foregroundService: {
                        notificationTitle: 'AMT Kitchen Location',
                        notificationBody: 'กำลังติดตามตำแหน่งของคุณ',
                        notificationColor: '#3498db'
                    }
                });

                setIsBackgroundLocationActive(true);
                console.log('✅ Background location tracking started successfully');
                return true;
            }
        } catch (error) {
            console.error('❌ Error starting background location:', error);
            console.error('❌ Error details:', error.message);
            setErrorMsg('Error starting background location: ' + error.message);
            return false;
        }
    };

    // ฟังก์ชันหยุด background location tracking
    const stopBackgroundLocation = async () => {
        try {
            if (Platform.OS === 'android') {
                // ใช้ Native Service สำหรับ Android
                console.log('🛑 Stopping native location service...');
                const result = await LocationService.stopLocationTracking();
                console.log('✅ Native location service stopped:', result);
                setIsBackgroundLocationActive(false);
            } else {
                // ใช้ Expo Location สำหรับ iOS
                const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
                if (isRegistered) {
                    await Location.stopLocationUpdatesAsync(BACKGROUND_LOCATION_TASK);
                    setIsBackgroundLocationActive(false);
                    console.log('Background location tracking stopped');
                }
            }
        } catch (error) {
            console.error('Error stopping background location:', error);
        }
    };

    // ฟังก์ชันตรวจสอบสถานะ background location
    const checkBackgroundLocationStatus = async () => {
        try {
            if (Platform.OS === 'android') {
                // ใช้ Native Service สำหรับ Android
                const result = await LocationService.isLocationTrackingActive();
                setIsBackgroundLocationActive(result.isActive);
                return result.isActive;
            } else {
                // ใช้ Expo Location สำหรับ iOS
                const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
                setIsBackgroundLocationActive(isRegistered);
                return isRegistered;
            }
        } catch (error) {
            console.error('Error checking background location status:', error);
            return false;
        }
    };

    // เรียกใช้เมื่อ component ถูก mount
    useEffect(() => {
        const initializeLocation = async () => {
            try {
                console.log('🚀 Initializing location services...');
                
                await getDeviceInfo();
                console.log('✅ Device info loaded');
                
                await getUserLocation();
                console.log('✅ User location obtained');
                
                await checkBackgroundLocationStatus();
                console.log('✅ Background location status checked');
                
                const backgroundStarted = await startBackgroundLocation();
                if (backgroundStarted) {
                    console.log('✅ Background location started successfully');
                } else {
                    console.log('⚠️ Background location failed to start, but app will continue');
                }
            } catch (error) {
                console.error('❌ Error during location initialization:', error);
                setErrorMsg('Error initializing location: ' + error.message);
            }
        };

        initializeLocation();

        // Cleanup function
        return () => {
            console.log('🧹 Cleaning up location services...');
            if (Platform.OS === 'ios') {
                // On iOS, stop background updates when component unmounts
                stopBackgroundLocation();
            }
            // On Android, keep the native Foreground Service running
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