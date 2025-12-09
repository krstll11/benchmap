package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class AddMarkerActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextDescription;
    private EditText editTextAddress;
    private Spinner spinnerType;
    private Spinner spinnerStatus;
    private Button buttonSave;

    // Новые поля для фото
    private Button buttonSelectImage;
    private ImageView imageViewPreview;
    private Uri selectedImageUri = null; // Тут будем хранить ссылку на фото

    private double latitude;
    private double longitude;

    // Лаунчер для выбора картинки из галереи
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imageViewPreview.setVisibility(View.VISIBLE);
                    imageViewPreview.setImageURI(uri);
                    buttonSelectImage.setText("Изменить фото");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_marker);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
            latitude = intent.getDoubleExtra("latitude", 0);
            longitude = intent.getDoubleExtra("longitude", 0);

            TextView tvCoords = findViewById(R.id.textViewCoordinates);
            if (tvCoords != null) {
                tvCoords.setText(String.format("Координаты: %.6f, %.6f", latitude, longitude));
            }
        } else {
            Toast.makeText(this, "Ошибка: координаты не получены", Toast.LENGTH_SHORT).show();
            finish();
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextAddress = findViewById(R.id.editTextAddress);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        buttonSave = findViewById(R.id.buttonSave);

        // Инициализация фото-элементов
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        imageViewPreview = findViewById(R.id.imageViewPreview);
    }

    private void setupListeners() {
        // Нажатие на выбор фото
        buttonSelectImage.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        buttonSave.setOnClickListener(v -> saveMarker());
    }

    private void saveMarker() {
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название места", Toast.LENGTH_SHORT).show();
            return;
        }

        int type = spinnerType.getSelectedItemPosition() + 1;
        int status = spinnerStatus.getSelectedItemPosition() + 1;

        Intent resultIntent = new Intent();
        resultIntent.putExtra("name", name);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("address", address);
        resultIntent.putExtra("type", type);
        resultIntent.putExtra("status", status);
        resultIntent.putExtra("latitude", latitude);
        resultIntent.putExtra("longitude", longitude);
        resultIntent.putExtra("has_review", false);

        // Если картинка выбрана, передаем её URI строкой
        if (selectedImageUri != null) {
            resultIntent.putExtra("image_uri", selectedImageUri.toString());
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}