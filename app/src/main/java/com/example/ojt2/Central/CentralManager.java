package com.example.ojt2.Central;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.example.ojt2.Constants.CCCD;
import static com.example.ojt2.Constants.Rx;
import static com.example.ojt2.Constants.SCAN_PERIOD;
import static com.example.ojt2.Constants.SERVICE_UUID;

public class CentralManager{

    private final String TAG = CentralManager.class.getSimpleName();

    public static Context mContext;
    protected static volatile CentralManager sInstance = null;

    public final static String ACTION_GATT_CONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_UART";
    public final static String ACTION_DATA_AVAILABLE =
            "com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.nordicsemi.nrfUART.EXTRA_DATA";


    // ble adapter
    private BluetoothAdapter bleAdapter;
    private BluetoothManager mBluetoothManager;
    // flag for scanning
    private boolean isScanning = false;
    // flag for connection
    private boolean isConnected = false;
    // scan results
    private Map<String, BluetoothDevice> scanResults;
    // scan callback
    private ScanCallback scanCallback;
    // ble scanner
    private BluetoothLeScanner bleScanner;
    // scan handler
    private Handler scanHandler;

    // BLE Gatt
    private BluetoothGatt bleGatt;

    private static String MAC_ADDR; //기기의 MAC_address 찾기

    public void setMAC_ADDR(String MAC_ADDR) { //mac address setter
        CentralManager.MAC_ADDR = MAC_ADDR;
        Log.d(TAG, CentralManager.MAC_ADDR);
    }


    public CentralManager(Context context) {//singleton
        mContext = context.getApplicationContext();
    }

    public static CentralManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CentralManager(context);
        }

        return sInstance;
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bleAdapter = mBluetoothManager.getAdapter();
        if (bleAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Start BLE scan
     */
    public void startScan() {

        /*
         * 이미 연결된 상태라면 더이상 진행시키지 않는다.
         */

        if (isConnected)
            return;

        Log.d(TAG, "Scanning...");

        /*
         * 블루투스를 사용할 수 있는 상태인지 체크한다.
         */
        if (bleAdapter == null || !bleAdapter.isEnabled()) {
            Log.e(TAG, "Scanning Failed: ble not enabled");
            return;
        }
        bleScanner = bleAdapter.getBluetoothLeScanner();

         //// set scan filters
        // create scan filter list
        List<ScanFilter> filters = new ArrayList<>();
        // create a scan filter with device uuid
        ScanFilter scan_filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build();

        // add the filter to the list
        filters.add(scan_filter);

        //// scan settings
        // 저전력 스캔 모드
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        scanResults = new HashMap<>();
        scanCallback = new BLEScanCallback(scanResults);

        //// now ready to scan
        // start scan
        bleScanner.startScan(filters, settings, scanCallback);
        // set scanning flag
        isScanning = true;

        scanHandler = new Handler();
        scanHandler.postDelayed(this::stopScan, SCAN_PERIOD);
    }

    /**
     * Stop scanning
     */
    private void stopScan() {
        // check pre-conditions
        if (isScanning && bleAdapter != null && bleAdapter.isEnabled() && bleScanner != null) {
            // stop scanning
            bleScanner.stopScan(scanCallback);
            scanComplete();
        }
        // reset flags
        if (scanCallback != null)
            scanCallback = null;
        if (scanHandler != null)
            scanHandler = null;
        isScanning = false;
        // update the status

        Log.d(TAG, "scanning stopped");
    }
    /**
     * Handle scan results after scan stopped
     */
    private void scanComplete() {
        // check if nothing found
        if (scanResults.isEmpty()) {
            Log.e(TAG, "scan results is empty");
            return;
        }
        // loop over the scan results and connect to them
        for (String device_addr : scanResults.keySet()) {
            Log.d(TAG, "Found device: " + device_addr);
            // get device instance using its MAC address
            BluetoothDevice device = scanResults.get(device_addr);
             if (device_addr.equals(MAC_ADDR)) {
                 Log.d(TAG, "connecting device: " + device_addr);
                 // connect to the device
                 connectDevice(device);
             }
        }
    }


    /**
     * Connect to the ble device
     * @param _device
     */

    public void connectDevice(BluetoothDevice _device) {

        // update the status
        Log.d(TAG, "Connecting to " + _device.getAddress());
        final BluetoothDevice device = bleAdapter.getRemoteDevice(_device.getAddress());
        bleGatt = device.connectGatt(mContext, false, mGattCallback);

        Log.d(TAG, "Trying to create a new connection.");
    }

    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_FAILURE) {
                close();
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) { //기기와 연결되었을 때
                broadcastUpdate(ACTION_GATT_CONNECTED);
                isConnected = true;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        bleGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) { //기기와 연결이 끊어 졌을때
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                isConnected = false;
                close();
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt _gatt, int _status) {
            super.onServicesDiscovered(_gatt, _status);

            // check if the discovery failed
            if (_status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery failed, status: " + _status);
                return;
            }
            Log.e(TAG, "getDevice().getAddress() : " + _gatt.getDevice().getAddress() + ", " + _gatt.getDevice().getName());

            // find discovered characteristics
            List<BluetoothGattCharacteristic> matching_characteristics = BluetoothUtils.findBLECharacteristics(_gatt);
            for (BluetoothGattCharacteristic characteristic : matching_characteristics) {
                Log.e(TAG, "characteristic : " + characteristic.getUuid());
            }

            if (matching_characteristics.isEmpty()) {
                Log.e(TAG, "Unable to find characteristics");
                return;
            }
            // log for successful discovery
            Log.d(TAG, "Services discovery is successful");

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
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.d(TAG, "characteristic changed: " + characteristic.getUuid().toString());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            for(int i = 0; i < characteristic.getValue().length; i++)
                Log.e(TAG, "READ" + characteristic.getValue()[i]);
        }

    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is handling for the notification on TX Character of NUS service
        if (UUID.fromString(Rx).equals(characteristic.getUuid())) {

            // Log.d(TAG, String.format("Received TX: %d",characteristic.getValue() ));
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }


    public void readCharacteristic() {

        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter or bleGatt not initialized");
            return;
        }
        BluetoothGattCharacteristic cmd_characteristic = BluetoothUtils.findCharacteristic(bleGatt, Rx);
        if(cmd_characteristic == null) {
            Log.e(TAG, "읽어올 데이터가 없습니다");
        }
        Log.e(TAG, "central에서 읽기: "  + bleGatt.readCharacteristic(cmd_characteristic) );
    }


        /**
         * After using a given BLE device, the app must call this method to ensure resources are
         * released properly.
         */
    public void close() {
        if(bleGatt == null){
            return;
        }
        bleGatt.close();
        bleGatt = null;
        Log.w(TAG, "blegatt Close");
    }

    public void Bluetooth_off(){
        bleGatt.disconnect();
        bleAdapter.disable();
    }

    /**
     * BLE Scan Callback class
     */
    private class BLEScanCallback extends ScanCallback {
        private Map<String, BluetoothDevice> cb_scan_results;

        /**
         * Constructor
         * @param _scan_results
         */
        BLEScanCallback(Map<String, BluetoothDevice> _scan_results) {
            cb_scan_results = _scan_results;
        }

        @Override
        public void onScanResult(int _callback_type, ScanResult _result) {
            addScanResult(_result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> _results) {
            for (ScanResult result : _results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int _error) {
            Log.e(TAG, "BLE scan failed with code " + _error);
        }

        /**
         * Add scan result
         * @param _result
         */
        private void addScanResult(ScanResult _result) {

            // get scanned device
            BluetoothDevice device = _result.getDevice();
            // get scanned device MAC address
            String device_address = device.getAddress();
            // add the device to the result list
            if(!cb_scan_results.containsKey(device_address)){
                cb_scan_results.put(device_address, device);
                Log.e(TAG, "scan results device: " + device_address + ", " + device.getName());
            }
        }
    }
}
