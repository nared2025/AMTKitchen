// Detail.js
import React from 'react';
import { View, Text, Image,StyleSheet } from 'react-native';

const images = {
  1: require('../img/1-4945f79b.png'),
  2: require('../img/2.-Counter-Top-Water-Boiler-WM-60-1new-.jpg.png'),
  3: require('../img/3.-Counter-Top-Water-Boiler-WM-80-1new-800x800.jpg.png'),
  4: require('../img/4.-Bulk-Volume-Version-Water-Boiler-WM-100-1new-800x800.jpg.png'),
  5: require('../img/1.-Bartscher-Ice-Crusher-4ICE-15-kg-800x800.jpg.png'),
  6: require('../img/2.-Bartscher-Ice-Crusher-135012-60-kg-800x800.jpg.png'),
  7: require('../img/3.-Bartscher-Ice-Crusher-2000-120-kg-800x800.jpg.png'),
  8: require('../img/2.1-Thrill-Vortex-Cube-black-808x800.png.png')
}

const Detail = ({ route }) => {
  const { id, name } = route.params;
  return (
    <View style={styles.container}>
    <Image source={images[route.params.id]} style={styles.image} />
    <Text style={styles.text}>{route.params.name}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
    container: {
    flex: 1,                // เต็มหน้าจอ
    justifyContent: 'flex-start', // เลื่อนลงมานิดหน่อย
    alignItems: 'center',  // จัดให้อยู่ตรงกลางแนวนอน
    paddingTop: 50,        // ขยับลงจากขอบบน
    backgroundColor: '#fff'
  },
  image: {
    width: 300,
    height: 300,
    marginBottom: 20,       // เว้นระยะใต้รูป
    resizeMode:'contain' //รูปแสดงเต็ม
  },
  text: {
     color: '#1a1a1a',
    fontSize: 22,
    fontWeight: '600',
    textAlign: 'center',
    lineHeight: 28,
    letterSpacing: 0.3,
  }
});

export default Detail;
