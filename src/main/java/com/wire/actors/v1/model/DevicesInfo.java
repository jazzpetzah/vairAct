package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DevicesInfo {
    private List<DeviceInfo> devices = new ArrayList<>();

    public DevicesInfo() {}

    @JsonProperty
    public List<DeviceInfo> getDevices() {
        return devices;
    }

    @JsonProperty
    public void setDevices(List<DeviceInfo> devices) {
        this.devices = devices;
    }
}
