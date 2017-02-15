package com.wire.actors.v1.service.sync_engine_bridge;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.waz.provision.CoordinatorActor;

class Coordinator {

    private static Coordinator instance = null;
    private final ActorRef actorRef;

    private Coordinator() {
        final Config config = ConfigFactory.load("actor_coordinator");
        final ActorSystem system = ActorSystem.create("CoordinatorSystem", config);
        this.actorRef = system.actorOf(Props.create(CoordinatorActor.class), "coordinatorActor");
    }

    static synchronized Coordinator getInstance() {
        if (instance == null) {
            try {
                instance = new Coordinator();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    ActorRef getActorRef() {
        return actorRef;
    }
}
