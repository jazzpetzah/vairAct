package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginCredentials {
    private String email;
    private String password;

    public LoginCredentials() {
    }

    public LoginCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @JsonProperty(required = true)
    public String getEmail() {
        return email;
    }

    @JsonProperty(required = true)
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty(required = true)
    public String getPassword() {
        return password;
    }

    @JsonProperty(required = true)
    public void setPassword(String password) {
        this.password = password;
    }
}
