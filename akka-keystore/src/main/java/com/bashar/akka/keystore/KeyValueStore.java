package com.bashar.akka.keystore;

import java.util.Optional;

import akka.cluster.ddata.*;
import com.bashar.akka.keystore.messages.Cached;
import com.bashar.akka.keystore.messages.GetRequest;
import com.bashar.akka.keystore.messages.SetRequest;
import scala.Option;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.actor.Status;
import akka.cluster.Cluster;

import akka.cluster.ddata.Replicator.Get;
import akka.cluster.ddata.Replicator.GetSuccess;
import akka.cluster.ddata.Replicator.NotFound;
import akka.cluster.ddata.Replicator.Update;

import static akka.cluster.ddata.Replicator.readLocal;
import static akka.cluster.ddata.Replicator.writeLocal;

public class KeyValueStore extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(context().system(), this);
  private final ActorRef replicator = DistributedData.get(context().system()).replicator();
  private final Cluster node = Cluster.get(context().system());

  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(SetRequest.class, cmd -> receivePutInCache(cmd.key, cmd.value))
      .match(GetRequest.class, cmd -> receiveGetFromCache(cmd.key))
      // messages from replicator
      .match(GetSuccess.class, this::receiveGetSuccess)
      .match(NotFound.class, this::receiveNotFound)
      .match(Replicator.UpdateResponse.class, this::receiveUpdateSuccess)
      // catch all
      .matchAny(o -> sender().tell(new Status.Failure(new ClassNotFoundException()), self()))
      .build();
  }

  private void receivePutInCache(String key, Object value) {
    log.info("Set received: " + key + ", val: " + value);
    Optional<Object> ctx = Optional.of(new Request(key, sender()));
    Update<LWWMap<String, Object>> update = new Update<>(dataKey(key), LWWMap.create(), writeLocal(), ctx,
            curr -> curr.put(node, key, value));
    replicator.tell(update, self());
  }

  private void receiveGetFromCache(String key) {
    log.info("get received: " + key);
    Optional<Object> ctx = Optional.of(new Request(key, sender()));
    Get<LWWMap<String, Object>> get = new Get<>(dataKey(key), readLocal(), ctx);
    replicator.tell(get, self());
  }

  private void receiveGetSuccess(GetSuccess<LWWMap<String, Object>> g) {
    log.debug("get successful from replicator");
    Request req = (Request) g.getRequest().get();
    Option<Object> valueOption = g.dataValue().get(req.key);
    Object value = valueOption.isDefined() ? valueOption.get() : null;
    req.replyTo.tell(new Cached(req.key, value), self());
  }

  private void receiveUpdateSuccess(Replicator.UpdateResponse<LWWMap<String, Object>> u) {
    log.debug("update successful from replicator");
    Optional<Request> req = Optional.class.cast(u.getRequest());
    req.ifPresent((r) -> r.replyTo.tell(true, self()));
  }

  private void receiveNotFound(NotFound<LWWMap<String, Object>> n) {
    log.debug("key not found from replicator");
    Request req = (Request) n.getRequest().get();
    req.replyTo.tell(new Cached(req.key, null), self());
  }

  private Key<LWWMap<String, Object>> dataKey(String entryKey) {
    return LWWMapKey.create("cache-" + Math.abs(entryKey.hashCode()) % 100);
  }
  
}
