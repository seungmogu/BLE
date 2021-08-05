package com.example.ojt2.Central;

public interface CentralCallback {
    void requestEnableBLE();

    void requestLocationPermission();

    void onToast(final String message);
}
