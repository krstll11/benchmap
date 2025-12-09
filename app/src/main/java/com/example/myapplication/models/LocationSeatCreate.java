package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;

public class LocationSeatCreate {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("address")
    private String address;

    @SerializedName("type")
    private int type;

    @SerializedName("cord_x")
    private double cordX;

    @SerializedName("cord_y")
    private double cordY;

    @SerializedName("status")
    private int status;

    @SerializedName("first_review")
    private ReviewCreate firstReview;

    public LocationSeatCreate(String name, String description, String address,
                              int type, double cordX, double cordY, int status) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.type = type;
        this.cordX = cordX;
        this.cordY = cordY;
        this.status = status;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public double getCordX() { return cordX; }
    public void setCordX(double cordX) { this.cordX = cordX; }

    public double getCordY() { return cordY; }
    public void setCordY(double cordY) { this.cordY = cordY; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public ReviewCreate getFirstReview() { return firstReview; }
    public void setFirstReview(ReviewCreate firstReview) { this.firstReview = firstReview; }
}