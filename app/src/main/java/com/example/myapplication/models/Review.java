package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

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

    @SerializedName("author_id")
    private int authorId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("author")
    private User author;

    // Getters and setters
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

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
}