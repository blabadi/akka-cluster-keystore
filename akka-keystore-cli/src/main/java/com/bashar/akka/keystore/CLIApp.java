package com.bashar.akka.keystore;

import java.util.Arrays;
import java.util.*;

import akka.actor.ActorPath;
import akka.actor.ActorPaths;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientSettings;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.bashar.akka.keystore.messages.Cached;
import com.bashar.akka.keystore.messages.GetRequest;
import com.bashar.akka.keystore.messages.SetRequest;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class CLIApp 
{
    private static ActorRef clientActor;
    private static Timeout timeout = new Timeout(Duration.create(5, "seconds"));

    public static void main( String[] args ) throws Exception {
        initActors();
        printOutMsg("initialization done");
        printOutMsg("usage: SET,key,val or GET,key");
        Scanner scanner = new Scanner(System.in);

        System.out.print("> ");
        while(scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                continue;
            }
            String[] tokens = line.split(",");
            boolean isValid = true;
            for(String t: tokens) {
                if (t.trim().isEmpty()) {
                    isValid = false;
                }
            }

            if (tokens.length < 2) {
                isValid = false;
            }

            if (isValid) {
                if (tokens[0].trim().equalsIgnoreCase("SET")) {
                    if (tokens.length != 3) {
                       isValid = false;
                    } else {
                        printOutMsg(CLIApp.set(tokens[1].trim(), tokens[2].trim()));
                    }
                } else if (tokens[0].trim().equalsIgnoreCase("GET")) {
                    printOutMsg(CLIApp.get(tokens[1].trim()));
                } else {
                    isValid = false;
                }
            }
            if (!isValid) {
                printOutMsg("invalid");
            }
            System.out.print("> ");
        }

    }

    private static void initActors() {
        ActorSystem system = ActorSystem.create("KeyValueStoreClient");
        clientActor = system.actorOf(ClusterClient.props(
            ClusterClientSettings.create(system).withInitialContacts(initialContacts())), "client"
        );
    }

    private static boolean set(String key, String val) throws Exception {
        System.out.println("sending..");
        SetRequest setRequest = new SetRequest(key, val);
        ClusterClient.Send setMsg = new ClusterClient.Send("/user/workers", setRequest, false);
        Future setFuture = Patterns.ask(clientActor, setMsg, timeout);
        Object setResult =  Await.result(setFuture, timeout.duration());
        return  (boolean) setResult;
    }

    private static String get(String key) throws Exception {
        System.out.println("fetching..");
        GetRequest getRequest = new GetRequest(key);
        ClusterClient.Send getMsg = new ClusterClient.Send("/user/workers", getRequest, false);
        Future getFuture = Patterns.ask(clientActor, getMsg, timeout);
        Object getResult = Await.result(getFuture, timeout.duration());
        return (String) ((Cached) getResult).value;
    }

    private static Set<ActorPath> initialContacts() {
        return new HashSet<>(
            Arrays.asList(
                ActorPaths.fromString("akka.tcp://KeyValueStore@127.0.0.1:2551/system/receptionist")
            )
        );
    }

    private static void printOutMsg(Object msg) {
        System.out.println("> " + msg);
    }

}
