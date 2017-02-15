package com.wire.actors.v1.service.sync_engine_bridge;

import java.io.File;

/**
 *
 */
interface IRemoteProcess extends IRemoteEntity {
    void restart() throws Exception;

    boolean isOtrOnly();

    File getLog();

    void shutdown();
}
