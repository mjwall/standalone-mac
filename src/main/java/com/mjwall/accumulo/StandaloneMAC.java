package com.mjwall.accumulo;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.zookeeper.KeeperException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.util.MonitorUtil;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.minicluster.impl.MiniAccumuloClusterImpl;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
import org.apache.accumulo.monitor.Monitor;

/**
 * Run a standalone mini accumulo cluster until killed
 * <p>
 * A Mini Accumulo cluster will start the following services: Zookeeper Master Monitor GC 2 - TServers Then an interactive shell will start and the cluster will
 * run until the shell is closed
 */
public class StandaloneMAC {

  public static void main(String[] args) {

    File tempDir = null;
    MiniAccumuloClusterImpl cluster = null;
    boolean purgeTemp = false;

    try {
      String tempMiniDir = System.getProperty("tempMiniDir", null);

      if (tempMiniDir == null) {
        tempDir = com.google.common.io.Files.createTempDir();
        tempDir.deleteOnExit();
        purgeTemp = true;
      } else {
        tempDir = new File(tempMiniDir);
        if (tempDir.exists()) {
          throw new RuntimeException("tempMiniDir directory must be empty: " + tempMiniDir);
          // be safer about deleting
          // recursiveDelete(tempDir.toPath());
        }
        tempDir.mkdir();
      }

      final String rootPassword = System.getProperty("rootPassword", "secret");
      final String instanceName = System.getProperty("instanceName", "smac");
      final int zookeeperPort = Integer.parseInt(System.getProperty("zookeeperPort", "2181"));

      MiniAccumuloConfigImpl config = new MiniAccumuloConfigImpl(tempDir, rootPassword);
      config.setInstanceName(instanceName);
      config.setNumTservers(2);
      try (Socket ignored = new Socket("localhost", zookeeperPort)) {
        throw new RuntimeException("Zookeeper can't bind to port already in use: " + zookeeperPort);
      } catch (IOException available) {
        config.setZooKeeperPort(zookeeperPort);
      }

      cluster = new MiniAccumuloClusterImpl(config);

      cluster.start(); // starts zookeeper, tablet servers, gc and master

      cluster.exec(Monitor.class);

      // Get monitor location to ensure it is running.
      String monitorLocation = null;
      for (int i = 0; i < 5; i++) {
        Thread.sleep(5 * 1000);
        try {
          Instance instance = new ZooKeeperInstance(cluster.getClientConfig());
          monitorLocation = MonitorUtil.getLocation(instance);
          if (monitorLocation != null) {
            break;
          }
        } catch (KeeperException e) {
          System.out.println("Waiting for zookeeper");
          // e.printStackTrace();
        }
      }

      System.out.println("Starting a Mini Accumulo Cluster:");
      System.out.println("InstanceName:       " + cluster.getInstanceName());
      System.out.println("Root user password: " + cluster.getConfig().getRootPassword());
      System.out.println("Temp dir is:        " + cluster.getConfig().getDir());
      System.out.println("Zookeeper is:       " + cluster.getZooKeepers());
      if (monitorLocation == null) {
        System.err.println("Monitor:            not started");
      } else {
        System.out.println("Monitor:            http://localhost:" + monitorLocation.split(":")[1]);
      }

      String extraScript = System.getProperty("extraScript", null);
      if (extraScript != null) {
        Path script = Paths.get(extraScript);
        if (Files.exists(script)) {
          ProcessBuilder pb = new ProcessBuilder(script.toAbsolutePath().toString());
          pb.inheritIO();
          System.out.println("Running extra script " + extraScript);
          System.out.println("--------------------");
          Process p = pb.start();
          int exitCode = p.waitFor();
          System.out.println("--------------------");
          System.out.println("extra script ended with" + exitCode);
        } else {
          System.err.println("Tried to run the following extra script but it didn't exist: " + extraScript);
        }
      }

      System.out.println("Starting a shell");
      String[] shellArgs = new String[] {"-u", "root", "-p", cluster.getConfig().getRootPassword(), "-zi", cluster.getInstanceName(), "-zh",
          cluster.getZooKeepers()};
      Shell shell = new Shell();
      shell.config(shellArgs);
      shell.start(); // this is the interactive
      shell.shutdown();

    } catch (IOException | InterruptedException error) {
      System.err.println(error.getMessage());
      error.printStackTrace();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    } finally {
      System.out.println("Stopping Mini Accumulo");
      try {
        cluster.stop();
        if (purgeTemp) {
          Thread.sleep(3000);
          recursiveDelete(tempDir.toPath());
        }
      } catch (IOException | InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

  }

  private static void recursiveDelete(Path dir) {
    System.out.println("Deleting dir recursively: " + dir.toString());
    try {
      java.nio.file.Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          java.nio.file.Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          java.nio.file.Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
