package com.example.myapplication;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class App extends Application {


    private final String API_KEY = "86ce15a9-739d-4974-9fb5-363951c6a0eb";

    @Override
    public void onCreate() {
        super.onCreate();


        MapKitFactory.setApiKey(API_KEY);
        MapKitFactory.initialize(this);
    }
}