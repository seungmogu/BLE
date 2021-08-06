package com.example.ojt2.Central;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ojt2.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class Device2 extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> Data;
    private ArrayAdapter<String> Data_Adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device2_page);

        listView = findViewById(R.id.ListView_Device);
        Data = new ArrayList<String>();
        Data_Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Data);

        listView.setAdapter(Data_Adapter);
        listView.setOnItemClickListener(listener);
        listView.setOnItemLongClickListener(longClickListener);

        Button Button_Qrcode = (Button) findViewById(R.id.Button_Qrcode);
        IntentIntegrator qrScan;

        //intializing scan object
        qrScan = new IntentIntegrator(this);

        Button_Qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //scan option
                qrScan.setPrompt("Scanning...");
                qrScan.setOrientationLocked(false);
                qrScan.initiateScan();
            }
        });

    }

    AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) { //해당 아이템이 클릭됬을 시
            Intent intent = new Intent(Device2.this, Qrcode_Information.class);
            intent.putExtra("기기 이름", Data.get(position));
            startActivity(intent);
        }
    };

    AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
            alert.setTitle("해당 목록을 삭제하시겠습니까?");
            alert.setMessage(Data.get(position));
            alert.setPositiveButton("예", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    removePreferences(Data.get(position));
                    Data.remove(Data.get(position));
                    Data_Adapter.notifyDataSetChanged();
                    dialog.dismiss();
                }
            });
            alert.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(Device2.this, "취소", Toast.LENGTH_SHORT).show();
                }
            });
            alert.show();
            return true; //클릭 리스너랑 롱 클릭리스너를 따로 따로 실행
        }
    };

    private void savePreferences(String Name, String Serial, String Address){
        SharedPreferences device_result = getSharedPreferences("device_result", MODE_PRIVATE);//모든 data 값을 저장
        SharedPreferences.Editor editor = device_result.edit();
        editor.putString(Name + "Name", Name);
        editor.putString(Name + "Serial", Serial);
        editor.putString(Name + "Address", Address);
        editor.commit();

        SharedPreferences device_Name = getSharedPreferences("device_Name", MODE_PRIVATE);//이름 data 값을 저장
        SharedPreferences.Editor edit = device_Name.edit();
        edit.putString(Name + "Name", Name);
        edit.commit();
    }

    private void removePreferences(String Name){ //저장소에 이름 값을 삭제 데이터값은 어짜피 중복되니까 이름만 삭제
        SharedPreferences device_result = getSharedPreferences("device_Name", MODE_PRIVATE);//모든 data 값을 저장
        SharedPreferences.Editor editor = device_result.edit();
        editor.remove(Name + "Name");
        editor.commit();
    }

    private void Show_List(SharedPreferences preferences){
        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if(!Data.contains(entry.getValue().toString())){
                Data.add(entry.getValue().toString());
                Data_Adapter.notifyDataSetChanged();
            }
        }
    }

    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //qrcode 가 없으면
            if (result.getContents() == null) {
                Toast.makeText(Device2.this, "취소!", Toast.LENGTH_SHORT).show();
            } else {
                //qrcode 결과가 있으면
                Toast.makeText(Device2.this, "스캔완료!", Toast.LENGTH_SHORT).show();
                try {
                    JSONObject obj = new JSONObject(result.getContents()); //data를 json으로 변환
                    savePreferences(obj.getString("name"),  obj.getString("Serial"), obj.getString("Address")); //SharedPreferences에 값을 저장

                    if(Data.contains(obj.getString("name"))){ //qr코드가 중복 됬을때
                        Toast.makeText(Device2.this, "등록된 QR코드 정보 입니다", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        SharedPreferences device_Name = getSharedPreferences("device_Name", MODE_PRIVATE);//이름 data 값을 저장
        Show_List(device_Name);
    }
}

