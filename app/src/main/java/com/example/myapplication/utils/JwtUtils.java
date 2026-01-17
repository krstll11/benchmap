package com.example.myapplication.utils;

import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;

public class JwtUtils {

    public static int getUserIdFromToken(String token) {
        try {
            // Токен выглядит так: Header.Payload.Signature
            // Нам нужна 2-я часть (индекс 1)
            String[] split = token.split("\\.");
            if (split.length < 2) {
                return 0;
            }

            // Декодируем из Base64
            String body = getJson(split[1]);

            // Парсим JSON
            JSONObject jsonObject = new JSONObject(body);

            // Достаем "sub" (где лежит ID)
            // В вашем примере "sub": "1" (строка), превращаем в int
            return Integer.parseInt(jsonObject.getString("sub"));

        } catch (Exception e) {
            Log.e("JWT_DECODE", "Ошибка декодирования токена", e);
            return 0;
        }
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }
}