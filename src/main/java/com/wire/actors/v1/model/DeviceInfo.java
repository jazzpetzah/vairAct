package com.wire.actors.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.actors.v1.service.sync_engine_bridge.SEBridgeService;

import java.util.List;

public class DeviceInfo {
    private String uuid;
    private String name;
    private String label;
    private String fingerprint;
    private String deviceId;
    private Long msTTL = SEBridgeService.TTL_DEFAULT;
    private UserInfo user;
    private ProcessInfo hostProcess;
    private List<DeviceConversation> conversations;

    public DeviceInfo() {
    }

    @JsonProperty
    public String getUuid() {
        return uuid;
    }

    @JsonProperty
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    public String getLabel() {
        return label;
    }

    @JsonProperty
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty
    public Long getMsTTL() {
        return msTTL;
    }

    @JsonProperty
    public void setMsTTL(Long msTTL) {
        this.msTTL = msTTL;
    }

    @JsonProperty
    public String getFingerprint() {
        return this.fingerprint;
    }

    @JsonProperty
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    @JsonProperty
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @JsonProperty
    public String getDeviceId() {
        return this.deviceId;
    }

    @JsonProperty
    public UserInfo getUser() {
        return user;
    }

    @JsonProperty
    public void setUser(UserInfo user) {
        this.user = user;
    }

    @JsonProperty
    public List<DeviceConversation> getConversations() {
        return conversations;
    }

    @JsonProperty
    public void setConversations(List<DeviceConversation> conversations) {
        this.conversations = conversations;
    }

    @JsonProperty
    public ProcessInfo getHostProcess() {
        return hostProcess;
    }

    @JsonProperty
    public void setHostProcess(ProcessInfo hostProcess) {
        this.hostProcess = hostProcess;
    }
}
