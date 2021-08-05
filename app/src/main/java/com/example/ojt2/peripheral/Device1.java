package com.example.ojt2.peripheral;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import com.example.ojt2.Central.CentralManager;
import com.example.ojt2.R;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static com.example.ojt2.Constants.REQUEST_ENABLE_BT;

public class Device1 extends AppCompatActivity {

    private final String TAG = Device1.class.getSimpleName();

    private boolean onConnect = false;

    private TextView TextView_Accelometer_x, TextView_Accelometer_y, TextView_Accelometer_z,
            TextView_Gyro_x, TextView_Gyro_y, TextView_Gyro_z;

    private ToggleButton data_toggle;
    private boolean is_toggle_on = true; //토클 on off 감지

    //가속도 , 자이로 센서 매니저 선언
    private SensorManager mSensorManager = null;
    private SensorManager mGiroSensorManager = null;

    //가속도 센서 사용
    private SensorEventListener mAccLis;
    private Sensor mAccelometerSensor = null;

    //자이로 센서 선언
    private SensorEventListener mGyro;
    private Sensor mGyroScopeSensor = null;

    private Peripheral_Manager peripheral_manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device1_page);

        initView();
        Service_init();
        initServer();

        //자이로와 가속도 사용 선언
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGiroSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //가속도 사용
        mAccelometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccLis = new AccelometerListener();

        //회전 속도 사용
        mGyroScopeSensor = mGiroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGyro = new GiroScopeListener();

    }

    private void Service_init(){

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());


        peripheral_manager = new Peripheral_Manager();
        peripheral_manager.setmContext(this);
    }
        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { //todo device1의 onReceive는 동작하지 않습니다
            String action = intent.getAction();

            if(action.equals(CentralManager.ACTION_GATT_CONNECTED)){
                Log.d(TAG, "ACTION_GATT_CONNECTED");
                onConnect = true;
                //imu data 시작
                mSensorManager.registerListener(mAccLis, mAccelometerSensor, SensorManager.SENSOR_DELAY_UI); //가속도 센서
                mGiroSensorManager.registerListener(mGyro, mGyroScopeSensor, SensorManager.SENSOR_DELAY_UI); //자이로 센서
            }

            if(action.equals(CentralManager.ACTION_GATT_DISCONNECTED)){
                Log.d(TAG, "ACTION_GATT_DISCONNECTED");
                onConnect = false;
                finish();
            }

        }
    };



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CentralManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(CentralManager.ACTION_GATT_DISCONNECTED);

        return intentFilter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        is_toggle_on = false; //연결이 없어 지면 데이터 전송을 중단
        //서비스와 방송수신자 종료
        peripheral_manager.close();
        peripheral_manager.Bluetooth_off();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        //센서 출력 종료
        mSensorManager.unregisterListener(mAccLis);
        mGiroSensorManager.unregisterListener(mGyro);
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT)
           initServer();
    }


    /**
     * Gatt Server 시작.
     * Peripheral Callback 을 셋팅해준다.
     */
    private void initServer() {
        peripheral_manager.setCallBack(peripheralCallback);
        peripheral_manager.initServer();
    }

    @Override
    public void onBackPressed() {
        /**
         * 액티비티가 종료될때 서버를 종료시켜주기 위한 처리.
         */
        peripheral_manager.close();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                peripheral_manager.Bluetooth_off();
                finish();

            }
        }, 500);
    }

    /**
     * 불루투스 기능을 켠다.
     */
    private void requestEnableBLE() {
        Intent ble_enable_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(ble_enable_intent, REQUEST_ENABLE_BT);
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
                Toast.makeText(Device1.this, message, Toast.LENGTH_SHORT).show();
            }
        };
        handler.sendEmptyMessage(1);
    }

    /**
     * Peripheral Callback
     */
    PeripheralCallback peripheralCallback = new PeripheralCallback() {
        @Override
        public void requestEnableBLE() {
            Device1.this.requestEnableBLE();
        }


        @Override
        public void onToast(String message) {
            showToast(message);
        }
    };

    private void initView() {  //xml안에 아이디를 초기화
        TextView_Accelometer_x = findViewById(R.id.TextView_Accelometer_x);
        TextView_Accelometer_y = findViewById(R.id.TextView_Accelometer_y);
        TextView_Accelometer_z = findViewById(R.id.TextView_Accelometer_z);

        TextView_Gyro_x = findViewById(R.id.TextView_Gyro_x);
        TextView_Gyro_y = findViewById(R.id.TextView_Gyro_y);
        TextView_Gyro_z = findViewById(R.id.TextView_Gyro_z);

        data_toggle = findViewById(R.id.ToggleButton_imuData);

        send_data();
    }

    private void send_data(){ //토글 스위치로 데이터 전송하는 함수
            data_toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!onConnect) {
                        Toast.makeText(Device1.this, "블루투스 연결X", Toast.LENGTH_SHORT).show();
                        data_toggle.toggle();
                    }
                    else{
                        is_toggle_on = !data_toggle.getText().toString().equals("OFF");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (is_toggle_on) {
                                    try {
                                        Save_imuData();

                                        Thread.sleep(500);
                                        
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).start();//연결 중일때
                    }
                }
            });
    }

    private void Save_imuData(){
        peripheral_manager.sendData(TextView_Accelometer_x.getText().toString().getBytes(StandardCharsets.UTF_8));
        peripheral_manager.sendData(TextView_Accelometer_y.getText().toString().getBytes(StandardCharsets.UTF_8));
        peripheral_manager.sendData(TextView_Accelometer_z.getText().toString().getBytes(StandardCharsets.UTF_8));
        peripheral_manager.sendData(TextView_Gyro_x.getText().toString().getBytes(StandardCharsets.UTF_8));
        peripheral_manager.sendData(TextView_Gyro_y.getText().toString().getBytes(StandardCharsets.UTF_8));
        peripheral_manager.sendData(TextView_Gyro_z.getText().toString().getBytes(StandardCharsets.UTF_8));
    }

    private class GiroScopeListener implements SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent event) {
            TextView_Gyro_x.setText(String.format("%.4f", event.values[0]));
            TextView_Gyro_y.setText(String.format("%.4f", event.values[1]));
            TextView_Gyro_z.setText(String.format("%.4f", event.values[2]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class AccelometerListener implements SensorEventListener{
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            TextView_Accelometer_x.setText(String.format("%.4f", event.values[0]));
            TextView_Accelometer_y.setText(String.format("%.4f", event.values[1]));
            TextView_Accelometer_z.setText(String.format("%.4f", event.values[2]));

        }
    }

}
