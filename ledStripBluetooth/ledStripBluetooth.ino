#include <ArduinoBLE.h>
#include <Adafruit_NeoPixel.h>
#include <string>

#define PIN 2
#define NUM_LEDS 300
 
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUM_LEDS, PIN, NEO_GRB + NEO_KHZ800);


const char * deviceServiceUuid = "19b10000-e8f2-537e-4f6c-d104768a1214";
const char * deviceServiceRequestCharacteristicUuid = "19b10001-e8f2-537e-4f6c-d104768a1215";
const char * deviceServiceResponseCharacteristicUuid = "19b10001-e8f2-537e-4f6c-d104768a1216";

BLEService service(deviceServiceUuid);
BLEStringCharacteristic serviceRequestCharacteristic(deviceServiceRequestCharacteristicUuid, BLEWrite, 30);
BLEStringCharacteristic serviceResponseCharacteristic(deviceServiceResponseCharacteristicUuid, BLENotify, 30);

int colorR = 0, colorG = 0, colorB = 0, brightness = 255;

enum class Mode { None, Rainbow, Color };
Mode currentMode = Mode::None;

bool isStringInt(String str) {
  if (str.length() == 0) return false; // Pusty ciąg to nie liczba

  for (int i = 0; i < str.length(); i++) {
    if (!isDigit(str[i])) {
      return false; // Jeśli znajdziesz coś, co nie jest cyfrą, zwróć false
    }
  }
  return true; // Wszystkie znaki to cyfry
}

void none(){
  pixels.clear();
  pixels.show();
}

void rainbow(){
  static unsigned int position = 0;  // Position for rainbow animation
  pixels.rainbow(position, 1, 255, brightness, true);
  pixels.show();
  position = (position + 100) % 65536;
}

void color(){
  pixels.clear();
  pixels.fill(pixels.Color(colorR  * brightness / 255, colorG  * brightness / 255, colorB  * brightness / 255),100,NUM_LEDS-100);
  pixels.show();
}

void handleCommand(String command){
  command.trim();
  Serial.println("Recieved Command: " + String(command));

  if(command == "rainbow"){
    currentMode = Mode::Rainbow;
    Serial.println("Set rainbow");
  }
  else if(command.startsWith("color")){
    int firstComma = command.indexOf(',') + 1;
    int secondComma = command.indexOf(',', firstComma);
    int thirdComma = command.indexOf(',', secondComma + 1);

    if(firstComma > 0 && secondComma > firstComma && thirdComma > secondComma){
      colorR = command.substring(firstComma, secondComma).toInt();
      colorG = command.substring(secondComma + 1, thirdComma).toInt();
      colorB = command.substring(thirdComma + 1).toInt();

      if(colorR == 0 && colorG == 0 && colorB == 0){
        currentMode = Mode::None;
        Serial.println("Turn off");
      }
      else{
        currentMode = Mode::Color;
        Serial.println("Set Static Color: R(" + String(colorR) + ") G(" + String(colorG) + ") B(" + String(colorB) + ")");
      }
    }
  }
  else if(isStringInt(command)){
    brightness = constrain(command.toInt(), 0, 255);
    Serial.println("Set brightness: " + String(brightness));
  }
}


void setup() {
  pixels.begin();
  pixels.clear(); 
  pixels.show();
  Serial.begin(9600);

  BLE.setDeviceName("LED STRIP");
  BLE.setLocalName("LED STRIP");

  if (!BLE.begin()) {
    Serial.println("- Starting Bluetooth® Low Energy module failed! :c");
    while (1);
  }

  BLE.setAdvertisedService(service);
  service.addCharacteristic(serviceRequestCharacteristic);
  service.addCharacteristic(serviceResponseCharacteristic);
  BLE.addService(service);
  serviceResponseCharacteristic.writeValue("0");

  BLE.advertise();

  Serial.println("Arduino R4 WiFi BLE (Peripheral Device)");
  Serial.println(" ");
}


void loop() {
  // Handle BLE connection
  BLEDevice central = BLE.central();
  
  if (central && central.connected()) {
    if(serviceRequestCharacteristic.written()){
      handleCommand(serviceRequestCharacteristic.value());
    }

    switch (currentMode){
      case Mode::Rainbow: rainbow(); break;
      case Mode::Color: color(); break;
      case Mode::None: none(); break;
      default: break;
    }
  }
  else if (currentMode != Mode::None){
    currentMode = Mode::None;
  }
}