import { NativeModules, Platform } from 'react-native';

const { LocationModule } = NativeModules;

class LocationService {
  /**
   * เริ่มต้นการติดตามตำแหน่ง GPS
   * @returns {Promise<string>} ผลลัพธ์การเริ่มต้น
   */
  static async startLocationTracking() {
    if (Platform.OS !== 'android') {
      throw new Error('Location tracking is only supported on Android');
    }
    
    if (!LocationModule) {
      throw new Error('LocationModule is not available');
    }
    
    return await LocationModule.startLocationTracking();
  }

  /**
   * หยุดการติดตามตำแหน่ง GPS
   * @returns {Promise<string>} ผลลัพธ์การหยุด
   */
  static async stopLocationTracking() {
    if (Platform.OS !== 'android') {
      throw new Error('Location tracking is only supported on Android');
    }
    
    if (!LocationModule) {
      throw new Error('LocationModule is not available');
    }
    
    return await LocationModule.stopLocationTracking();
  }

  /**
   * ตรวจสอบสถานะการติดตามตำแหน่ง
   * @returns {Promise<Object>} สถานะการติดตาม
   */
  static async isLocationTrackingActive() {
    if (Platform.OS !== 'android') {
      return { isActive: false };
    }
    
    if (!LocationModule) {
      return { isActive: false };
    }
    
    return await LocationModule.isLocationTrackingActive();
  }

  /**
   * ขอสิทธิ์ยกเว้น Battery Optimization
   * @returns {Promise<string>} ผลลัพธ์การขอสิทธิ์
   */
  static async requestBatteryOptimizationExemption() {
    if (Platform.OS !== 'android') {
      throw new Error('Battery optimization exemption is only supported on Android');
    }
    
    if (!LocationModule) {
      throw new Error('LocationModule is not available');
    }
    
    return await LocationModule.requestBatteryOptimizationExemption();
  }
}

export default LocationService;
