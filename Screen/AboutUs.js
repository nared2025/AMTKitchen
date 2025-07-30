import { StyleSheet, Text, View, ScrollView, Image } from 'react-native';
import React from 'react';

export default function AboutUs() {
  return (
    <ScrollView>
      <View style={styles.container}>
        <Image source={require('../img/NJjourney-final.png')} style={styles.header}/>
        <View>
        <Text style={styles.textbox}>OUR JOURNEY</Text>
        <Image source={require('../img/Asset-1_2x.png')} style={styles.underline}/>
        </View>
        <Text style={styles.text}>
          {`The journey began in the late 1971,
when Allied Metals (Thailand) Co., Ltd. was established in Bangkok, Thailand
with the vision to supply professional kitchen equipment
to hotels, restaurants, airline catering, cooking schools,
and other Food & Beverages facilities.

Under BOI license, our new family member, Siam Stainless Steel Co., Ltd,
was set up in 1974 with an intention to fabricate quality stainless steel
kitchen equipment for the Thai market.`}
        </Text>
        <View>
            <Image source={require('../img/vision.png')} style={styles.headerimg}/>
            <Text style={styles.textbox}>OUR PHILOSOPHY</Text>
            <Image source={require('../img/Asset-1_2x.png')} style={styles.underlineimg}/>
            <Text style={styles.text}>
  {`Is to associate with customers in their thinking, understand their needs, and convert customer's wish list into a functional and productive kitchen facility.

Today, many F&B Facilities face issues of manpower shortage, limitation of space, food waste & hygiene problem, as well as energy consumption, causing challenges to obtain productive F&B facilities that helps to generate expected revenue.

Our role is to deliver sufficient commercial kitchen, and products that reduce dependency on manpower, minimize environmental impacts while optimizing profit by managing risks and balance productivity through our Turnkey Solution being DESIGN-BUILD-MAINTAIN.`}
            </Text>
        </View>
         <View>
            <Image source={require('../img/OUR-MISSION_Join.png')} style={styles.headerimg}/>
            <Text style={styles.textbox}>OUR VISION</Text>
            <Image source={require('../img/Asset-1_2x.png')} style={styles.underlineOUR}/>
            <Text style={styles.text}>
  {`Join our journey in building a sustainable planet`}
            </Text>
        </View>
        <View>
            <Text style={styles.textbox}>OUR MISSION</Text>
            <Image source={require('../img/Asset-1_2x.png')} style={styles.underlineMISSION}/>
            <Text style={styles.text}>
  {`To provide a reliable 
and sustainable kitchen solutions 
with our expertise and experience 
via our products and services 
in order to uplift the quality of life 
for our community. 
`}</Text>
        </View>
        <View>
            <Image source={require('../img/Logo1.png.png')} style={styles.logo1}/>
        </View>
          <View>
            <Image source={require('../img/Picture1.jpg.png')} style={styles.headerimg}/>
            <Text style={styles.textbox}>OUR 1ST SUSTAINABILITY PROJECT</Text>
            <Image source={require('../img/Asset-1_2x.png')} style={styles.underlineOUR1ST}/>
            <Image source={require('../img/Asset-3_2x-1.png')} style={styles.logo2}/>
            <Text style={styles.textcontent}>
  {`The Food School passionately considered implementing sustainable measures from design process, equipment selection practice, collaborative partnership, up to measuring the results and their impacts.`}
            </Text>
            <Text style={styles.textcontent}>
  {`Kitchen design intention was to optimize functionality, productivity, and flexibility so this facility could be professionally utilized by various world class teaching methods and concepts; enabling, students to apply their knowledge and skills seamlessly into real life situations.`}
            </Text>
            <Text style={styles.textcontent}>
  {`Functions and capacity were calculated systematically to minimize operation costs, optimizing profitability. Food safety and sanitation aspects were considered vigorously to prevent cross contamination and potential hazards.`}
            </Text>
            <Text style={styles.textcontent}>
  {`The Food School is also actively involved in minimizing the impact of food waste, reducing and recycling it. As energy takes the biggest part in total costs of operating a commercial kitchen, the implementation of high performance equipment and collaborative partnership with key manufacturers takes on an important role in achieving energy efficiency practice, especially for kitchen ventilation system, refrigeration system, and cooking equipment.`}
            </Text>
        </View>
      </View>

    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
  },
  textbox: {
    color: '#36507A',
    fontWeight: 'bold',
    // textDecorationLine: 'underline',
  },
  underline: {
    width: '29%',
    height:  5,
  },
  text: {
    fontSize: 16,
    lineHeight: 24,
  },
  header: {
    width:'100%',
    height:300,
    resizeMode:'center',
    marginBottom: 15,
  },
  headerimg: {
    width:'100%',
    height:200,
    resizeMode:'center',
  },
  underlineimg:{
    width: '36%',
    height: 5,
  },
  underlineOUR:{
    width: '24%',
    height: 5,
  },
  underlineMISSION:{
    width: '28%',
    height: 5,
  },
  logo1: {
    width: '50%',
    height: 100,
    resizeMode: 'center',
    alignSelf: 'center',
  },
  underlineOUR1ST: {
    width: '72%',
    height: 5,
  },
  logo2: {
    marginBottom:20,
    marginTop:20,
    width: '18%',
    height: 50,
  },
   textcontent: {
    fontSize: 16,
    lineHeight: 24,
    marginTop: 15,
  },
});
