package com.wire.actors.v1.service.sync_engine_bridge;

import akka.actor.ActorRef;

interface IRemoteEntity {

    String name();

    ActorRef ref();

    boolean isConnected();
}
