package com.bashar.akka.keystore;

import static akka.cluster.ClusterEvent.initialStateAsEvents;

import java.util.ArrayList;
import java.util.List;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ClusterController extends AbstractActor {
    protected final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private final Cluster cluster = Cluster.get(context().system());

    @Override
    public void preStart() {
        cluster.subscribe(self(), initialStateAsEvents(), ClusterEvent.ClusterDomainEvent.class);
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }
    
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ClusterEvent.CurrentClusterState.class, state -> {
                log.debug("Current members: {}", state.members());
            })
            .match(ClusterEvent.MemberUp.class, event -> {
                log.debug("Member is Up: {}", event.member().address());
            })
            .match(ClusterEvent.UnreachableMember.class, event -> {
                log.debug("Member detected as unreachable: {}", event.member());
            })
            .match(ClusterEvent.MemberRemoved.class, event -> {
                log.debug("Member is Removed: {} after {}", event.member().address(), event.previousStatus());
            })
            .match(ClusterEvent.MemberEvent.class, event -> {
                log.info("Member Event: " + event.toString());
            })
            .build();
  }
}