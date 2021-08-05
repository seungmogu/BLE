package com.example.ojt2.peripheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import static com.example.ojt2.Central.CentralManager.ACTION_GATT_CONNECTED;
import static com.example.ojt2.Central.CentralManager.ACTION_GATT_DISCONNECTED;
import static com.example.ojt2.Constants.CCCD;
import static com.example.ojt2.Constants.Rx;
import static com.example.ojt2.Constants.SERVICE_STRING;
import static com.example.ojt2.Constants.SERVICE_UUID;
import static com.example.ojt2.Constants.Tx;

public class Peripheral_Manager {

    private final String TAG = Peripheral_Manager.class.getSimpleName();

    private Context mContext;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattServer mGattServer;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothLeAdvertiser mAdvertiser;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGattCharacteristic TX_Characteristic;
    private BluetoothGattCharacteristic RX_Characteristic;

    private PeripheralCallback listener;

    private void broadcastUpdate(String action) { //todo 이 함수측이 인텐트 액션을 브로드캐스트 리시버에다 송신하는 역할을 합니다
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    public void setCallBack(PeripheralCallback listener) { //인터페이스로 콜백 설정
        this.listener = listener;
    }

    public void initServer(){
        Log.d(TAG, "initServer =================================== IN");

        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        /**
         * 불루투스를 사용할 수 있는지 체크.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            listener.requestEnableBLE();
            return;
        }

        mBluetoothGattService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY); //서비스와 특성들을 추가

        //add a read characteristic.
        TX_Characteristic = new BluetoothGattCharacteristic(UUID.fromString(Tx), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //add a descriptor
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(CCCD,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        TX_Characteristic.addDescriptor(descriptor);

        //add a write characteristic.
        RX_Characteristic = new BluetoothGattCharacteristic(UUID.fromString(Rx),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        RX_Characteristic.addDescriptor(new BluetoothGattDescriptor(CCCD, (BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ)));
        RX_Characteristic.setValue(new byte[]{0, 0});

        mBluetoothGattService.addCharacteristic(TX_Characteristic);
        mBluetoothGattService.addCharacteristic(RX_Characteristic);

        startAdvertising(); //광고 시작
        startServer(); //서버 시작
    }


    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            listener.onToast("Failed to create advertiser");
            return;
        }

        AdvertiseSettings advSettings = new AdvertiseSettings.Builder() //광고 모드 설정
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();

        AdvertiseData advData = new AdvertiseData.Builder() //서비스 uuid에 전송 전력레벨 포함
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid.fromString(SERVICE_STRING))
                .build();

        AdvertiseData advScanResponse = new AdvertiseData.Builder() //디바이스 이름을 포함
                .setIncludeDeviceName(true)
                .build();

        mAdvertiser.startAdvertising(advSettings, advData, advScanResponse, mAdvCallBack);
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mAdvertiser == null)
            return;

        mAdvertiser.stopAdvertising(mAdvCallBack);
    }

    /**
     * Gatt Server 를 생성한다.
     */
    private void startServer() {
        mGattServer = mBluetoothManager.openGattServer(mContext, bluetoothGattServerCallback);
        if (mGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            listener.onToast("Unable to create GATT server");
            return;
        }

        mGattServer.addService(mBluetoothGattService);
    }

    /**
     * Advertise Callback
     */
    private final AdvertiseCallback mAdvCallBack = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e(TAG, "AdvertiseCallback onStartSuccess");
            listener.onToast("GattServer onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "AdvertiseCallback onStartFailure");
            listener.onToast("GattServer onStartFailure");
        }
    };

  public void sendData(byte[] datas) {
      /**
       * 테스트중 아래의 오류가 발생한적이 있다.
       * java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String android.bluetooth.BluetoothDevice.getAddress()' on a null object reference
       * if 문으로 체크해보자. **/
      if (mBluetoothDevice == null) {
          Log.e(TAG, "BluetoothDevice is null");
          return;
      } else if (mBluetoothDevice.getAddress() == null) {
          Log.e(TAG, "GattServer lost device address");
          return;
      } else if (mGattServer == null) {
          Log.e(TAG, "GattServer is null");
          return;
      }
      boolean indicate = (RX_Characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;
      RX_Characteristic.setValue(datas); // 20byte limit
      mGattServer.notifyCharacteristicChanged(mBluetoothDevice, RX_Characteristic, indicate);
      Log.d(TAG, "Send byte[] imu datas");
    }

    /**
     * Gatt Server Callback
     */
    BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {


        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG, "BluetoothGattServerCallback onDescriptorWriteRequest");
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            // now tell the connected device that this was all successfull
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, String.format("onCharacteristicReadRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.e(TAG, "BluetoothGattServerCallback onConnectionStateChange");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevice = device;
                    listener.onToast("GattServer STATE_CONNECTED");
                    broadcastUpdate(ACTION_GATT_CONNECTED);

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevice = null;
                    listener.onToast("GattServer STATE_DISCONNECTED");
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                }

            } else {
                mBluetoothDevice = null;
                listener.onToast("GattServer GATT_FAILURE");
            }


        }
    };


    /**
     * GATT Server 를 끈다.
     */
    private void stopServer() {
        if (mGattServer == null)
            return;

        mGattServer.close();
    }

    public void Bluetooth_off(){
        mBluetoothAdapter.disable();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        listener.onToast("서버 종료");
        if (mBluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }
    }
}
