package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.mapview.MapView;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private Button locationButton;
    private final String API_KEY = "";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private com.yandex.mapkit.location.LocationManager locationManager;
    private LocationListener locationListener;
    private boolean isLocationTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapKitFactory.setApiKey(API_KEY);
        MapKitFactory.initialize(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapview);
        locationButton = findViewById(R.id.locationButton);

        locationManager = MapKitFactory.getInstance().createLocationManager();

        setupMap();
        setupLocationListener();
        setupLocationButton();
    }

    private void setupMap() {
        mapView.getMap().setMapType(com.yandex.mapkit.map.MapType.VECTOR_MAP);

        mapView.getMap().move(
                new CameraPosition(
                        new Point(55.751574, 37.573856),
                        15.0f, 0.0f, 0.0f
                ),
                new Animation(Animation.Type.SMOOTH, 1f),
                null
        );
    }

    private void setupLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationUpdated(@NonNull Location location) {
                if (isLocationTracking) {
                    Point userLocation = location.getPosition();


                    if (userLocation.getLatitude() != 0 && userLocation.getLongitude() != 0) {
                        mapView.getMap().move(
                                new CameraPosition(
                                        userLocation,
                                        16.0f, 0.0f, 0.0f
                                ),
                                new Animation(Animation.Type.SMOOTH, 1f),
                                null
                        );

                        locationButton.setText("📍 Найдено");
                        isLocationTracking = false;


                        new Handler().postDelayed(() -> {
                            locationManager.unsubscribe(locationListener);
                        }, 1000);
                    }
                }
            }

            @Override
            public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {
                switch (locationStatus) {
                    case AVAILABLE:

                        break;
                    case NOT_AVAILABLE:
                        locationButton.setText("📍 GPS выкл");
                        Toast.makeText(MainActivity.this, "Включите GPS на устройстве", Toast.LENGTH_LONG).show();
                        isLocationTracking = false;
                        break;
                }
            }
        };
    }

    private void setupLocationButton() {
        locationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                getSystemLocation();

            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        });
    }

    private void getSystemLocation() {
        android.location.LocationManager systemLocationManager =
                (android.location.LocationManager) getSystemService(LOCATION_SERVICE);

        if (systemLocationManager != null &&
                systemLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {

            try {
                android.location.Location location =
                        systemLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);

                if (location != null) {
                    Point userLocation = new Point(location.getLatitude(), location.getLongitude());
                    moveToLocation(userLocation);
                    locationButton.setText("📍 Найдено");
                } else {
                    locationButton.setText("📍 Нет данных");
                    Toast.makeText(this, "Нет данных о местоположении", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Нет разрешения на доступ к локации", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "GPS не включен", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }
    private void moveToLocation(Point location) {
        mapView.getMap().move(
                new CameraPosition(
                        location,
                        16.0f, 0.0f, 0.0f
                ),
                new Animation(Animation.Type.SMOOTH, 1f),
                null
        );
    }
    @Override
    protected void onStop() {
        locationManager.unsubscribe(locationListener);
        isLocationTracking = false;

        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }
}
