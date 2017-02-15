package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessInfo {
    private String name;
    private String logPath;

    public ProcessInfo() {}

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public void setName(String id) {
        this.name = id;
    }

    @JsonProperty
    public String getLogPath() {
        return logPath;
    }

    @JsonProperty
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
}
