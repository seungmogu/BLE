package com.example.ojt2.Central;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import android.util.Log;
import android.view.View;

import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.ojt2.R;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import static com.example.ojt2.Constants.REQUEST_ENABLE_BT;
import static com.example.ojt2.Constants.REQUEST_FINE_LOCATION;


public class Qrcode_Information extends AppCompatActivity {
    private final String TAG = Qrcode_Information.class.getSimpleName();

    private TextView Dialog_TextView_Accelometer_x;
    private TextView Dialog_TextView_Accelometer_y;
    private TextView Dialog_TextView_Accelometer_z;
    private TextView Dialog_TextView_Gyro_x;
    private TextView Dialog_TextView_Gyro_y;
    private TextView Dialog_TextView_Gyro_z;


    private TextView Device_Name, Serial_Num, Bluetooth_Address;
    private Button Bluetooth_Connect;

    private Dialog dialog;
    private int data_value = 0; //imu_data의 데이터 값들을 확인 하기위한 함수

    private BluetoothAdapter mBtAdapter = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrcode_page);

        //view의 아이디 선언
        Device_Name = findViewById(R.id.TextView_DeviceName);
        Serial_Num = findViewById(R.id.TextView_SerialNum);
        Bluetooth_Address = findViewById(R.id.TextView_Address);
        Bluetooth_Connect = findViewById(R.id.Button_BluetoothConnect);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) { //블루투스 사용 가능 여부 판별
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestLocationPermission();
        }
        CentralManager.getInstance(this).initialize();
        Service_init();
        initView(); //아이디와 버튼 선언


        Bluetooth_Connect.setOnClickListener(new View.OnClickListener() { //연결 버튼을 누를때
            @Override
            public void onClick(View v) {

                CentralManager.getInstance(Qrcode_Information.this).startScan();
                showToast("검색 중...");
            }
        });
    }



    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { //todo 이 부분의 onReceive는 동작 합니다
            String action = intent.getAction();

            if(action.equals(CentralManager.ACTION_GATT_CONNECTED)){
                Log.d(TAG, "ACTION_GATT_CONNECTED");
                showToast("연결 성공!");
                showPrinterPickDialog();
            }

            if (action.equals(CentralManager.ACTION_GATT_SERVICES_DISCOVERED)) {
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            }

            if (action.equals(CentralManager.ACTION_GATT_DISCONNECTED)){
                finish();
                dialog.dismiss();
            }

            if (action.equals(CentralManager.DEVICE_DOES_NOT_SUPPORT_UART)){
                Toast.makeText(Qrcode_Information.this, "Device doesn't support UART. Disconnecting", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                finish();
            }

            if(action.equals(CentralManager.ACTION_DATA_AVAILABLE)){
                final byte[] txValue = intent.getByteArrayExtra(CentralManager.EXTRA_DATA);
                try{
                    String text = new String(txValue, StandardCharsets.UTF_8);
                    update_data(text);
                    Log.d(TAG, text);
                }catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }

        }
    };

    private void update_data(String data){ //지정된 데이터를  텍스트뷰에 옮김

        switch (data_value){ //0 1
            case 0:
                Dialog_TextView_Accelometer_x.setText(data);
                data_value++;
                break;
            case 1:
                Dialog_TextView_Accelometer_y.setText(data);
                data_value++;
                break;
            case 2:
                Dialog_TextView_Accelometer_z.setText(data);
                data_value++;
                break;
            case 3:
                Dialog_TextView_Gyro_x.setText(data);
                data_value++;
                break;
            case 4:
                Dialog_TextView_Gyro_y.setText(data);
                data_value++;
                break;
            case 5:
                Dialog_TextView_Gyro_z.setText(data);
                data_value = 0;
                break;
        }
        Log.e(TAG, String.valueOf(data_value));
    }

    private void showPrinterPickDialog() { //custom dialog띄움
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.device1_imu_data);

        find_Ids(dialog);
        dialog.setCancelable(false); //뒤로가기 X
        dialog.show();

        dialog.findViewById(R.id.bt_request_characteristics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CentralManager.getInstance(Qrcode_Information.this).readCharacteristic();
                Log.d(TAG, "bt_request_characteristics Click");
            }
        });

        dialog.findViewById(R.id.bt_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CentralManager.getInstance(Qrcode_Information.this).Bluetooth_off();
                CentralManager.getInstance(Qrcode_Information.this).close();
                dialog.dismiss();
            }
        });

    }

    private void find_Ids(Dialog dialog) {  //xml안에 아이디를 초기화
        Dialog_TextView_Accelometer_x = dialog.findViewById(R.id.TextView_Accelometer_x);
        Dialog_TextView_Accelometer_y = dialog.findViewById(R.id.TextView_Accelometer_y);
        Dialog_TextView_Accelometer_z = dialog.findViewById(R.id.TextView_Accelometer_z);

        Dialog_TextView_Gyro_x = dialog.findViewById(R.id.TextView_Gyro_x);
        Dialog_TextView_Gyro_y = dialog.findViewById(R.id.TextView_Gyro_y);
        Dialog_TextView_Gyro_z = dialog.findViewById(R.id.TextView_Gyro_z);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CentralManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(CentralManager.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(CentralManager.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(CentralManager.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(CentralManager.DEVICE_DOES_NOT_SUPPORT_UART);

        return intentFilter;
    }


    /**
     * 안드로이드 권한 설정 결과
     * 블루투스는 위치 권한을 필요로 함.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { //위치 정보 허용 펄미션 결과
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(Qrcode_Information.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            showToast("블루투스 On");
        }
    }

    private void initView(){
        Intent intent = getIntent(); //기기 이름 정보 가져옴
        String Device = intent.getStringExtra("기기 이름");

        Show_TextView(Device); //텍스트 뷰에 데이터 저장
        CentralManager.getInstance(Qrcode_Information.this).setMAC_ADDR(Bluetooth_Address.getText().toString()); //기기 주소 setter
    }


    /**
     * Request BLE enable
     * 블루투스 기능을 켠다.
     */
    private void requestEnableBLE() {
        Intent ble_enable_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(ble_enable_intent, REQUEST_ENABLE_BT);
    }

    /**
     * Request Fine Location permission
     * 위치 권한을 안내한다.
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(Qrcode_Information.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        // 위치정보 설정 Intent
      //  startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    /**
     * 토스트를 표시한다.
     * @param message
     */
    private void showToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Toast.makeText(Qrcode_Information.this, message, Toast.LENGTH_SHORT).show();
            }
        };
        handler.sendEmptyMessage(1);
    }



    private void Show_TextView(String Device){ //qr코드 결과 값을 리스트 뷰에 저장
        SharedPreferences device_result = getSharedPreferences("device_result", MODE_PRIVATE);
        Device_Name.setText(device_result.getString(Device + "Name", ""));
        Serial_Num.setText(device_result.getString(Device + "Serial", ""));
        Bluetooth_Address.setText(device_result.getString(Device + "Address", ""));

    }

    private void Service_init(){ //broadcast 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onClick - BT not enabled yet");
            showToast("블루투스 켜주세요");
            requestEnableBLE();
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestLocationPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CentralManager.getInstance(this).close();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}
