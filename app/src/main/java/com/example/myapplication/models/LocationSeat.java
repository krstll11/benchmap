package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LocationSeat {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("address")
    private String address;

    @SerializedName("type")
    private int type;

    @SerializedName("cord_x")
    private double cordX; // latitude

    @SerializedName("cord_y")
    private double cordY; // longitude

    @SerializedName("status")
    private int status;

    @SerializedName("author_id")
    private int authorId;

    @SerializedName("reviews")
    private List<Review> reviews;

    @SerializedName("pictures")
    private List<Picture> pictures;

    @SerializedName("author")
    private User author; // Если API возвращает автора

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }

    public List<Picture> getPictures() { return pictures; }
    public void setPictures(List<Picture> pictures) { this.pictures = pictures; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
}