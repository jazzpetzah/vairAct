package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfo {
    private String uniqueName;

    public UserInfo() {
    }

    @JsonProperty(required = true)
    public String getUniqueName() {
        return uniqueName;
    }

    @JsonProperty(required = true)
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }
}
