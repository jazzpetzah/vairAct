package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorInfo {
    private String message;

    public ErrorInfo() {
    }

    public ErrorInfo(String message) {
        this.message = message;
    }

    @JsonProperty(required = true)
    public String getMessage() {
        return message;
    }

    @JsonProperty(required = true)
    public void setMessage(String message) {
        this.message = message;
    }
}
