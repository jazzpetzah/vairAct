package com.wire.actors.v1.service.sync_engine_bridge;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

public class CachedDevice {
    private UUID uuid = UUID.randomUUID();
    private Optional<Future<IDevice>> devicePromise;
    private String name;
    private CachedProcess hostProcess;

    CachedDevice(String name) {
        this.name = name;
    }

    public String getUUID() {
        return uuid.toString();
    }

    public IDevice getDevice() throws Exception {
        return this.devicePromise.orElseThrow(
                () -> new IllegalStateException("Device promise is expected to be set")
        ).get();
    }

    void setDevicePromise(Future<IDevice> devicePromise) {
        this.devicePromise = Optional.of(devicePromise);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof CachedDevice) && this.getUUID().equals(((CachedDevice) other).getUUID());
    }

    public String getName() {
        return name;
    }

    public CachedProcess getHostProcess() {
        return hostProcess;
    }

    void setHostProcess(CachedProcess hostProcess) {
        this.hostProcess = hostProcess;
    }
}