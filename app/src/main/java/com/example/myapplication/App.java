package com.example.myapplication;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class App extends Application {

    // Вставьте ключ сюда
    private final String API_KEY = "94ac0fbb-0ad5-4441-a1cc-3fa24a521915";

    @Override
    public void onCreate() {
        super.onCreate();

        // Настраиваем MapKit один раз при запуске приложения
        MapKitFactory.setApiKey(API_KEY);
        MapKitFactory.initialize(this);
    }
}