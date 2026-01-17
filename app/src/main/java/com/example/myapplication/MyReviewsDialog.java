package com.example.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.api.ApiService;
import com.example.myapplication.models.Review;
import com.example.myapplication.utils.SharedPreferencesManager;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReviewsDialog extends Dialog {

    private List<Review> reviews;
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;
    private Context context;

    public MyReviewsDialog(Context context, List<Review> reviews, ApiService apiService, SharedPreferencesManager prefsManager) {
        super(context);
        this.context = context;
        this.reviews = reviews;
        this.apiService = apiService;
        this.prefsManager = prefsManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        mainLayout.setBackgroundColor(context.getResources().getColor(android.R.color.white));

        // Заголовок
        TextView title = new TextView(context);
        title.setText("Мои отзывы");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 20);
        mainLayout.addView(title);

        // Контейнер с прокруткой
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(container);

        // Заполняем отзывы
        if (reviews == null || reviews.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("У вас пока нет отзывов");
            container.addView(empty);
        } else {
            for (Review review : reviews) {
                View itemView = View.inflate(context, R.layout.item_review, null);

                // Настраиваем поля (используем логику из вашего LocationInfoDialog)
                TextView tvRate = itemView.findViewById(R.id.tvRate);
                TextView tvAuthor = itemView.findViewById(R.id.tvAuthor);
                TextView tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
                View btnDelete = itemView.findViewById(R.id.btnDeleteReview);

                tvRate.setText(getStarsString(review.getRate()));


                tvAuthor.setText("Мой отзыв (ID места: " + review.getId() + ")"); // Или review.getLocationId() если есть

                if (tvCreatedAt != null && review.getCreatedAt() != null) {
                    tvCreatedAt.setText(review.getCreatedAt());
                }

                // Кнопка удаления
                if (btnDelete != null) {
                    btnDelete.setVisibility(View.VISIBLE);
                    btnDelete.setOnClickListener(v -> {
                        confirmDelete(review.getId(), itemView, container);
                    });
                }

                container.addView(itemView);

                // Разделитель
                View divider = new View(context);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(android.graphics.Color.LTGRAY);
                container.addView(divider);
            }
        }

        mainLayout.addView(scrollView);

        // Кнопка закрыть
        Button btnClose = new Button(context);
        btnClose.setText("Закрыть");
        btnClose.setOnClickListener(v -> dismiss());
        mainLayout.addView(btnClose);

        setContentView(mainLayout);
    }

    private void confirmDelete(int reviewId, View view, LinearLayout container) {
        new AlertDialog.Builder(context)
                .setTitle("Удалить?")
                .setMessage("Удалить этот отзыв?")
                .setPositiveButton("Да", (d, w) -> {
                    apiService.deleteReview("Bearer " + prefsManager.getAuthToken(), reviewId)
                            .enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    if(response.isSuccessful()) {
                                        container.removeView(view);
                                        Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {}
                            });
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private String getStarsString(int rate) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rate; i++) stars.append("★");
        return stars.toString();
    }
}