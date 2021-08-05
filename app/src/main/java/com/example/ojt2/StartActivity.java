package com.example.ojt2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.ojt2.Central.Device2;
import com.example.ojt2.peripheral.Device1;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_page);

        Button Button_device1 = (Button) findViewById(R.id.Button_device1);
        Button Button_device2 = (Button) findViewById(R.id.Button_device2);

        Button_device1.setOnClickListener(new View.OnClickListener() { //기기 1
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, Device1.class);
                startActivity(intent);
            }
        });
        Button_device2.setOnClickListener(new View.OnClickListener() { //기기 2
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, Device2.class);
                startActivity(intent);
            }
        });


    }
}