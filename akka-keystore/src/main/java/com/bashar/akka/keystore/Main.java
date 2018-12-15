package com.bashar.akka.keystore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.client.ClusterClientReceptionist;
import akka.routing.BalancingPool;

public class Main {
  public static void main(String... args)  {
    ActorSystem system = ActorSystem.create("KeyValueStore");
    system.actorOf(Props.create(ClusterController.class), "clusterController");
    ActorRef workers = system.actorOf(new BalancingPool(5).props(Props.create(KeyValueStore.class)), "workers");
    ((ClusterClientReceptionist) ClusterClientReceptionist.apply(system)).registerService(workers);

  }
}
