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
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) { //?????? ???????????? ???????????? ???
            Intent intent = new Intent(Device2.this, Qrcode_Information.class);
            intent.putExtra("?????? ??????", Data.get(position));
            startActivity(intent);
        }
    };

    AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
            alert.setTitle("?????? ????????? ?????????????????????????");
            alert.setMessage(Data.get(position));
            alert.setPositiveButton("???", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    removePreferences(Data.get(position));
                    Data.remove(Data.get(position));
                    Data_Adapter.notifyDataSetChanged();
                    dialog.dismiss();
                }
            });
            alert.setNegativeButton("?????????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(Device2.this, "??????", Toast.LENGTH_SHORT).show();
                }
            });
            alert.show();
            return true; //?????? ???????????? ??? ?????????????????? ?????? ?????? ??????
        }
    };

    private void savePreferences(String Name, String Serial, String Address){
        SharedPreferences device_result = getSharedPreferences("device_result", MODE_PRIVATE);//?????? data ?????? ??????
        SharedPreferences.Editor editor = device_result.edit();
        editor.putString(Name + "Name", Name);
        editor.putString(Name + "Serial", Serial);
        editor.putString(Name + "Address", Address);
        editor.commit();

        SharedPreferences device_Name = getSharedPreferences("device_Name", MODE_PRIVATE);//?????? data ?????? ??????
        SharedPreferences.Editor edit = device_Name.edit();
        edit.putString(Name + "Name", Name);
        edit.commit();
    }

    private void removePreferences(String Name){ //???????????? ?????? ?????? ?????? ??????????????? ????????? ??????????????? ????????? ??????
        SharedPreferences device_result = getSharedPreferences("device_Name", MODE_PRIVATE);//?????? data ?????? ??????
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
            //qrcode ??? ?????????
            if (result.getContents() == null) {
                Toast.makeText(Device2.this, "??????!", Toast.LENGTH_SHORT).show();
            } else {
                //qrcode ????????? ?????????
                Toast.makeText(Device2.this, "????????????!", Toast.LENGTH_SHORT).show();
                try {
                    JSONObject obj = new JSONObject(result.getContents()); //data??? json?????? ??????
                    savePreferences(obj.getString("name"),  obj.getString("Serial"), obj.getString("Address")); //SharedPreferences??? ?????? ??????

                    if(Data.contains(obj.getString("name"))){ //qr????????? ?????? ?????????
                        Toast.makeText(Device2.this, "????????? QR?????? ?????? ?????????", Toast.LENGTH_SHORT).show();
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
        SharedPreferences device_Name = getSharedPreferences("device_Name", MODE_PRIVATE);//?????? data ?????? ??????
        Show_List(device_Name);
    }
}

