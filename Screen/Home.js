import { View, Text, Pressable, Button, Image, StyleSheet } from 'react-native'
import React from 'react'
import useLocation from '../hooks/useLocation';

const home = ({navigation}) => {
  const { latitude, longitude, errorMsg } = useLocation();
    const onPress = () => {
      navigation.navigate('Product');
    };
    const onPressAboutUs = () => {
      navigation.navigate('AboutUs');
    };

    const onGetData = () => {
        console.log('กดรับข้อมูล');
        
    };
  return (
    <View>
      <Image source ={require('../img/Service-Cover-Test-3.png')} style={styles.header} />
    <Pressable onPress = {onPress}>
      
    <View>
      <Text>Product</Text>
    </View>
    </Pressable>
    <Pressable onPress = { onPressAboutUs }>
      
    <View>
      <Text>About Us</Text>
    </View>
    </Pressable>


    <View>
        <Button title="รับข้อมูล" onPress={onGetData} />
            </View>
    </View>

  )
}

const styles = StyleSheet.create ({
  header: {
    width:'100%',
    height:100,
    resizeMode: 'cover',
  },

});

export default home