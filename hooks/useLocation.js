import { StyleSheet, Text, View, Alert, Linking, Platform, NativeModules } from 'react-native'
import React, { useEffect, useState} from 'react'
import * as Location from "expo-location"
import * as TaskManager from 'expo-task-manager';
import * as Device from 'expo-device';
import AsyncStorage from '@react-native-async-storage/async-storage';
import LocationService from '../LocationModule';

// ชื่อ task สำหรับ background location
const BACKGROUND_LOCATION_TASK = 'background-location-task';

// ฟังก์ชันลบ device ID เก่า (สำหรับทดสอบ)
const clearDeviceId = async () => {
    try {
        await AsyncStorage.removeItem('unique_device_id');
        console.log('Device ID cleared');
    } catch (error) {
        console.error('Error clearing device ID:', error);
    }
};

// ฟังก์ชันแสดง device ID ปัจจุบัน
const showCurrentDeviceId = async () => {
    try {
        const deviceId = await AsyncStorage.getItem('unique_device_id');
        console.log('Current Device ID:', deviceId);
        return deviceId;
    } catch (error) {
        console.error('Error getting current device ID:', error);
        return null;
    }
};

// ฟังก์ชันสร้าง unique device ID (global scope)
const getUniqueDeviceId = async () => {
    try {
        // ใช้ AsyncStorage เพื่อเก็บ device ID ที่สร้างแล้ว
        let deviceId = await AsyncStorage.getItem('unique_device_id');
        
        if (!deviceId) {
            // สร้าง unique ID ใหม่ที่รวมข้อมูลหลายอย่าง
            const timestamp = Date.now();
            const random1 = Math.random().toString(36).substring(2, 15);
            const random2 = Math.random().toString(36).substring(2, 15);
            const random3 = Math.random().toString(36).substring(2, 15);
            
            // สร้าง UUID-like string
            const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                const r = Math.random() * 16 | 0;
                const v = c == 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
            
            // รวมข้อมูลหลายอย่างเพื่อให้ unique มากขึ้น
            const deviceInfo = [
                Device.brand || 'unknown',
                Device.model || 'unknown', 
                Device.osInternalBuildId || 'unknown',
                Device.deviceName || 'unknown',
                Device.osVersion || 'unknown',
                Device.platform || 'unknown',
                timestamp.toString(),
                uuid,
                random1,
                random2,
                random3
            ].join('_');
            
            deviceId = deviceInfo;
            
            // บันทึก device ID ลง AsyncStorage
            await AsyncStorage.setItem('unique_device_id', deviceId);
            console.log('🆔 Created new unique device ID:', deviceId);
        } else {
            console.log('🆔 Using existing device ID:', deviceId);
        }
        
        return deviceId;
    } catch (error) {
        console.error('Error creating unique device ID:', error);
        // fallback ใช้วิธีเดิม
        return Device.osInternalBuildId || Device.deviceName || 'unknown_device';
    }
};

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
const sendLocationToServer = async (coords, attempt = 1) => {
    try {
        // ดึงข้อมูลอุปกรณ์ (เฉพาะ device_id) - ใช้ unique device ID
        const deviceId = await getUniqueDeviceId();

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
            // retry แบบง่าย: ลองใหม่สูงสุด 3 ครั้ง
            if (attempt < 3) {
                const nextAttempt = attempt + 1;
                const backoffMs = 3000 * attempt;
                console.log(`⏳ Retry sending in ${backoffMs} ms (attempt ${nextAttempt}/3)`);
                setTimeout(() => sendLocationToServer(coords, nextAttempt), backoffMs);
            }
        } else {
            console.log('✅ Location sent successfully');
        }
    } catch (error) {
        console.error('❌ Fetch error:', error);
        console.error('❌ Error details:', error.message);
        if (attempt < 3) {
            const nextAttempt = attempt + 1;
            const backoffMs = 3000 * attempt;
            console.log(`⏳ Retry sending in ${backoffMs} ms (attempt ${nextAttempt}/3)`);
            setTimeout(() => sendLocationToServer(coords, nextAttempt), backoffMs);
        }
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
            // สร้าง unique device ID ที่รวมข้อมูลหลายอย่าง
            const deviceId = await getUniqueDeviceId();
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
            // Sync device_id ก่อนดึงตำแหน่ง
            await syncDeviceIdToNative();
            
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

            // ส่ง last known position ทันทีถ้ามี (บูตใหม่/ยังจับสัญญาณไม่พร้อม)
            const lastKnown = await Location.getLastKnownPositionAsync();
            if (lastKnown?.coords) {
                try { await sendLocationToServer(lastKnown.coords); } catch {}
            }

            // ดึงตำแหน่งปัจจุบัน (active fix)
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

    // ฟังก์ชัน sync device_id ไปยัง Android Native
    const syncDeviceIdToNative = async () => {
        try {
            if (Platform.OS === 'android') {
                const deviceId = await getUniqueDeviceId();
                console.log('🔄 Syncing device ID to Native:', deviceId);
                
                // ใช้ AsyncStorage เพื่อให้ Native อ่านได้
                await AsyncStorage.setItem('unique_device_id', deviceId);
                console.log('✅ Device ID synced to Native');
            }
        } catch (error) {
            console.error('❌ Error syncing device ID to Native:', error);
        }
    };

    // ฟังก์ชันขอสิทธิ์ Battery Optimization
    const requestBatteryOptimizationExemption = async () => {
        try {
            if (Platform.OS === 'android') {
                const { NativeModules } = require('react-native');
                if (NativeModules?.LocationModule?.requestBatteryOptimizationExemption) {
                    const result = await NativeModules.LocationModule.requestBatteryOptimizationExemption();
                    console.log('🔋 Battery optimization exemption result:', result);
                    return result;
                }
            }
        } catch (error) {
            console.warn('⚠️ Could not request battery optimization exemption:', error);
        }
        return false;
    };

    // ฟังก์ชันเริ่ม background location tracking (ใช้ Native Service)
    const startBackgroundLocation = async () => {
        try {
            // Sync device_id ก่อนเริ่ม background tracking
            await syncDeviceIdToNative();
            
            if (Platform.OS === 'android') {
                // Android: พยายามใช้ Native ก่อน ถ้าไม่มีให้ fallback เป็น Expo
                console.log('🚀 Starting background location (Android)...');

                // ขอสิทธิ์ Battery Optimization ก่อน
                await requestBatteryOptimizationExemption();

                const { status } = await Location.requestBackgroundPermissionsAsync();
                if (status !== 'granted') {
                    console.log('❌ Background location permission not granted');
                    setErrorMsg('Background location permission not granted');
                    return false;
                }

                const hasNativeModule = !!NativeModules?.LocationModule;
                if (hasNativeModule) {
                    try {
                        const result = await LocationService.startLocationTracking();
                        console.log('✅ Native location service started:', result);
                        setIsBackgroundLocationActive(true);
                        return true;
                    } catch (nativeErr) {
                        console.warn('⚠️ Native LocationModule failed, falling back to Expo:', nativeErr?.message || nativeErr);
                    }
                } else {
                    console.log('ℹ️ Native LocationModule not found. Using Expo fallback.');
                }

                const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
                if (!isRegistered) {
                    await Location.startLocationUpdatesAsync(BACKGROUND_LOCATION_TASK, {
                        accuracy: Location.Accuracy.BestForNavigation,
                        timeInterval: 300000, // 5 นาที (5 * 60 * 1000 ms)
                        distanceInterval: 0, // ส่งทุก 5 นาที แม้ไม่ขยับ
                        showsBackgroundLocationIndicator: true,
                        foregroundService: {
                            notificationTitle: 'AMT Kitchen Location',
                            notificationBody: 'กำลังติดตามตำแหน่งของคุณ',
                            notificationColor: '#3498db'
                        }
                    });
                }

                setIsBackgroundLocationActive(true);
                console.log('✅ Background location tracking started via Expo');
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
                    timeInterval: 300000, // 5 นาที (5 * 60 * 1000 ms)
                    distanceInterval: 0, // ส่งทุก 5 นาที แม้ไม่ขยับ
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
                console.log('🛑 Stopping background location (Android)...');
                const hasNativeModule = !!NativeModules?.LocationModule;
                if (hasNativeModule) {
                    try {
                        const result = await LocationService.stopLocationTracking();
                        console.log('✅ Native location service stopped:', result);
                        setIsBackgroundLocationActive(false);
                        return;
                    } catch (nativeErr) {
                        console.warn('⚠️ Native stop failed, falling back to Expo:', nativeErr?.message || nativeErr);
                    }
                }

                const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
                if (isRegistered) {
                    await Location.stopLocationUpdatesAsync(BACKGROUND_LOCATION_TASK);
                }
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
                const hasNativeModule = !!NativeModules?.LocationModule;
                if (hasNativeModule) {
                    try {
                        const result = await LocationService.isLocationTrackingActive();
                        setIsBackgroundLocationActive(result.isActive);
                        return result.isActive;
                    } catch (nativeErr) {
                        console.warn('⚠️ Native status check failed, using Expo status:', nativeErr?.message || nativeErr);
                    }
                }
                const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_LOCATION_TASK);
                setIsBackgroundLocationActive(isRegistered);
                return isRegistered;
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
        checkBackgroundLocationStatus,
        clearDeviceId,
        showCurrentDeviceId,
        syncDeviceIdToNative,
        requestBatteryOptimizationExemption
    };
};

export default useLocation;

// สไตล์ (ยังไม่ได้ใช้งาน)
const styles = StyleSheet.create({});