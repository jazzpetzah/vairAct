package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EphemeralProperties {
    private Long msTimeout;

    public EphemeralProperties() {}

    @JsonProperty(required = true)
    public long getMsTimeout() {
        return msTimeout;
    }

    @JsonProperty(required = true)
    public void setMsTimeout(Long msTimeout) {
        this.msTimeout = msTimeout;
    }
}
