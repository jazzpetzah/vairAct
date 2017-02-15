package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssetsVersionInfo {
    private String version;

    public AssetsVersionInfo() {
    }

    public AssetsVersionInfo(String version) {
        this.version = version;
    }

    @JsonProperty(required = true)
    public String getVersion() {
        return version;
    }

    @JsonProperty(required = true)
    public void setVersion(String version) {
        this.version = version;
    }
}
