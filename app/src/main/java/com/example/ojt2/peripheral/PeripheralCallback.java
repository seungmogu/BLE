package com.example.ojt2.peripheral;

public interface PeripheralCallback {

    void requestEnableBLE();

    void onToast(final String message);
}
