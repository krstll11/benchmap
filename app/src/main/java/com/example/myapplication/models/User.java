package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private int id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("role_id")
    private int roleId;

    // Если API возвращает Username с большой буквы, адаптируем
    @SerializedName("Username")
    private String Username;

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() {
        // Возвращаем то поле, которое заполнено
        if (username != null && !username.isEmpty()) {
            return username;
        }
        return Username != null ? Username : "";
    }

    public void setUsername(String username) {
        this.username = username;
        this.Username = username;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }
}