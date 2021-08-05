# BLE (BLE통신으로 Server의 IMU데이터 Client로 송신)

## 사용 방법
  이 앱은 안드로이드 기기 전용이며 총 2대의 기기로 한 쪽은 Server, 다른 한 쪽은 Client로 동작 한다.
  
### Server
   시작 화면에서 장치 1을 클릭   
   1. 블루투스 On
   2. Gattserver On StartSuccess 토스트가 뜨는 지 확인
   3. 정상적으로 연결 후 연결 후 토글 스위치 On (IMU 데이터 송신)

### Client
   시작 화면에서 장치 2을 클릭
   
   1. Qr코드 등록 
   
   테스트용 
   
   ![seungmo_phone](https://user-images.githubusercontent.com/76981135/128312623-07e40866-67b0-4dd9-be15-1b113d5a17b6.png) 
   
   
   //Qr코드가 필요하시다면 Qr코드를 읽은 후 해당 QR코드의 방식대로 생성해주세요 [QR Code Generator] (#https://goqr.me/#)
   
   2. 생성된 List_View 클릭
   3. 블루투스 On
   4. 장치이름, 시리얼 번호, 블루투스 정보 확인하고 Server측과 일치하다면 Bluetooth Connect 클릭
   5. 정상적으로 연결 되면 Client Dialog에 IMU데이터 수신

## 동작 설명

Server의 IMU 데이터를 저장하는 코드

      boolean indicate = (RX_Characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;
      RX_Characteristic.setValue(datas); // 20byte limit
      mGattServer.notifyCharacteristicChanged(mBluetoothDevice, RX_Characteristic, indicate);
      Log.d(TAG, "Send byte[] imu datas");
 
 두 기기가 연결 되면 Client 측에서 onServicesDiscovered 동작하여 Server의 characteristic 찾아 setCharacteristicNotification 과 BluetoothGattDescriptor 설정 
  
    // Set CharacteristicNotification
            BluetoothGattCharacteristic cmd_characteristic = BluetoothUtils.findCharacteristic(bleGatt, Rx);
            _gatt.setCharacteristicNotification(cmd_characteristic, true);

            // 리시버 설정
            BluetoothGattDescriptor descriptor = cmd_characteristic.getDescriptor(CCCD);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean success = _gatt.writeDescriptor(descriptor);
            if (success) {
                Log.e(TAG, "writeCharacteristic success");
            } else {
                Log.e(TAG, "writeCharacteristic fail");
            }

 
  Server의 IMU데이터가 바뀔때 마다 Client측의 onCharacteristicChanged 호출 & BroadcastReceiver로 전달 되어 0.5초마다 업데이트됨
 
