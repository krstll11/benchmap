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
    private Button locationButton;
    private Button myLocationsButton;
    private Button loginButton;

    // Временная метка (когда ставим точку для нового места)
    private PlacemarkMapObject temporaryMarker;

    private final String API_KEY = "b8cd571c-0d98-4f34-bba7-9df9644e9bfc"; // ВСТАВЬТЕ СЮДА ВАШ КЛЮЧ

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int ADD_MARKER_REQUEST = 101;
    private static final int LOGIN_REQUEST = 102;

    private MapObjectCollection mapObjects;
    private Point selectedPoint;

    // Для работы с API
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    // --- НОВОЕ: Слушатель камеры для подгрузки меток ---
    private final CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(@NonNull Map map,
                                            @NonNull CameraPosition cameraPosition,
                                            @NonNull CameraUpdateReason cameraUpdateReason,
                                            boolean finished) {
            // Загружаем метки только когда камера остановилась
            if (finished) {
                loadMarkersInVisibleRegion();
            }
        }
    };
    private final com.yandex.mapkit.map.InputListener mapInputListener = new com.yandex.mapkit.map.InputListener() {
        @Override
        public void onMapTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {
            // При обычном тапе можно сбрасывать выделение
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
        // 1. СНАЧАЛА Инициализация MapKit (ОБЯЗАТЕЛЬНО до setContentView)
        try {
            MapKitFactory.setApiKey(API_KEY);
            MapKitFactory.initialize(this);
        } catch (Exception e) {
            Log.e("MAPKIT", "Ошибка инициализации MapKit", e);
            // Если MapKit не завелся, нет смысла продолжать, приложение упадет дальше
            finish();
            return;
        }

        super.onCreate(savedInstanceState);

        // 2. Теперь загружаем Layout
        setContentView(R.layout.activity_main);

        // 3. Инициализация View и Сервисов
        try {
            mapView = findViewById(R.id.mapview);
            locationButton = findViewById(R.id.locationButton);
            myLocationsButton = findViewById(R.id.myLocationsButton);
            loginButton = findViewById(R.id.loginButton);

            // Проверка, что View найдены
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

        // 4. Настройка логики (ТОЛЬКО ПОСЛЕ того, как mapView найден)
        setupMap();
        setupButtons();
        setupMapLongTapListener();
        checkAuthStatus();


    }

    private void setupMap() {
        // Сначала добавляем слушатель
        mapView.getMap().addCameraListener(cameraListener);

        // Двигаем камеру (Москва). Animation.Type.SMOOTH вызовет событие "finished" в конце
        mapView.getMap().move(
                new CameraPosition(new Point(55.751574, 37.573856), 14.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 1f), // Анимация 1 сек
                null
        );
    }


    // --- НОВОЕ: Основной метод загрузки меток для видимой области ---
    private void loadMarkersInVisibleRegion() {
        // Если пользователь ставит точку, не обновляем карту, чтобы не сбить процесс
        if (selectedPoint != null) return;

        // Проверяем, готова ли карта
        if (mapView == null || mapView.getMap() == null) return;

        VisibleRegion region = mapView.getMap().getVisibleRegion();

        // Считаем границы экрана
        double minLat = Math.min(region.getBottomLeft().getLatitude(), region.getBottomRight().getLatitude());
        double maxLat = Math.max(region.getTopLeft().getLatitude(), region.getTopRight().getLatitude());
        double minLon = Math.min(region.getBottomLeft().getLongitude(), region.getTopLeft().getLongitude());
        double maxLon = Math.max(region.getBottomRight().getLongitude(), region.getTopRight().getLongitude());

        // ЛОГИРОВАНИЕ: Смотрим в Logcat, какие координаты уходят
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

                    // Очищаем и рисуем заново
                    removeAllMarkersExceptTemporary();

                    for (LocationSeat loc : locations) {
                        addMarkerToMap(loc);
                    }
                } else {
                    Log.e("MAP_ERROR", "Ошибка сервера: " + response.code());
                    // Попробуйте прочитать тело ошибки для отладки
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

        // --- ИЗМЕНЕНИЕ: Используем векторную иконку (круглую) ---
        // Если хотите разные иконки для типов, можно сделать switch внутри
        // Но для "круглого значка" используем R.drawable.circle_marker
        Bitmap iconBitmap = createBitmapFromVector(R.drawable.circle_marker);
        if (iconBitmap != null) {
            marker.setIcon(ImageProvider.fromBitmap(iconBitmap));
        } else {
            // Фолбек на старый метод, если битмап не создался
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

    // --- НОВОЕ: Конвертация XML (vector) в Bitmap для MapKit ---
    private Bitmap createBitmapFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) return null;

        // Проверка размеров, чтобы не было краша
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();

        // Если размеры не определились (например, 0 или -1), ставим дефолтные
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
            // Восстанавливаем временную метку
            temporaryMarker = mapObjects.addPlacemark(tempPoint);
            temporaryMarker.setIcon(ImageProvider.fromResource(this, R.drawable.ic_marker_temp)); // Убедитесь что этот ресурс есть
            temporaryMarker.setOpacity(0.7f);
            temporaryMarker.setUserData("TEMP_MARKER");
        } else {
            mapObjects.clear();
        }
    }

    // ---------------------------------------------------------
    // ОСТАЛЬНОЙ КОД БЕЗ ИЗМЕНЕНИЙ (Кнопки, Логика создания и т.д.)
    // ---------------------------------------------------------

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
                // При просмотре "Моих мест" можно временно отключить автозагрузку
                // Но для простоты оставим как есть, или загрузим и подвинем камеру
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
    }



    private void setupMapLongTapListener() {
        // ВАЖНО: Передаем нашу переменную mapInputListener, а не создаем новую через new!
        mapView.getMap().addInputListener(mapInputListener);
    }

    private void showTemporaryMarker(Point point) {
        removeAllTemporaryMarkers(); // Сначала удалим старую
        temporaryMarker = mapObjects.addPlacemark(point);

        // Тут можно использовать отдельную иконку для "Новой точки"
        temporaryMarker.setIcon(ImageProvider.fromResource(this, R.drawable.ic_marker_temp));
        temporaryMarker.setOpacity(0.7f);
        temporaryMarker.setUserData("TEMP_MARKER");
    }

    private void removeAllTemporaryMarkers() {
        if (temporaryMarker != null) {
            // Пытаемся удалить конкретный объект из коллекции
            try {
                // В MapKit иногда сложно удалить один объект, если коллекция была очищена
                // Но метод isValid проверяет, жив ли объект
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
        } else {
            loginButton.setText("Войти");
            myLocationsButton.setEnabled(false);
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
                    loadMarkersInVisibleRegion(); // Перезагрузить общую карту
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
                // Данные пришли - создаем на сервере
                double lat = data.getDoubleExtra("latitude", 0);
                double lon = data.getDoubleExtra("longitude", 0);
                String name = data.getStringExtra("name");
                // ... сбор остальных полей ...
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
            // В любом случае (успех или отмена) убираем временный маркер и сбрасываем точку
            removeAllTemporaryMarkers();
            selectedPoint = null;

            // Можно обновить карту
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

                    Toast.makeText(MainActivity.this, "Место добавлено!", Toast.LENGTH_SHORT).show();

                    // 1. Добавляем метку на карту
                    addMarkerToMap(createdLocation);

                    // 2. ЕСЛИ была выбрана картинка, начинаем загрузку
                    if (imageUri != null) {
                        // Запускаем загрузку, используя ID только что созданного места
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
            // 1. Превращаем URI в реальный файл во временной папке (кэше)
            // Это нужно, так как Android не дает прямого доступа к файлам галереи
            File file = createTempFileFromUri(imageUri);

            // 2. Готовим тело запроса для Retrofit
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            // 3. Отправляем
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

    // Вспомогательный метод для копирования файла из галереи в кэш приложения
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
        // 1. Создаем билдер диалога
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        // 2. Надуваем (Inflate) наш кастомный XML
        View view = getLayoutInflater().inflate(R.layout.dialog_location_info, null);
        builder.setView(view); // Устанавливаем этот View в диалог

        // 3. Создаем диалог (но пока не показываем)
        android.app.AlertDialog dialog = builder.create();

        // --- ПРИВЯЗКА ДАННЫХ (BINDING) ---

        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvDescription = view.findViewById(R.id.tvDescription);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        TextView tvType = view.findViewById(R.id.tvType);
        TextView tvStatus = view.findViewById(R.id.tvStatus);
        LinearLayout reviewsContainer = view.findViewById(R.id.reviewsContainer);
        Button btnDelete = view.findViewById(R.id.btnDelete);
        Button btnClose = view.findViewById(R.id.btnClose);

        // Заполняем тексты
        tvName.setText(location.getName());
        tvDescription.setText(location.getDescription() != null && !location.getDescription().isEmpty()
                ? location.getDescription() : "Описание отсутствует");
        tvAddress.setText("Адрес: " + (location.getAddress() != null ? location.getAddress() : "Не указан"));

        // Преобразуем ID типа и статуса в текст (вспомогательные методы ниже)
        tvType.setText("Тип: " + getTypeName(location.getType()));
        tvStatus.setText("Статус: " + getStatusName(location.getStatus()));

        // --- ЛОГИКА ОТЗЫВОВ ---
        // Очищаем контейнер перед добавлением (на случай переиспользования)
        reviewsContainer.removeAllViews();

        if (location.getReviews() != null && !location.getReviews().isEmpty()) {
            for (Review review : location.getReviews()) {
                // Создаем TextView для каждого отзыва программно
                TextView reviewView = new TextView(this);
                // Формируем текст отзыва (например: Оценка и Комментарий)
                String reviewText = "⭐ " + review.getRate();
                // Если есть текст отзыва, добавляем его (зависит от вашей модели Review)
                // reviewText += "\n" + review.getComment();

                reviewView.setText(reviewText);
                reviewView.setTextSize(14f);
                reviewView.setPadding(0, 8, 0, 8);

                reviewsContainer.addView(reviewView);

                // Добавляем разделитель (полоску)
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                reviewsContainer.addView(divider);
            }
        } else {
            TextView noReviews = new TextView(this);
            noReviews.setText("Отзывов пока нет");
            noReviews.setPadding(0, 10, 0, 10);
            reviewsContainer.addView(noReviews);
        }

        // --- ЛОГИКА КНОПКИ УДАЛИТЬ ---
        // Проверяем, является ли текущий пользователь автором метки
        int currentUserId = prefsManager.getUserId(); // Убедитесь, что в prefsManager есть этот метод
        if (currentUserId == location.getAuthorId()) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                // Логика удаления (нужен отдельный метод)
                deleteLocation(location.getId(), dialog);
            });
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        // Кнопка Закрыть
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Прозрачный фон диалога, чтобы углы были скругленные (если в XML корневой layout с фоном)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        Button btnAddReview = view.findViewById(R.id.btnAddReview);
        if (prefsManager.isLoggedIn()) {
            btnAddReview.setVisibility(View.VISIBLE);
            btnAddReview.setOnClickListener(v -> {
                dialog.dismiss(); // Закрываем окно деталей
                showAddReviewDialog(location); // Открываем окно добавления отзыва
            });
        } else {
            btnAddReview.setVisibility(View.GONE); // Скрываем, если не авторизован
        }

        dialog.show();
    }

    private void showAddReviewDialog(LocationSeat location) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Находим Views
        RatingBar ratingBar = view.findViewById(R.id.ratingBarNew);
        Spinner spPollution = view.findViewById(R.id.spinnerPollution);
        Spinner spCondition = view.findViewById(R.id.spinnerCondition);
        Spinner spMaterial = view.findViewById(R.id.spinnerMaterial);
        Spinner spSeating = view.findViewById(R.id.spinnerSeating);
        EditText etComment = view.findViewById(R.id.etComment); // Если есть в API
        Button btnSend = view.findViewById(R.id.btnSendReview);

        // Настройка спиннеров (данные как в AddMarkerActivity)
        setupSpinner(spPollution, new String[]{"Нет данных", "Чисто", "Немного мусора", "Грязно"});
        setupSpinner(spCondition, new String[]{"Нет данных", "Новое", "Потертое", "Сломано"});
        setupSpinner(spMaterial, new String[]{"Нет данных", "Дерево", "Металл", "Бетон", "Пластик"});
        setupSpinner(spSeating, new String[]{"Нет данных", "1 место", "2 места", "3 места", "4+ мест"});

        btnSend.setOnClickListener(v -> {
            int rate = (int) ratingBar.getRating();
            if (rate == 0) {
                Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show();
                return;
            }

            // Собираем объект отзыва (как в AddMarkerActivity)
            ReviewCreate review = new ReviewCreate(
                    rate,
                    spPollution.getSelectedItemPosition() + 1,
                    spCondition.getSelectedItemPosition() + 1,
                    spMaterial.getSelectedItemPosition() + 1,
                    spSeating.getSelectedItemPosition() + 1
            );

            // Если в ReviewCreate есть поле comment, добавьте:
            // review.setComment(etComment.getText().toString());

            review.setLocationId(location.getId());
            sendReviewToServer(review, dialog); // location.getId() отсюда убираем, он уже внутри review

        });

        dialog.show();
    }
    private void sendReviewToServer(ReviewCreate review, AlertDialog dialog) {
        String token = prefsManager.getAuthToken();
        if (token == null) return;

        // Теперь передаем только токен и объект (ID внутри объекта)
        Call<com.example.myapplication.models.Review> call = apiService.addReview("Bearer " + token, review);

        call.enqueue(new Callback<com.example.myapplication.models.Review>() {
            @Override
            public void onResponse(Call<com.example.myapplication.models.Review> call, Response<com.example.myapplication.models.Review> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Отзыв добавлен!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // Обновляем карту (чтобы подтянулись новые данные метки с отзывами)
                    // В идеале можно запросить только одну метку по ID, но проще обновить область:
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

    // Вспомогательный метод для спиннеров
    private void setupSpinner(Spinner spinner, String[] items) {
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    // Вспомогательные методы для красивого текста
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

    // Заглушка для удаления
    private void deleteLocation(int locationId, android.app.AlertDialog dialog) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены?")
                .setPositiveButton("Да", (d, w) -> {
                    // ТУТ ВЫЗОВ API ДЛЯ УДАЛЕНИЯ
                    // apiService.deleteLocation(token, locationId)...

                    // После успеха:
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
                    removeAllMarkersExceptTemporary(); // Очистить карту от "чужих"
                    for (LocationSeat loc : locations) addMarkerToMap(loc);

                    if (!locations.isEmpty()) {
                        Point p = new Point(locations.get(0).getCordX(), locations.get(0).getCordY());
                        mapView.getMap().move(new CameraPosition(p, 12.0f, 0.0f, 0.0f),
                                new Animation(Animation.Type.SMOOTH, 1f), null);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<LocationSeat>> call, Throwable t) { }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }


    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

}