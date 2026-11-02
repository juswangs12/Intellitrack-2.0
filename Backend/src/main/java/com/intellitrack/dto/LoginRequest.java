package com.intellitrack.dto;

public class LoginRequest {
    private String email;
    private String password;
    private String audience;
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
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
}
