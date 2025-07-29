import { View, Text } from 'react-native'
import React from 'react'
import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'

import Home from './Screen/Home'
import Product from './Screen/Product'
import Detail from './Screen/Detail'

const Stack = createNativeStackNavigator();

const App = () => {
  return (
    <NavigationContainer>
    <Stack.Navigator>
      <Stack.Screen
      name='Home'
      component={Home}
      options={{title: 'Wellcome'}}
      />
      <Stack.Screen 
      name="Product" 
      component={Product}
      options={{ title: 'Product'}}
      />
      <Stack.Screen 
      name='Detail'
      component={Detail}
      options={{ title: 'Detail'}}
      />
    </Stack.Navigator>  
  </NavigationContainer>
  )
}

export default App