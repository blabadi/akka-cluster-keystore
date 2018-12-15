package com.bashar.akka.keystore;
import akka.actor.ActorRef;

class Request {
    public final String key;
    public final ActorRef replyTo;

    public Request(String key, ActorRef replyTo) {
      this.key = key;
      this.replyTo = replyTo;
    }
  }