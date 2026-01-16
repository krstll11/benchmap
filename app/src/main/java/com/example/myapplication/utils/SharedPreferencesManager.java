package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;

public class SharedPreferencesManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE_ID = "role_id";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SharedPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_ACCESS_TOKEN, token);
        editor.apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    // Добавил этот метод отдельно, чтобы LoginActivity мог сохранять только ID
    public void saveUserId(int userId) {
        editor.putInt(KEY_USER_ID, userId);
        editor.apply();
    }

    public void saveUserInfo(int userId, String username, String email, int roleId) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.putInt(KEY_ROLE_ID, roleId);
        editor.apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public int getRoleId() {
        return prefs.getInt(KEY_ROLE_ID, -1);
    }

    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }

    // Ваш метод декодирования токена (оставлен без изменений)
    public int getUserIdFromToken() {
        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            return -1;
        }

        try {
            String[] split = token.split("\\.");
            if (split.length < 2) return -1;

            String body = new String(Base64.decode(split[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(body);

            if (json.has("sub")) {
                try {
                    return Integer.parseInt(json.getString("sub"));
                } catch (NumberFormatException e) {
                    if (json.has("id")) return json.getInt("id");
                    if (json.has("user_id")) return json.getInt("user_id");
                }
            }
        } catch (Exception e) {
            Log.e("JWT_DECODE", "Ошибка декодирования токена", e);
        }
        return -1;
    }
}