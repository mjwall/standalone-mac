package com.mjwall.accumulo;

import org.apache.accumulo.shell.Shell;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static com.mjwall.accumulo.RunningEnv.RUNNING_ENV_FILE;

public class SmacAccumuloShell {

    private final String username;
    private final String password;
    private final String instanceName;
    private final String zookeepers;

    public SmacAccumuloShell() {
        if (Files.exists(RUNNING_ENV_FILE)) {
            Properties runningEnv = new Properties();
            try {
                runningEnv.load(new FileInputStream(RUNNING_ENV_FILE.toFile()));
                username = runningEnv.getProperty(RunningEnv.ROOT_USERNAME_PROP);
                password = runningEnv.getProperty(RunningEnv.ROOT_PASSWORD_PROP);
                instanceName = runningEnv.getProperty(RunningEnv.INSTANCE_NAME_PROP);
                zookeepers = runningEnv.getProperty(RunningEnv.ZOOKEEPERS_PROP);
            } catch (IOException e) {
                throw new RuntimeException("Could not load " + RUNNING_ENV_FILE);
            }
        } else {
            throw new RuntimeException("Running smac env not found: " + RUNNING_ENV_FILE);
        }
    }

    public void start() throws IOException {
        String[] shellArgs = new String[]{
                "-u", this.username,
                "-p", this.password,
                "-zi", this.instanceName,
                "-zh", this.zookeepers
        };
        Shell shell = new Shell();
        shell.config(shellArgs);
        shell.start(); // this is gonna block till you exit
        shell.shutdown();
    }

    public static void main(String[] args) throws IOException {
        new SmacAccumuloShell().start();
    }
}
