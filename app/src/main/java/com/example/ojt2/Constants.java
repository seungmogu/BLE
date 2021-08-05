package com.example.ojt2;

import java.util.UUID;

public class Constants {

    // used to identify adding bluetooth names
    public final static int REQUEST_ENABLE_BT = 3054;
    // used to request fine location permission
    public final static int REQUEST_FINE_LOCATION = 3055;
    // scan period in milliseconds
    public final static int SCAN_PERIOD = 10000;

    //사용자 BLE UUID Service/Rx/Tx
    public static String SERVICE_STRING = "CB660002-4339-FF22-A1ED-DEBFED27BDB4";
    public static final UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

    public static String Rx = "CB660004-4339-FF22-A1ED-DEBFED27BDB4";
    public static String Tx = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    //CCCD 고정
    public static final UUID CCCD = UUID.fromString("00005609-0000-1001-8080-00705c9b34cb");
    public static final UUID CUD = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");

}
