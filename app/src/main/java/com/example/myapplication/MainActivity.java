package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.myapplication.models.Review; // Ваша модель отзыва
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.api.ApiService;
import com.example.myapplication.api.RetrofitClient;
import com.example.myapplication.models.LocationSeat;
import com.example.myapplication.models.LocationSeatCreate;
import com.example.myapplication.models.ReviewCreate;
import com.example.myapplication.utils.SharedPreferencesManager;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.VisibleRegion;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private Button myReviewsButton;
    private Button locationButton;
    private Button myLocationsButton;
    private Button loginButton;


    private PlacemarkMapObject temporaryMarker;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int ADD_MARKER_REQUEST = 101;
    private static final int LOGIN_REQUEST = 102;

    private MapObjectCollection mapObjects;
    private Point selectedPoint;


    private ApiService apiService;
    private SharedPreferencesManager prefsManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    //Слушатель камеры для подгрузки меток ---
    private final CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(@NonNull Map map,
                                            @NonNull CameraPosition cameraPosition,
                                            @NonNull CameraUpdateReason cameraUpdateReason,
                                            boolean finished) {

            if (finished) {
                loadMarkersInVisibleRegion();
            }
        }
    };
    private final com.yandex.mapkit.map.InputListener mapInputListener = new com.yandex.mapkit.map.InputListener() {
        @Override
        public void onMapTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {

            if (selectedPoint != null) {
                removeAllTemporaryMarkers();
                selectedPoint = null;
            }
        }

        @Override
        public void onMapLongTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {
            // Логируем
            Log.d("MAP_INPUT", "Long tap detected: " + point.getLatitude());

            // Сохраняем точку
            selectedPoint = new Point(point.getLatitude(), point.getLongitude());
            showTemporaryMarker(selectedPoint);
            openAddMarkerActivity();
        }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        try {
            mapView = findViewById(R.id.mapview);
            locationButton = findViewById(R.id.locationButton);
            myLocationsButton = findViewById(R.id.myLocationsButton);
            loginButton = findViewById(R.id.loginButton);
            myReviewsButton = findViewById(R.id.myReviewsButton);


            if (mapView == null) {
                Log.e("INIT", "MapView not found!");
                return;
            }

            prefsManager = new SharedPreferencesManager(this);
            apiService = RetrofitClient.getApiService(this);

            // Получаем коллекцию для меток
            mapObjects = mapView.getMap().getMapObjects().addCollection();

        } catch (Exception e) {
            Log.e("MAPKIT", "Ошибка инициализации View", e);
            Toast.makeText(this, "Ошибка интерфейса", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        setupMap();
        setupButtons();
        setupMapLongTapListener();
        checkAuthStatus();


    }

    private void setupMap() {
        // слушатель
        mapView.getMap().addCameraListener(cameraListener);

        // Двигаем камеру (Нижний Тагил)
        mapView.getMap().move(
                new CameraPosition(new Point(57.92149, 59.981156), 14.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1f), // Анимация 1 сек
                null
        );
    }


    // метод загрузки меток для видимой области
    private void loadMarkersInVisibleRegion() {
        // пользователь ставит точку, не обновляем карту, чтобы не сбить процесс
        if (selectedPoint != null) return;

        // готова ли карта???
        if (mapView == null || mapView.getMap() == null) return;

        VisibleRegion region = mapView.getMap().getVisibleRegion();

        // Считаем границы экрана
        double minLat = Math.min(region.getBottomLeft().getLatitude(), region.getBottomRight().getLatitude());
        double maxLat = Math.max(region.getTopLeft().getLatitude(), region.getTopRight().getLatitude());
        double minLon = Math.min(region.getBottomLeft().getLongitude(), region.getTopLeft().getLongitude());
        double maxLon = Math.max(region.getBottomRight().getLongitude(), region.getTopRight().getLongitude());

        // ЛОГИРОВАНИЕ: какие координаты уходят
        Log.d("MAP_DEBUG", String.format("Запрос меток: lat[%.4f - %.4f], lon[%.4f - %.4f]", minLat, maxLat, minLon, maxLon));

        Call<List<LocationSeat>> call = apiService.getLocations(
                minLat, maxLat, minLon, maxLon, null, null
        );

        call.enqueue(new Callback<List<LocationSeat>>() {
            @Override
            public void onResponse(Call<List<LocationSeat>> call, Response<List<LocationSeat>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LocationSeat> locations = response.body();
                    Log.d("MAP_DEBUG", "Пришло меток с сервера: " + locations.size());


                    removeAllMarkersExceptTemporary();

                    for (LocationSeat loc : locations) {
                        addMarkerToMap(loc);
                    }
                } else {
                    Log.e("MAP_ERROR", "Ошибка сервера: " + response.code());

                    try {
                        if(response.errorBody() != null) Log.e("MAP_ERROR", response.errorBody().string());
                    } catch (Exception e) {}
                }
            }

            @Override
            public void onFailure(Call<List<LocationSeat>> call, Throwable t) {
                Log.e("MAP_FAILURE", "Нет связи с сервером: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Ошибка загрузки карты", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Обновленный метод добавления метки
    private void addMarkerToMap(LocationSeat location) {
        Point point = new Point(location.getCordX(), location.getCordY());
        PlacemarkMapObject marker = mapObjects.addPlacemark(point);


        Bitmap iconBitmap = createBitmapFromVector(R.drawable.circle_marker);
        if (iconBitmap != null) {
            marker.setIcon(ImageProvider.fromBitmap(iconBitmap));
        } else {

            marker.setIcon(ImageProvider.fromResource(this, R.drawable.ic_marker_default));
        }

        // Данные и обработчик клика
        marker.setUserData(location);
        marker.addTapListener((mapObject, p) -> {
            LocationSeat loc = (LocationSeat) mapObject.getUserData();
            showLocationInfoDialog(loc);
            return true;
        });
    }

    // Конвертация XML (vector) в Bitmap для MapKit
    private Bitmap createBitmapFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) return null;


        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();


        if (width <= 0) width = 48;
        if (height <= 0) height = 48;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private void removeAllMarkersExceptTemporary() {
        if (temporaryMarker != null) {
            Point tempPoint = temporaryMarker.getGeometry();
            mapObjects.clear();

            temporaryMarker = mapObjects.addPlacemark(tempPoint);
            temporaryMarker.setIcon(ImageProvider.fromResource(this, R.drawable.ic_marker_temp)); // Убедитесь что этот ресурс есть
            temporaryMarker.setOpacity(0.7f);
            temporaryMarker.setUserData("TEMP_MARKER");
        } else {
            mapObjects.clear();
        }
    }



    private void setupButtons() {
        locationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getSystemLocation();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        });

        myLocationsButton.setOnClickListener(v -> {
            if (prefsManager.isLoggedIn()) {

                loadMyLocations();
            } else {
                Toast.makeText(this, "Войдите в систему", Toast.LENGTH_SHORT).show();
                openLoginActivity();
            }
        });

        loginButton.setOnClickListener(v -> {
            if (prefsManager.isLoggedIn()) showLogoutDialog();
            else openLoginActivity();
        });
        myReviewsButton.setOnClickListener(v -> {
            if (!prefsManager.isLoggedIn()) {
                Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show();
                return;
            }

            String token = prefsManager.getAuthToken();
            apiService.getMyReviews("Bearer " + token).enqueue(new Callback<List<Review>>() {
                @Override
                public void onResponse(Call<List<Review>> call, Response<List<Review>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Review> reviews = response.body();

                        // Открываем наш новый диалог
                        MyReviewsDialog dialog = new MyReviewsDialog(MainActivity.this, reviews, apiService, prefsManager);
                        dialog.show();
                    } else {
                        Toast.makeText(MainActivity.this, "Не удалось загрузить отзывы", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<Review>> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }



    private void setupMapLongTapListener() {

        mapView.getMap().addInputListener(mapInputListener);
    }

    private void showTemporaryMarker(Point point) {
        removeAllTemporaryMarkers();
        temporaryMarker = mapObjects.addPlacemark(point);


        temporaryMarker.setIcon(ImageProvider.fromResource(this, R.drawable.ic_marker_temp));
        temporaryMarker.setOpacity(0.7f);
        temporaryMarker.setUserData("TEMP_MARKER");
    }

    private void removeAllTemporaryMarkers() {
        if (temporaryMarker != null) {

            try {

                if (temporaryMarker.isValid()) {
                    mapObjects.remove(temporaryMarker);
                }
            } catch (Exception e) { Log.e("MAP", "Ошибка удаления маркера"); }
            temporaryMarker = null;
        }
    }

    private void checkAuthStatus() {
        if (prefsManager.isLoggedIn()) {
            loginButton.setText("Выйти");
            myLocationsButton.setEnabled(true);
            myReviewsButton.setVisibility(View.VISIBLE); // Показываем
        } else {
            loginButton.setText("Войти");
            myLocationsButton.setEnabled(false);
            myReviewsButton.setVisibility(View.GONE); // Скрываем
        }
    }

    private void showLogoutDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы действительно хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    prefsManager.clear();
                    checkAuthStatus();
                    Toast.makeText(this, "Вы вышли", Toast.LENGTH_SHORT).show();
                    loadMarkersInVisibleRegion();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void getSystemLocation() {
        android.location.LocationManager lm = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm != null && lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            try {
                android.location.Location loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                if (loc != null) {
                    Point p = new Point(loc.getLatitude(), loc.getLongitude());
                    mapView.getMap().move(new CameraPosition(p, 16.0f, 0.0f, 0.0f),
                            new Animation(Animation.Type.SMOOTH, 1f), null);
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Нет прав", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAddMarkerActivity() {
        if (selectedPoint != null) {
            Intent intent = new Intent(MainActivity.this, AddMarkerActivity.class);
            intent.putExtra("latitude", selectedPoint.getLatitude());
            intent.putExtra("longitude", selectedPoint.getLongitude());
            startActivityForResult(intent, ADD_MARKER_REQUEST);
        }
    }

    private void openLoginActivity() {
        startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), LOGIN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOGIN_REQUEST && resultCode == RESULT_OK) {
            checkAuthStatus();
        }

        if (requestCode == ADD_MARKER_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {

                double lat = data.getDoubleExtra("latitude", 0);
                double lon = data.getDoubleExtra("longitude", 0);
                String name = data.getStringExtra("name");

                String desc = data.getStringExtra("description");
                String addr = data.getStringExtra("address");
                int type = data.getIntExtra("type", 1);
                int status = data.getIntExtra("status", 1);
                String uriString = data.getStringExtra("image_uri");
                Uri imageUri = (uriString != null) ? Uri.parse(uriString) : null;

                boolean hasReview = data.getBooleanExtra("has_review", false);
                ReviewCreate review = null;
                if(hasReview) {
                    review = new ReviewCreate(
                            data.getIntExtra("rate", 5),
                            data.getIntExtra("pollution_id", 1),
                            data.getIntExtra("condition_id", 1),
                            data.getIntExtra("material_id", 1),
                            data.getIntExtra("seating_positions", 1)
                    );
                }

                createLocationOnServer(lat, lon, name, desc, addr, type, status, review, imageUri);
            }

            removeAllTemporaryMarkers();
            selectedPoint = null;


            loadMarkersInVisibleRegion();
        }
    }

    private void createLocationOnServer(double lat, double lon, String name,
                                        String description, String address,
                                        int type, int status, ReviewCreate review,
                                        Uri imageUri) { // <--- Аргумент imageUri

        LocationSeatCreate locationData = new LocationSeatCreate(name, description, address, type, lat, lon, status);
        if (review != null) locationData.setFirstReview(review);

        String token = prefsManager.getAuthToken();
        if (token == null) return;

        apiService.createLocation("Bearer " + token, locationData).enqueue(new Callback<LocationSeat>() {
            @Override
            public void onResponse(Call<LocationSeat> call, Response<LocationSeat> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LocationSeat createdLocation = response.body();

                    Toast.makeText(MainActivity.this, "Локация добавлена!", Toast.LENGTH_SHORT).show();


                    addMarkerToMap(createdLocation);


                    if (imageUri != null) {

                        uploadImageToServer(createdLocation.getId(), imageUri, token);
                    }

                } else {
                    Toast.makeText(MainActivity.this, "Ошибка создания: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<LocationSeat> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Метод загрузки картинки
    private void uploadImageToServer(int locationId, Uri imageUri, String token) {
        try {

            File file = createTempFileFromUri(imageUri);


            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);


            apiService.uploadPicture("Bearer " + token, locationId, body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Фото загружено!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("UPLOAD", "Ошибка загрузки фото: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    Log.e("UPLOAD", "Ошибка сети при загрузке фото", t);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show();
        }
    }


    private File createTempFileFromUri(Uri uri) throws java.io.IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = new File(getCacheDir(), "upload_image_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private void showLocationInfoDialog(LocationSeat location) {
        // Мы создаем экземпляр ВАШЕГО НОВОГО КЛАССА
        LocationInfoDialog dialog = new LocationInfoDialog(
                MainActivity.this,           // Context
                location,                    // Объект места
                apiService,                  // Retrofit сервис
                prefsManager,                // Менеджер настроек
                new LocationInfoDialog.OnActionListener() { // Слушатель событий
                    @Override
                    public void onLocationDeleted() {
                        // Если удалили - обновляем карту
                        loadMarkersInVisibleRegion();
                    }

                    @Override
                    public void onAddReviewClick(LocationSeat loc) {
                        // Если нажали "Отзыв" - открываем окно отзыва
                        showAddReviewDialog(loc);
                    }
                }
        );

        // Показываем диалог
        dialog.show();
    }

    private void showAddReviewDialog(LocationSeat location) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


        RatingBar ratingBar = view.findViewById(R.id.ratingBarNew);
        Spinner spPollution = view.findViewById(R.id.spinnerPollution);
        Spinner spCondition = view.findViewById(R.id.spinnerCondition);
        Spinner spMaterial = view.findViewById(R.id.spinnerMaterial);
        Spinner spSeating = view.findViewById(R.id.spinnerSeating);

        Button btnSend = view.findViewById(R.id.btnSendReview);


        setupSpinner(spPollution, getResources().getStringArray(R.array.pollution_levels));
        setupSpinner(spCondition, getResources().getStringArray(R.array.condition_levels));
        setupSpinner(spMaterial, getResources().getStringArray(R.array.material_types));
        setupSpinner(spSeating, getResources().getStringArray(R.array.seating_positions));

        btnSend.setOnClickListener(v -> {
            int rate = (int) ratingBar.getRating();
            if (rate == 0) {
                Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show();
                return;
            }


            ReviewCreate review = new ReviewCreate(
                    rate,
                    spPollution.getSelectedItemPosition() + 1,
                    spCondition.getSelectedItemPosition() + 1,
                    spMaterial.getSelectedItemPosition() + 1,
                    spSeating.getSelectedItemPosition() + 1
            );



            review.setLocationId(location.getId());
            sendReviewToServer(review, dialog);

        });

        dialog.show();
    }
    private void sendReviewToServer(ReviewCreate review, AlertDialog dialog) {
        String token = prefsManager.getAuthToken();
        if (token == null) return;


        Call<com.example.myapplication.models.Review> call = apiService.addReview("Bearer " + token, review);

        call.enqueue(new Callback<com.example.myapplication.models.Review>() {
            @Override
            public void onResponse(Call<com.example.myapplication.models.Review> call, Response<com.example.myapplication.models.Review> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Отзыв добавлен!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();


                    loadMarkersInVisibleRegion();
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.myapplication.models.Review> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupSpinner(Spinner spinner, String[] items) {
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private String getTypeName(int typeId) {
        switch (typeId) {
            case 1: return "Скамейка";
            case 2: return "Беседка";
            case 3: return "Парк";
            default: return "Неизвестно";
        }
    }

    private String getStatusName(int statusId) {
        switch (statusId) {
            case 1: return "Активно";
            case 2: return "На ремонте";
            case 3: return "Удалено";
            default: return "Временно недоступно";
        }
    }


    private void deleteLocation(int locationId, android.app.AlertDialog dialog) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены?")
                .setPositiveButton("Да", (d, w) -> {



                    dialog.dismiss(); // закрываем окно деталей
                    loadMarkersInVisibleRegion(); // обновляем карту
                    Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void loadMyLocations() {
        String token = prefsManager.getAuthToken();
        if (token == null) return;

        apiService.getMyLocations("Bearer " + token).enqueue(new Callback<List<LocationSeat>>() {
            @Override
            public void onResponse(Call<List<LocationSeat>> call, Response<List<LocationSeat>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LocationSeat> locations = response.body();

                    // Открываем диалог со списком
                    MyLocationsListDialog dialog = new MyLocationsListDialog(
                            MainActivity.this,
                            locations,
                            apiService,
                            prefsManager,
                            new MyLocationsListDialog.OnLocationClickListener() {
                                @Override
                                public void onLocationClick(LocationSeat location) {



                                    mapView.getMap().move(
                                            new com.yandex.mapkit.map.CameraPosition(
                                                    new com.yandex.mapkit.geometry.Point(location.getCordX(), location.getCordY()),
                                                    17.0f, 0.0f, 0.0f),
                                            new com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1f),
                                            null
                                    );


                                    showLocationInfoDialog(location);
                                }

                                @Override
                                public void onListUpdated() {
                                    // Если удалили место из списка, обновляем карту
                                    loadMarkersInVisibleRegion();
                                }
                            }
                    );
                    dialog.show();

                } else {
                    Toast.makeText(MainActivity.this, "Не удалось загрузить локации", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<LocationSeat>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    };

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();

        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onStop() {

        if (mapView != null) {
            mapView.onStop();
        }
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }


}