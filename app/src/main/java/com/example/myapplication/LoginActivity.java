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
import com.example.myapplication.utils.JwtUtils; // Импортируем наш новый класс
import com.example.myapplication.utils.SharedPreferencesManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;
    private ProgressBar progressBar;
    private TextView textViewRegister;

    private ApiService apiService;
    private SharedPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);
        textViewRegister = findViewById(R.id.textViewRegister);

        apiService = RetrofitClient.getApiService(this);
        prefsManager = new SharedPreferencesManager(this);

        buttonLogin.setOnClickListener(v -> login());

        textViewRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void login() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        Call<AuthResponse> call = apiService.login(username, password);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressBar.setVisibility(View.GONE);
                buttonLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    String token = authResponse.getAccessToken();

                    if (token != null && !token.isEmpty()) {
                        // 1. Сохраняем токен
                        prefsManager.saveAuthToken(token);

                        // 2. Достаем ID прямо из токена! (без запроса к серверу)
                        int userId = JwtUtils.getUserIdFromToken(token);

                        Log.d("LOGIN_DEBUG", "ID из токена: " + userId);

                        if (userId != 0) {
                            prefsManager.saveUserId(userId);

                            // 3. Переходим на главный экран
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Ошибка: некорректный ID в токене", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(LoginActivity.this, "Ошибка: токен пуст", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Ошибка авторизации: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                buttonLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}