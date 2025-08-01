import { View, Text, Pressable, Image, StyleSheet, SafeAreaView, Dimensions } from 'react-native';
import React from 'react';
import useLocation from '../hooks/useLocation';
import Octicons from 'react-native-vector-icons/Octicons';
import Feather from 'react-native-vector-icons/Feather';

const Home = ({ navigation }) => {
  const { latitude, longitude, errorMsg } = useLocation();

  const onPress = () => navigation.navigate('Product');
  const onPressAboutUs = () => navigation.navigate('AboutUs');
  const onGetData = () => console.log('กดรับข้อมูล');

  return (
    <SafeAreaView style={styles.safeArea}>
      <Image source={require('../img/Service-Cover-Test-3.png')} style={styles.header} />
      <View style={styles.containerbox}>
        <Pressable onPress={onPress} style={styles.button}>
          <Feather name="shopping-bag" color="#000" size={20}/>
          <Text style={styles.textbutton}>Product</Text>
        </Pressable>
        <Pressable onPress={onPressAboutUs} style={styles.button}>
          <Octicons name="person" color="#000" size={20}/>
          <Text style={styles.textbutton}>About Us</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
};
const {width} = Dimensions.get('window');
const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    width: '100%',
    height: width/3.2,
    resizeMode: 'cover',
    
  },
  containerbox: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    // padding: 20,
    // marginTop: 20,
  },
  button: {
    padding: 10,
    backgroundColor: '#ffffff',
    borderStyle: 'solid',
    flexDirection: 'row',
    alignItems: 'center',
  },
  textbutton: {
    marginLeft: 6,
  }

});

export default Home;
