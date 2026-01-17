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
import com.example.myapplication.models.LocationSeat;
import com.example.myapplication.utils.SharedPreferencesManager;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyLocationsListDialog extends Dialog {

    private Context context;
    private List<LocationSeat> locations;
    private ApiService apiService;
    private SharedPreferencesManager prefsManager;
    private OnLocationClickListener listener;


    public interface OnLocationClickListener {
        void onLocationClick(LocationSeat location);
        void onListUpdated();
    }

    public MyLocationsListDialog(Context context, List<LocationSeat> locations,
                                 ApiService apiService, SharedPreferencesManager prefsManager,
                                 OnLocationClickListener listener) {
        super(context);
        this.context = context;
        this.locations = locations;
        this.apiService = apiService;
        this.prefsManager = prefsManager;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Основной контейнер диалога
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(context.getResources().getColor(android.R.color.white));
        mainLayout.setPadding(32, 32, 32, 32);

        // Заголовок
        TextView title = new TextView(context);
        title.setText("Мои места");
        title.setTextSize(22);
        title.setPadding(0, 0, 0, 24);
        title.setTextColor(context.getResources().getColor(android.R.color.black));
        mainLayout.addView(title);

        // Скролл для списка
        ScrollView scrollView = new ScrollView(context);
        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);

        // Заполняем список
        if (locations == null || locations.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Вы еще не добавили ни одного места.");
            listContainer.addView(empty);
        } else {
            for (LocationSeat loc : locations) {
                // Инфлейтим (создаем) вьюшку из XML шага 1
                View itemView = View.inflate(context, R.layout.item_my_location, null);

                TextView tvName = itemView.findViewById(R.id.tvLocationName);
                TextView tvAddress = itemView.findViewById(R.id.tvLocationAddress);
                TextView tvStatus = itemView.findViewById(R.id.tvStatus);
                View btnDelete = itemView.findViewById(R.id.btnDeleteLocation);
                View containerInfo = itemView.findViewById(R.id.containerInfo);

                tvName.setText(loc.getName());
                tvAddress.setText(loc.getAddress() != null ? loc.getAddress() : "Без адреса");
                tvStatus.setText("Статус ID: " + loc.getStatus());


                containerInfo.setOnClickListener(v -> {
                    dismiss();
                    if (listener != null) listener.onLocationClick(loc);
                });


                btnDelete.setOnClickListener(v -> confirmDelete(loc, itemView, listContainer));

                listContainer.addView(itemView);

                View divider = new View(context);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(0xFFEEEEEE); // Светло-серый
                listContainer.addView(divider);
            }
        }


        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mainLayout.addView(scrollView, scrollParams);


        Button btnClose = new Button(context);
        btnClose.setText("Закрыть");
        btnClose.setOnClickListener(v -> dismiss());
        mainLayout.addView(btnClose);

        setContentView(mainLayout);


        if (getWindow() != null) {
            getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void confirmDelete(LocationSeat loc, View view, LinearLayout container) {
        new AlertDialog.Builder(context)
                .setTitle("Удалить место?")
                .setMessage("Вы уверены, что хотите удалить \"" + loc.getName() + "\"?")
                .setPositiveButton("Удалить", (d, w) -> {
                    deleteLocation(loc.getId(), view, container);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteLocation(int id, View view, LinearLayout container) {
        String token = prefsManager.getAuthToken();
        apiService.deleteLocation("Bearer " + token, id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Место удалено", Toast.LENGTH_SHORT).show();
                    container.removeView(view); // Удаляем строчку из списка визуально

                    if (listener != null) listener.onListUpdated(); // Обновляем карту
                } else {
                    Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}