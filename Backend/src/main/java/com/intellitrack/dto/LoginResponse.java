package com.intellitrack.dto;

public class LoginResponse {
    private String token;
    private String refreshToken;
    private UserDTO user;
    private String role;

    public LoginResponse() {}

    public LoginResponse(String token, UserDTO user, String role) {
        this.token = token;
        this.user = user;
        this.role = role;
    }

    public LoginResponse(String token, String refreshToken, UserDTO user, String role) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
        this.role = role;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}