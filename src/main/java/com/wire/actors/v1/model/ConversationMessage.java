package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.core.MediaType;

public class ConversationMessage {
    private String payload;
    private String mimeType = MediaType.TEXT_PLAIN;
    private String fileName;

    public ConversationMessage() {
    }

    @JsonProperty(required = true)
    public String getPayload() {
        return payload;
    }

    @JsonProperty(required = true)
    public void setPayload(String payload) {
        this.payload = payload;
    }

    @JsonProperty(required = true)
    public String getMimeType() {
        return mimeType;
    }

    @JsonProperty(required = true)
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @JsonProperty
    public String getFileName() {
        return fileName;
    }

    @JsonProperty
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
