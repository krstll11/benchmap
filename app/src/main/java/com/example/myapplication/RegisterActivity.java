package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.api.ApiService;
import com.example.myapplication.api.RetrofitClient;
import com.example.myapplication.models.AuthResponse;
import com.example.myapplication.models.RegisterRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private EditText editTextPasswordConfirm;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPasswordConfirm = findViewById(R.id.editTextPasswordConfirm);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);

        apiService = RetrofitClient.getApiService(this);

        buttonRegister.setOnClickListener(v -> register());

        // Кнопка "Уже есть аккаунт" просто закрывает регистрацию
        textViewLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String email = editTextEmail.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordConfirm = editTextPasswordConfirm.getText().toString().trim();

        // 1. Проверка на пустоту
        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Валидация email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Проверка совпадения паролей
        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        // Создаем запрос
        RegisterRequest request = new RegisterRequest(username, email, password, passwordConfirm);

        Call<AuthResponse> call = apiService.register(request);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressBar.setVisibility(View.GONE);
                buttonRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    // УСПЕХ:
                    Toast.makeText(RegisterActivity.this, "Регистрация успешна! Теперь войдите.", Toast.LENGTH_LONG).show();

                    // Переходим на экран логина
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    // Флаги, чтобы очистить историю (нельзя вернуться назад на регистрацию)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // Закрываем экран регистрации

                } else {
                    // ОШИБКА:
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("REG_ERROR", "Code: " + response.code() + ", Body: " + errorBody);

                        if (response.code() == 422) {
                            Toast.makeText(RegisterActivity.this, "Некорректные данные (пароль или email)", Toast.LENGTH_LONG).show();
                        } else if (response.code() == 400) {
                            Toast.makeText(RegisterActivity.this, "Пользователь уже существует", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Ошибка регистрации: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(RegisterActivity.this, "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                buttonRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}