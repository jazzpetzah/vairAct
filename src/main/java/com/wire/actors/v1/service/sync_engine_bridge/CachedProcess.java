package com.wire.actors.v1.service.sync_engine_bridge;

import java.util.concurrent.Future;

public class CachedProcess {
    private String name;
    private Future<IRemoteProcess> processPromise;

    CachedProcess (String name, Future<IRemoteProcess> processPromise) {
        this.name = name;
        this.processPromise = processPromise;
    }

    public String getName() {
        return name;
    }

    IRemoteProcess getProcess() throws Exception {
        return processPromise.get();
    }

    Future<IRemoteProcess> getProcessPromise() {
        return this.processPromise;
    }

    public String getLogPath() {
        return RemoteProcess.generateLogFile(getName()).getAbsolutePath();
    }
}
