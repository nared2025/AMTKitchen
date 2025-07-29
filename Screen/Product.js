import { View, Text, ScrollView, Image, TouchableOpacity, StyleSheet } from 'react-native';
import React from 'react';

const ProfileScreen = ({ navigation }) => {
  const onPressItem = (id, name) => {
    navigation.navigate('Detail', { id: id, name: name });
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Product</Text>

      <View style={styles.gridContainer}>
        <TouchableOpacity
          style={styles.item}
          onPress={() =>
            onPressItem(
              1,
              '30 l/hr Counter Top Instant Water Boiler is fully controlled by electronic monitoring system, which is able to guarantee the temperature of water output at 92-97C'
            )
          }
        >
          <Image
            source={require('../img/1-4945f79b.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>WM-35-1</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.item}
          onPress={() =>
            onPressItem(
              2,
              '55 l/hr Counter Top Instant Water Boiler is fully controlled by electronic monitoring system, which is able to guarantee the temperature of water output at 92-97C'
            )
          }
        >
          <Image
            source={require('../img/2.-Counter-Top-Water-Boiler-WM-60-1new-.jpg.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>WM-60-1</Text>
        </TouchableOpacity>
        
        <TouchableOpacity
        style = {styles.item}
        onPress={() => 
            onPressItem(
                3,
                '90 l/hr Counter Top Instant Water Boiler is fully controlled by electronic monitoring system, which is able to guarantee the temperature of water output at 92-97C'
            )
        }
        >
         <Image
            source={require('../img/3.-Counter-Top-Water-Boiler-WM-80-1new-800x800.jpg.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>WM-80-1</Text>
        </TouchableOpacity>
        <TouchableOpacity
        style = {styles.item}
        onPress={() => 
            onPressItem(
                4,
                '180 l/hr, Bulk Volume Instant Water Boiler is fully controlled by electronic monitoring system, which is able to guarantee the temperature of water output at 92-97C'
            )
        }
        >
         <Image
            source={require('../img/4.-Bulk-Volume-Version-Water-Boiler-WM-100-1new-800x800.jpg.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>WM-100-1</Text>
        </TouchableOpacity>
        <TouchableOpacity
        style = {styles.item}
        onPress={() => 
            onPressItem(
                5,
                'Ice Crusher that can crush ice at maximum 15 kg/h. It is excellent for preparing cocktails, drinks and frozen desserts.'
            )
        }
        >
         <Image
            source={require('../img/1.-Bartscher-Ice-Crusher-4ICE-15-kg-800x800.jpg.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>4ICE+</Text>
        </TouchableOpacity>
        <TouchableOpacity
        style = {styles.item}
        onPress={() => 
            onPressItem(
                6,
                'Ice Crusher that can crush ice at maximum 60 kg per hour.'
            )
        }
        >
         <Image
            source={require('../img/2.-Bartscher-Ice-Crusher-135012-60-kg-800x800.jpg.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>4ICE+</Text>
        </TouchableOpacity>
        <TouchableOpacity
        style = {styles.item}
        onPress={() => 
            onPressItem(
                7,
                'Powerful Ice Crusher that produces crushed ice at maximum 120 kg/h. Suitable for bars or restaurant that do not have flake ice machine.'
            )
        }
        >
         <Image
            source={require('../img/3.-Bartscher-Ice-Crusher-2000-120-kg-800x800.jpg.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>2000</Text>
        </TouchableOpacity>
        <TouchableOpacity
        style = {styles.item}
        onPress={() => 
            onPressItem(
                8,
                'A CUBE looking Glass Chiller that sanitises glasses with food grade CO2, killing up to 88% of bacteria on the glass surface, while removing bad odours.'
            )
        }
        >
         <Image
            source={require('../img/2.1-Thrill-Vortex-Cube-black-808x800.png.png')}
            style={styles.image}
          />
          <Text style={styles.itemText}>Vortex Cube</Text>
        </TouchableOpacity>

        {/* เพิ่มสินค้าได้เรื่อย ๆ แบบนี้ */}
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 10,
    backgroundColor: '#fff'
  },
  title: {
    fontSize: 30,
    marginBottom: 20,
  },
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between', // เว้นช่องระหว่าง item
  },
  item: {
    width: '48%', // 2 ชิ้นต่อแถว (ใกล้เคียง 50%)
    marginBottom: 15,
    backgroundColor: '#f4f4f4',
    borderRadius: 8,
    padding: 10,
    alignItems: 'center',
  },
  image: {
    width: 100,
    height: 100,
    marginBottom: 8,
    resizeMode: 'contain',
  },
  itemText: {
    color: '#8DC63F',
    fontWeight: 'bold',
  },
});

export default ProfileScreen;
