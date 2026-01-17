package com.example.myapplication.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    // В Java называем переменную с маленькой (username),
    // но аннотация говорит GSON'у: "В JSON запиши это поле как 'Username'"
    @SerializedName("Username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("password_confirm")
    private String passwordConfirm;

    public RegisterRequest(String username, String email, String password, String passwordConfirm) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
    }

    // Геттеры и сеттеры
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}