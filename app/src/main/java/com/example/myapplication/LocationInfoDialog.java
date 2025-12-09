package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.models.LocationSeat;
import com.example.myapplication.models.Review;
import com.example.myapplication.api.ApiService;
import com.example.myapplication.utils.SharedPreferencesManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class LocationInfoDialog extends Dialog {

    private LocationSeat location;
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    // Интерфейс для общения с MainActivity
    private OnActionListener actionListener;

    public interface OnActionListener {
        void onLocationDeleted(); // Вызывается после успешного удаления
        void onAddReviewClick(LocationSeat location); // Вызывается при клике "Отзыв"
    }

    private TextView tvName, tvDescription, tvAddress, tvType, tvStatus;
    private LinearLayout reviewsContainer;
    private Button btnDelete, btnClose, btnAddReview;

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

        // Устанавливаем прозрачный фон для окна, чтобы работали закругления в XML
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        setContentView(R.layout.dialog_location_info);

        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvAddress = findViewById(R.id.tvAddress);
        tvType = findViewById(R.id.tvType);
        tvStatus = findViewById(R.id.tvStatus);
        reviewsContainer = findViewById(R.id.reviewsContainer);
        btnDelete = findViewById(R.id.btnDelete);
        btnClose = findViewById(R.id.btnClose);
        btnAddReview = findViewById(R.id.btnAddReview);

        // --- Заполнение данных ---
        tvName.setText(location.getName());
        tvDescription.setText(location.getDescription() != null && !location.getDescription().isEmpty() ?
                location.getDescription() : "Нет описания");
        tvAddress.setText(location.getAddress() != null ?
                "Адрес: " + location.getAddress() : "Адрес не указан");

        // Тип и Статус
        tvType.setText("Тип: " + safeGetArrayItem(R.array.location_types, location.getType()));
        tvStatus.setText("Статус: " + safeGetArrayItem(R.array.location_statuses, location.getStatus()));

        // Отзывы
        displayReviews(location.getReviews());

        // Проверка прав на удаление
        checkDeletePermission();

        // Кнопка отзыва (видна только авторизованным)
        if (prefsManager.isLoggedIn()) {
            btnAddReview.setVisibility(View.VISIBLE);
            btnAddReview.setOnClickListener(v -> {
                dismiss();
                if (actionListener != null) actionListener.onAddReviewClick(location);
            });
        } else {
            btnAddReview.setVisibility(View.GONE);
        }

        btnDelete.setOnClickListener(v -> confirmDeleteLocation());
        btnClose.setOnClickListener(v -> dismiss());
    }

    // Безопасное получение строки из массива ресурсов
    private String safeGetArrayItem(int arrayResId, int index) {
        try {
            String[] items = getContext().getResources().getStringArray(arrayResId);
            // Индексы обычно приходят с 1, а массив с 0
            int arrayIndex = index - 1;
            if (arrayIndex >= 0 && arrayIndex < items.length) {
                return items[arrayIndex];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Неизвестно (" + index + ")";
    }

    private void displayReviews(List<Review> reviews) {
        reviewsContainer.removeAllViews(); // Очистить перед добавлением

        if (reviews == null || reviews.isEmpty()) {
            TextView noReviews = new TextView(getContext());
            noReviews.setText("Отзывов пока нет");
            noReviews.setPadding(0, 10, 0, 10);
            reviewsContainer.addView(noReviews);
            return;
        }

        for (Review review : reviews) {
            // Инфлейтим наш новый макет item_review.xml
            View reviewView = getLayoutInflater().inflate(R.layout.item_review, null);

            TextView tvAuthor = reviewView.findViewById(R.id.tvAuthor);
            TextView tvCreatedAt = reviewView.findViewById(R.id.tvCreatedAt);
            TextView tvRate = reviewView.findViewById(R.id.tvRate);
            TextView tvPollution = reviewView.findViewById(R.id.tvPollution);
            TextView tvCondition = reviewView.findViewById(R.id.tvCondition);
            TextView tvMaterial = reviewView.findViewById(R.id.tvMaterial);
            TextView tvSeating = reviewView.findViewById(R.id.tvSeating);
            // TextView tvComment = reviewView.findViewById(R.id.tvComment); // Если есть

            // Заполняем данные
            tvRate.setText("⭐ " + review.getRate());

            // Автор (проверяем на null)
            String authorName = (review.getAuthor() != null) ? review.getAuthor().getUsername() : "Аноним";
            tvAuthor.setText(authorName);

            // Дата (можно отформатировать, если приходит строка)
            tvCreatedAt.setText(review.getCreatedAt() != null ? review.getCreatedAt() : "");

            // Детали - используем массивы из ресурсов
            tvPollution.setText(safeGetArrayItem(R.array.pollution_levels, review.getPollutionId()));
            tvCondition.setText(safeGetArrayItem(R.array.condition_levels, review.getConditionId()));
            tvMaterial.setText(safeGetArrayItem(R.array.material_types, review.getMaterialId()));
            tvSeating.setText(safeGetArrayItem(R.array.seating_positions, review.getSeatingPositions()));

            reviewsContainer.addView(reviewView);
        }
    }

    private void checkDeletePermission() {
        int currentUserId = prefsManager.getUserId();
        // ВАЖНО: Убедитесь, что prefsManager.getUserId() возвращает правильный ID текущего юзера

        // Логика: Показывать кнопку только автору
        boolean isAuthor = (location.getAuthorId() == currentUserId);

        // Если вы хотите добавить админа, раскомментируйте:
        // boolean isAdmin = prefsManager.getRoleId() == 1;

        if (isAuthor /* || isAdmin */) {
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteLocation() {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить это место?")
                .setPositiveButton("Да", (dialog, which) -> performDelete())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performDelete() {
        String token = prefsManager.getAuthToken();
        if (token == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ResponseBody> call = apiService.deleteLocation("Bearer " + token, location.getId());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Место успешно удалено", Toast.LENGTH_SHORT).show();
                    dismiss(); // Закрываем диалог

                    // Сообщаем MainActivity, что нужно обновить карту
                    if (actionListener != null) {
                        actionListener.onLocationDeleted();
                    }
                } else {
                    String error = "Ошибка: " + response.code();
                    try {
                        if(response.errorBody() != null) error += " " + response.errorBody().string();
                    } catch (Exception e) {}
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    Log.e("DELETE", error);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                Log.e("DELETE", "Failure", t);
            }
        });
    }
}