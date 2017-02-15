package com.wire.actors.v1.service.sync_engine_bridge;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.pattern.Patterns;

import com.waz.provision.ActorMessage;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

abstract class RemoteEntity implements IRemoteEntity {

    private FiniteDuration actorTimeout;

    private String name = null;

    private ActorRef ref = null;

    RemoteEntity(String name, FiniteDuration actorTimeout) {
        this.actorTimeout = actorTimeout;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public ActorRef ref() {
        return ref;
    }

    void setRef(ActorRef ref) {
        this.ref = ref;
    }

    @Override
    public boolean isConnected() {
        if (this.ref == null) {
            return false;
        }
        Object resp;
        try {
            resp = askActor(ref, new ActorMessage.Echo("test", "test"), 5000);
        } catch (Exception e) {
            return false;
        }
        return (resp instanceof ActorMessage.Echo && ((ActorMessage.Echo) resp).msg().equals("test"));
    }

    /**
     * The method is synchronous
     */
    Object askActor(ActorRef actorRef, ActorMessage message) throws Exception {
        Future<Object> future = Patterns.ask(actorRef, message, actorTimeout.toMillis());
        return Await.result(future, actorTimeout);
    }

    Object askActor(ActorRef actorRef, ActorMessage message, long timeoutMilliseconds) throws Exception {
        final FiniteDuration timeotObj = new FiniteDuration(timeoutMilliseconds, TimeUnit.MILLISECONDS);
        Future<Object> future = Patterns.ask(actorRef, message, timeotObj.toMillis());
        return Await.result(future, timeotObj);
    }
}
