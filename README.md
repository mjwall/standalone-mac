# Standalone Mini Accumulo Cluster (SMAC)

## Description

This project is an experiment with the Accumulo Mini Cluster (MAC).  The intent is to make an executable jar that runs the MAC.  
Currently, running the jar will start a Zookeeper, Master, Monitor, GC and 2 TServer process.  It will then drop you into
an interactive shell.  The cluster runs until you close the shell.  More documentation is needed

## Building

To build this standalone jar, run the following

    mvn clean package
    
In the target directory, you will see a file named `standalone-1.8.0-mac-shaded-0.0.1-SNAPSHOT.jar`.  That is the 
executable jar, currently at 44M

## Running

After building the jar, run the following from the project top level directory

    java -jar target/standalone-1.8.0-mac-shaded-0.0.1-SNAPSHOT.jar
  
Your output should be something like the following


    Starting a Mini Accumulo Cluster:
    InstanceName:       smac
    Root user password: secret
    Temp dir is:        /var/folders/2y/n9lzqm2x10lfxqm9n40xvfvw0000gn/T/1473862620200-0
    Zookeeper is:       localhost:2181
    Monitor:            http://localhost:52922

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

## Config options
 
TODO: add them all

### tempMiniDir -  temp directory used by the SMAC

It will be deleted when you exit.  To make it persist pass in a `-DtempMiniDir=/some/path` option before the
 `-jar` command.  You can then vist that directory and see all the logs.

### initScript - initialization script

Make a shell script and use -DinitScript to point to it.  This script will be run after the cluster starts up
but before the shell is invoked.  Use the script to do things like add jars to lib/ext or run some other
initialization stuff.

### setJDWP - set the Java Debug Wire Protocol

This is part of the Mini Accumulo Cluster, and will startup debug ports for each process so you can attach a 
remote debugger.

To find the port for a process, look at the options it was started with.

    ps -ef | grep '\-Dproc=TabletServer'
    
You will see it there.  But you can also the following if you know the pid

    jcmd <pid> VM.command_line
    
Look for a string lik '-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=<port>'.  That is the remote debug port

## Running from maven

You can also run the SMAC from maven with the following

    mvn clean test -Drun
    
The log directory will be `target/mini`


## Running a shell

If you start up the cluster with `-DstartShell=false` you can run a shell and attach to the running instance with

    java -cp target/standalone-*-mac-shaded-*.jar com.mjwall.accumulo.SmacAccumuloShell
    
## Running a zookeeper shell
    
Sometimes you need to look at zookeeper.  You can do that with
    
    java -cp target/standalone-*-mac-shaded-*.jar com.mjwall.accumulo.SmacZookeeperCli