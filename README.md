# Standalone Mini Accumulo Cluster (SMAC)

## Description

This project is an experiment with the Accumulo Mini Cluster (MAC).  The intent is to make an executable jar that runs the MAC.  
Currently, running the jar will start a Zookeeper, Master, Monitor, GC and 2 TServer process.  It will then drop you into
an interactive shell.  The cluster runs until you close the shell.  More documentation is needed

## Building

To build this standalone jar, run the following

    mvn clean package
    
In the target directory, you will see a file named `standalone-mini-accumulo-cluster-0.0.1-SNAPSHOT.jar`.  That is the 
executable jar, currently at 44M

## Running

After building the jar, run the following from the project top level directory

    java -jar target/standalone-mini-accumulo-cluster-0.0.1-SNAPSHOT.jar
  
Your output should be something like the following


    Starting a Mini Accumulo Cluster: instanceName: smac with rootPassword: secret
    Temp dir is: /var/folders/cd/l8dpphgn3j1gfpr2gs6yb9vjjpd1pt/T/1473438202535-0
    Zookeeper is: localhost:2181
    Monitor running at: 0.0.0.0:56198
    Starting a shell
    
    Shell - Apache Accumulo Interactive Shell
    - 
    - version: 1.8.0
    - instance name: smac
    - instance id: 02376280-2881-4c17-9091-aa23b8ee1238
    - 
    - type 'help' for a list of available commands
    - 
    root@smac> exit

#### Couple of things to note here.  
- The temp dir is printed.  It will be deleted when you exit.  To make it persist
pass in a `-DtempMiniDir=/some/path` option before the `-jar` command.  You can then vist that directory and
see all the logs.

- The monitor port is given, 53541 in this example.  You should be able to hit http://localhost:53541 and see the
monitor

## Running from maven

You can also run the SMAC from maven with the following

    mvn clean test -Drun
    
The log directory will be `target/mini`

