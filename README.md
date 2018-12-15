# Building
`mvn install`

# running as jar:

## backend:
1- start seed node (port 2551)
`java -jar .\target\akka-keystore-1.0-SNAPSHOT-allinone.jar`

2- start a new node to join the cluster port=0 (random port will be assigned or specify yours)
`java "-Dakka.remote.netty.tcp.port=<port>" -jar .\target\akka-keystore-1.0-SNAPSHOT-allinone.jar`

## cli client:
 - run CLIApp.java


# Notes:
- gradle shadowJar didn't work
- maven shades jar worked, as documented in the akka docs here: https://doc.akka.io/docs/akka/current/general/configuration.html
- had to add quotes to -Dakka.remote..etc to work

## todo:
- maven multi module
- cluster client
- test key value store get/set

https://doc.akka.io/docs/akka/2.5/distributed-data.html