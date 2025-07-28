import { View, Text, Pressable, Button } from 'react-native'
import React from 'react'
import useLocation from '../hooks/useLocation';

const home = ({navigation}) => {
  const { latitude, longitude, errorMsg } = useLocation();
    const onPress = () => {
      navigation.navigate('Profile');
    };

    const onGetData = () => {
        console.log('กดรับข้อมูล');
        
    };
  return (
    <View>
    <Pressable onPress = {onPress}>
      
    <View>
      <Text>home</Text>
    </View>
    </Pressable>

    <View>
        <Button title="รับข้อมูล" onPress={onGetData} />
            </View>
    </View>

  )
}

export default home