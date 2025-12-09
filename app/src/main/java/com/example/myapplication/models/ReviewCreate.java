package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;

public class ReviewCreate {
    @SerializedName("rate")
    private int rate;

    @SerializedName("pollution_id")
    private int pollutionId;

    @SerializedName("condition_id")
    private int conditionId;

    @SerializedName("material_id")
    private int materialId;

    @SerializedName("seating_positions") // Важно: именно так, с подчеркиванием
    private int seatingPositions;

    @SerializedName("location_id") // Важно: передаем ID внутри тела
    private int locationId;

    // Конструктор
    public ReviewCreate(int rate, int pollutionId, int conditionId, int materialId, int seatingPositions) {
        this.rate = rate;
        this.pollutionId = pollutionId;
        this.conditionId = conditionId;
        this.materialId = materialId;
        this.seatingPositions = seatingPositions;
    }

    // Сеттер для ID (вызываем его перед отправкой)
    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }
}