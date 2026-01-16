package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;

public class Picture {
    @SerializedName("id")
    private int id;

    @SerializedName("location_id")
    private int locationId;

    // ВАЖНО: Это имя "file_path" должно совпадать с тем, что присылает ваш Python-сервер
    @SerializedName("file_path")
    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    public int getId() {
        return id;
    }

    public int getLocationId() {
        return locationId;
    }
}