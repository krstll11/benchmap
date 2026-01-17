package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;

public class Review {
    @SerializedName("id")
    private int id;

    @SerializedName("rate")
    private int rate;

    @SerializedName("pollution_id")
    private int pollutionId;

    @SerializedName("condition_id")
    private int conditionId;

    @SerializedName("material_id")
    private int materialId;

    @SerializedName("seating_positions")
    private int seatingPositions;
    @SerializedName("location_name")
    private String locationName;


    @SerializedName("author_id")
    private int authorId;

    @SerializedName("created_at")
    private String createdAt;

    // --- ИЗМЕНЕНИЯ ЗДЕСЬ ---

    // 1. Принимаем строку из JSON (так как сервер шлет "author_username")
    @SerializedName("author_username")
    private String authorUsername;

    // 2. Поле author убираем из JSON-маппинга (убираем @SerializedName),
    // так как в JSON нет объекта "author".
    private User author;

    // --- ГЕТТЕРЫ И СЕТТЕРЫ ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }

    public int getPollutionId() { return pollutionId; }
    public void setPollutionId(int pollutionId) { this.pollutionId = pollutionId; }

    public int getConditionId() { return conditionId; }
    public void setConditionId(int conditionId) { this.conditionId = conditionId; }

    public int getMaterialId() { return materialId; }
    public void setMaterialId(int materialId) { this.materialId = materialId; }

    public int getSeatingPositions() { return seatingPositions; }
    public void setSeatingPositions(int seatingPositions) { this.seatingPositions = seatingPositions; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }


    // --- МАГИЯ ЗДЕСЬ ---
    // Метод создает "фейкового" пользователя на лету,
    // чтобы ваш старый код review.getAuthor().getUsername() работал
    public User getAuthor() {
        if (author == null && authorUsername != null) {
            author = new User();
            author.setUsername(authorUsername);
            author.setId(authorId);
        }
        return author;
    }

    public void setAuthor(User author) { this.author = author; }
}