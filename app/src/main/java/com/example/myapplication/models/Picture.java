package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;

public class Picture {
    @SerializedName("id")
    private int id;

    @SerializedName("url")
    private String url; // Поле есть, но к нему нужен доступ

    @SerializedName("user_id")
    private int userId;

    // --- ВАМ НЕ ХВАТАЛО ВОТ ЭТОГО МЕТОДА ---
    public String getUrl() {
        return url;
    }
    // ---------------------------------------

    public void setUrl(String url) {
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}