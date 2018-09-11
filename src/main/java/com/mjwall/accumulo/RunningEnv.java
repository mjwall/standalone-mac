package com.mjwall.accumulo;

import org.apache.accumulo.minicluster.impl.MiniAccumuloClusterImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RunningEnv {

    public final static String INSTANCE_NAME_PROP       = "INSTANCE_NAME";
    public final static String ROOT_USERNAME_PROP       = "ROOT_USERNAME";
    public final static String ROOT_PASSWORD_PROP       = "ROOT_PASSWORD";
    public final static String TEMP_MINI_DIR_PROP       = "TEMP_MINI_DIR";
    public final static String NUM_TSERVERS_PROP        = "NUM_TSERVERS";
    public final static String ZOOKEEPERS_PROP          = "ZOOKEEPERS";
    public final static String REUSE_TEMP_MINI_DIR_PROP = "REUSE_TEMP_MINI_DIR";
    public final static String MONITOR_URL_PROP         = "MONITOR_URL";
    public final static String INIT_SCRIPT_PROP         = "INIT_SCRIPT";
    public final static String START_SHELL_PROP         = "START_SHELL";

    public final static Path RUNNING_ENV_FILE = Paths.get(System.getProperty("user.home") + ".smac-running-env");

    private final String instanceName;
    private final String rootUsername;
    private final String rootPassword;
    private final File tempMiniDir;
    private final int numTservers;
    private final String zookeepers;
    private final boolean reuseTempMiniDir;
    private final String monitorUrl;
    private final String initScript;
    private final boolean startShell;

    public RunningEnv(MiniAccumuloClusterImpl cluster, boolean reuseTempMiniDir, String monitorUrl,
                      String initScript, boolean startShell) {
        this.instanceName = cluster.getInstanceName();
        this.rootUsername = cluster.getConfig().getRootUserName();
        this.rootPassword = cluster.getConfig().getRootPassword();
        this.tempMiniDir = cluster.getConfig().getDir();
        this.numTservers = cluster.getConfig().getNumTservers();
        this.zookeepers = cluster.getZooKeepers();
        this.reuseTempMiniDir = reuseTempMiniDir;
        this.monitorUrl = monitorUrl;
        this.initScript = initScript;
        this.startShell = startShell;
    }

    // TODO: constructor from file

    public void toStdOut() {
        System.out.println("InstanceName:        " + this.instanceName);
        System.out.println("Root username:       " + this.rootUsername);
        System.out.println("Root user password   " + this.rootPassword);
        System.out.println("Temp mini dir is:    " + this.tempMiniDir.getAbsolutePath());
        System.out.println("Num tservers:        " + this.numTservers);
        System.out.println("Zookeeper is:        " + this.zookeepers);
        System.out.println("Reuse temp mini dir: " + this.reuseTempMiniDir);
        System.out.println("Monitor url:         " + this.monitorUrl);
        System.out.println("Init script:         " + this.initScript);
        System.out.println("Start shell:         " + this.startShell);
    }

    public void writeEnvFile() {
        StringBuilder sb = new StringBuilder();
        sb.append(INSTANCE_NAME_PROP).append("=").append(this.instanceName).append("\n");
        sb.append(ROOT_USERNAME_PROP).append("=").append(this.rootUsername).append("\n");
        sb.append(ROOT_PASSWORD_PROP).append("=").append(this.rootPassword).append("\n");
        sb.append(TEMP_MINI_DIR_PROP).append("=").append(this.tempMiniDir).append("\n");
        sb.append(NUM_TSERVERS_PROP).append("=").append(this.numTservers).append("\n");
        sb.append(ZOOKEEPERS_PROP).append("=").append(this.zookeepers).append("\n");
        sb.append(REUSE_TEMP_MINI_DIR_PROP).append("=").append(this.reuseTempMiniDir).append("\n");
        sb.append(MONITOR_URL_PROP).append("=").append(this.monitorUrl).append("\n");
        sb.append(INIT_SCRIPT_PROP).append("=").append(this.initScript).append("\n");
        sb.append(START_SHELL_PROP).append("=").append(this.startShell).append("\n");
        if (Files.exists(RUNNING_ENV_FILE)) {
            System.err.println("Running SMAC env file exists, overwriting at " + RUNNING_ENV_FILE);
            try {
                Files.delete(RUNNING_ENV_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.write(RUNNING_ENV_FILE, sb.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
