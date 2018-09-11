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
      // not sure this works yet
      boolean reuseTempMiniDir = Boolean.parseBoolean(System.getProperty("reuseTempMiniDir", "false"));

      if (tempMiniDir == null) {
        tempDir = com.google.common.io.Files.createTempDir();
        tempDir.deleteOnExit();
        purgeTemp = true;
      } else {
        tempDir = new File(tempMiniDir);
        if (tempDir.exists()) {
          if (!reuseTempMiniDir) {
            throw new RuntimeException("tempMiniDir directory must be empty: " + tempMiniDir);
            // be safer about deleting
            // recursiveDelete(tempDir.toPath());
          } else {
            System.out.println("tempMiniDir existed at " + tempMiniDir + " and reuseTempMiniDir was true");
          }
        } else {
          tempDir.mkdir();
        }
      }

      //final String rootUsername = System.getProperty("rootUsername", "root");
      final String rootPassword = System.getProperty("rootPassword", "secret");
      final String instanceName = System.getProperty("instanceName", "smac");
      final int zookeeperPort = Integer.parseInt(System.getProperty("zookeeperPort", "2181"));

      MiniAccumuloConfigImpl config = new MiniAccumuloConfigImpl(tempDir, rootPassword);
      // TODO: make this work
      //config.setRootUserName(rootUsername);
      config.setInstanceName(instanceName);
      config.setNumTservers(2);
      try (Socket ignored = new Socket("localhost", zookeeperPort)) {
        throw new RuntimeException("Zookeeper can't bind to port already in use: " + zookeeperPort);
      } catch (IOException available) {
        config.setZooKeeperPort(zookeeperPort);
      }
      final boolean setJDWP = Boolean.valueOf(System.getProperty("setJDWP", "false"));
      if (setJDWP) {
          config.setJDWPEnabled(true);
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

      String monitorUrl = "";
      if (monitorLocation == null) {
        System.err.println("Monitor:            not started");
      } else {
        monitorUrl = "http://localhost:" + monitorLocation.split(":")[1];
      }
      String initScript = System.getProperty("initScript", null);
      boolean startShell = Boolean.valueOf(System.getProperty("startShell", "true"));

      RunningEnv env = new RunningEnv(cluster, reuseTempMiniDir, monitorUrl, initScript, startShell);
      env.toStdOut();
      env.writeEnvFile();

      if (initScript != null) {
        Path script = Paths.get(initScript);
        if (Files.exists(script)) {
          ProcessBuilder pb = new ProcessBuilder(script.toAbsolutePath().toString());
          pb.inheritIO();
          System.out.println("Running init script " + initScript);
          System.out.println("--------------------");
          Process p = pb.start();
          int exitCode = p.waitFor();
          System.out.println("--------------------");
          System.out.println("init script ended with" + exitCode);
        } else {
          System.err.println("Tried to run the following init script but it didn't exist: " + initScript);
        }
      }

      if (startShell) {
        System.out.println("Your Standalone Mini Accumulo Cluster will stay up until you close the shell");
        SmacAccumuloShell.main(new String[]{});
      } else {
        // TODO: look at Parisi's stop port stuff
        System.out.println("Not running a shell, Ctrl-C to stop the Standalone Mini Accumulo Cluster");
        Thread.currentThread().join();
        // TODO: last non deamon thread so the finally is not running here
      }


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
      try {
          System.out.println("Removing " + RunningEnv.RUNNING_ENV_FILE.toAbsolutePath().toString());
          Files.delete(RunningEnv.RUNNING_ENV_FILE);
      } catch (IOException e) {
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
