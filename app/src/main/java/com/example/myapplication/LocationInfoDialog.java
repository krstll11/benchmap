package com.example.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.myapplication.api.ApiService;
import com.example.myapplication.models.LocationSeat;
import com.example.myapplication.models.Picture;
import com.example.myapplication.models.Review;
import com.example.myapplication.utils.SharedPreferencesManager;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationInfoDialog extends Dialog {

    private LocationSeat location;
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;
    private OnActionListener actionListener;

    private TextView tvName, tvDescription, tvAddress, tvType, tvStatus, tvPhotosLabel;
    private LinearLayout reviewsContainer, imagesContainer;
    private HorizontalScrollView scrollPhotos;
    private Button btnDelete, btnClose, btnAddReview;


    private static final String BASE_URL = "http://10.0.2.2:8000";

    public interface OnActionListener {
        void onLocationDeleted();
        void onAddReviewClick(LocationSeat location);
    }

    public LocationInfoDialog(Context context, LocationSeat location,
                              ApiService apiService, SharedPreferencesManager prefsManager,
                              OnActionListener listener) {
        super(context);
        this.location = location;
        this.apiService = apiService;
        this.prefsManager = prefsManager;
        this.actionListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        setContentView(R.layout.dialog_location_info);


        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvAddress = findViewById(R.id.tvAddress);
        tvType = findViewById(R.id.tvType);
        tvStatus = findViewById(R.id.tvStatus);
        reviewsContainer = findViewById(R.id.reviewsContainer);
        imagesContainer = findViewById(R.id.imagesContainer);
        tvPhotosLabel = findViewById(R.id.tvPhotosLabel);
        scrollPhotos = findViewById(R.id.scrollPhotos);
        btnDelete = findViewById(R.id.btnDelete);
        btnClose = findViewById(R.id.btnClose);
        btnAddReview = findViewById(R.id.btnAddReview);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }


        tvName.setText(location.getName());
        tvDescription.setText(location.getDescription() != null && !location.getDescription().isEmpty() ?
                location.getDescription() : "Нет описания");
        tvAddress.setText(location.getAddress() != null ? location.getAddress() : "Адрес не указан");
        tvType.setText("Тип: " + safeGetArrayItem(R.array.location_types, location.getType()));
        tvStatus.setText("Статус: " + safeGetArrayItem(R.array.location_statuses, location.getStatus()));

        // --- Загрузка картинок (Отдельный запрос) ---
        loadLocationPictures();

        // --- Заполнение отзывов ---
        displayReviews(location.getReviews());

        // --- Проверка прав на удаление места ---
        checkDeletePermission();

        btnDelete.setOnClickListener(v -> confirmDeleteLocation());
        btnClose.setOnClickListener(v -> dismiss());

        if (prefsManager.isLoggedIn()) {
            btnAddReview.setVisibility(View.VISIBLE);
            btnAddReview.setOnClickListener(v -> {
                dismiss();
                if (actionListener != null) {
                    actionListener.onAddReviewClick(location);
                }
            });
        } else {
            btnAddReview.setVisibility(View.GONE);
        }
    }

    private void loadLocationPictures() {
        String token = prefsManager.getAuthToken();
        if (token == null) return;

        apiService.getLocationPictures("Bearer " + token, location.getId()).enqueue(new Callback<List<Picture>>() {
            @Override
            public void onResponse(Call<List<Picture>> call, Response<List<Picture>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayImages(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Picture>> call, Throwable t) {
                Log.e("DEBUG_PICS", "Ошибка загрузки фото", t);
            }
        });
    }

    private void displayImages(List<Picture> pictures) {
        imagesContainer.removeAllViews(); // Очищаем старые

        // 1. УПРАВЛЕНИЕ ВИДИМОСТЬЮ
        if (pictures == null || pictures.isEmpty()) {
            tvPhotosLabel.setVisibility(View.GONE);
            scrollPhotos.setVisibility(View.GONE);
            return;
        } else {

            tvPhotosLabel.setVisibility(View.VISIBLE);
            scrollPhotos.setVisibility(View.VISIBLE);
        }

        for (Picture pic : pictures) {
            ImageView imageView = new ImageView(getContext());

            // 2. РАЗМЕРЫ

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(400, 400);
            params.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // 3. ССЫЛКА
            String fullUrl = "http://10.0.2.2:8000" + pic.getUrl();
            Log.d("IMAGE_LOAD", "Загружаю: " + fullUrl);

            // 4. GLIDE
            com.bumptech.glide.Glide.with(getContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(android.R.drawable.stat_notify_error) // Полезно видеть, если ошибка
                    .into(imageView);


            imagesContainer.addView(imageView);

            // Клик для открытия
            imageView.setOnClickListener(v -> {
                showFullScreenImage(fullUrl);
            });
        }
    }

    private void displayReviews(List<Review> reviews) {
        reviewsContainer.removeAllViews();
        if (reviews == null || reviews.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("Отзывов пока нет");
            tv.setPadding(20, 20, 20, 20);
            reviewsContainer.addView(tv);
            return;
        }

        int currentUserId = prefsManager.getUserIdFromToken();
        if (currentUserId == -1) currentUserId = prefsManager.getUserId();

        for (Review review : reviews) {
            View reviewView = View.inflate(getContext(), R.layout.item_review, null);

            // 1. Находим ВСЕ TextView из макета item_review.xml
            TextView tvAuthor = reviewView.findViewById(R.id.tvAuthor);
            TextView tvRate = reviewView.findViewById(R.id.tvRate);
            TextView tvCreatedAt = reviewView.findViewById(R.id.tvCreatedAt); // Если есть поле даты
            TextView tvPollution = reviewView.findViewById(R.id.tvPollution);
            TextView tvCondition = reviewView.findViewById(R.id.tvCondition);
            TextView tvMaterial = reviewView.findViewById(R.id.tvMaterial);
            TextView tvSeating = reviewView.findViewById(R.id.tvSeating);
            View btnDeleteReview = reviewView.findViewById(R.id.btnDeleteReview);

            // 2. Заполняем основные данные
            tvRate.setText(getStarsString(review.getRate()));

            if (review.getAuthor() != null && review.getAuthor().getUsername() != null) {
                tvAuthor.setText(review.getAuthor().getUsername());
            } else {
                tvAuthor.setText("ID: " + review.getAuthorId());
            }

            // Заполняем дату (если есть TextView для нее)
            if (tvCreatedAt != null && review.getCreatedAt() != null) {
                tvCreatedAt.setText(review.getCreatedAt());
            }

            // 3. ЗАПОЛНЯЕМ ХАРАКТЕРИСТИКИ (Чистота, Состояние и т.д.)
            // Это те строки, которые у вас пропали:
            if (tvPollution != null) tvPollution.setText(safeGetArrayItem(R.array.pollution_levels, review.getPollutionId()));
            if (tvCondition != null) tvCondition.setText(safeGetArrayItem(R.array.condition_levels, review.getConditionId()));
            if (tvMaterial != null) tvMaterial.setText(safeGetArrayItem(R.array.material_types, review.getMaterialId()));
            if (tvSeating != null) tvSeating.setText(safeGetArrayItem(R.array.seating_positions, review.getSeatingPositions()));

            // 4. Логика кнопки удаления
            if (btnDeleteReview != null) {
                if (review.getAuthorId() == currentUserId || currentUserId == 1) {
                    btnDeleteReview.setVisibility(View.VISIBLE);
                    btnDeleteReview.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                            .setTitle("Удалить отзыв?")
                            .setMessage("Вы действительно хотите удалить этот отзыв?")
                            .setPositiveButton("Да", (d, w) -> deleteReviewById(review.getId(), reviewView))
                            .setNegativeButton("Нет", null).show());
                } else {
                    btnDeleteReview.setVisibility(View.GONE);
                }
            }

            reviewsContainer.addView(reviewView);
        }
    }

    private void deleteReviewById(int reviewId, View view) {
        apiService.deleteReview("Bearer " + prefsManager.getAuthToken(), reviewId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    reviewsContainer.removeView(view);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void checkDeletePermission() {
        int currentUserId = prefsManager.getUserIdFromToken();
        if (currentUserId == -1) currentUserId = prefsManager.getUserId();

        if (currentUserId == 1 || location.getAuthorId() == currentUserId) {
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteLocation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Удаление места")
                .setMessage("Вы уверены?")
                .setPositiveButton("Удалить", (dialog, which) -> performDelete())
                .setNegativeButton("Отмена", null).show();
    }

    private void performDelete() {
        apiService.deleteLocation("Bearer " + prefsManager.getAuthToken(), location.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    dismiss();
                    if (actionListener != null) actionListener.onLocationDeleted();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private String safeGetArrayItem(int resId, int index) {
        try {
            String[] items = getContext().getResources().getStringArray(resId);
            if (index > 0 && index <= items.length) return items[index - 1];
        } catch (Exception e) {}
        return "Неизвестно";
    }

    private String getStarsString(int rate) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rate; i++) stars.append("★");
        return stars.toString();
    }
    private void showFullScreenImage(String imageUrl) {

        Dialog fullScreenDialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        fullScreenDialog.setContentView(R.layout.dialog_full_image);


        ImageView ivFull = fullScreenDialog.findViewById(R.id.ivFullImage);
        View btnClose = fullScreenDialog.findViewById(R.id.btnCloseFull);


        com.bumptech.glide.Glide.with(getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(ivFull);


        btnClose.setOnClickListener(v -> fullScreenDialog.dismiss());


        ivFull.setOnClickListener(v -> fullScreenDialog.dismiss());


        fullScreenDialog.show();
    }
}