package net.themodcraft.tmc_server_start_cloud_v1_v;

import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TMC_Server_Start_Cloud_V1_V implements SimpleCommand {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger;

    @Inject
    public TMC_Server_Start_Cloud_V1_V(ProxyServer proxy, @DataDirectory Path dataDirectory, Logger logger) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length != 1) {
            invocation.source().sendMessage(Component.text("Usage: /startserver <servername>").color(NamedTextColor.RED));
            return;
        }

        String serverName = args[0];

        Map<String, String> configuration = loadConfig();
        if (configuration != null) {
            String startCommandPath = configuration.get("servers." + serverName + ".startCommand");

            if (startCommandPath != null) {
                File startCommandFile = new File(startCommandPath);

                if (startCommandFile.exists()) {
                    executeCommand(invocation.source(), startCommandFile.getAbsolutePath());
                    invocation.source().sendMessage(Component.text("Starting server: " + serverName).color((NamedTextColor.GREEN)) );
                    broadcastMessage(NamedTextColor.YELLOW + "Server " + serverName + " is starting!");
                } else {
                    invocation.source().sendMessage(Component.text("Start command file not found: " + startCommandPath).color(NamedTextColor.RED));
                }
            } else {
                invocation.source().sendMessage(Component.text( "Server not found in the config.").color(NamedTextColor.RED));
            }
        } else {
            invocation.source().sendMessage(Component.text("Error loading configuration").color(NamedTextColor.RED));
        }
    }

    private void executeCommand(CommandSource source, String commandPath) {
        try {
            File commandFile = new File(commandPath);

            if (Desktop.isDesktopSupported() && commandFile.isFile()) {
                Desktop.getDesktop().open(commandFile);
            } else {
                String[] cmd = {"/bin/sh", "-c", commandPath};
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            source.sendMessage(Component.text( "Error executing the command: " + commandPath).color(NamedTextColor.RED));
            logger.error("Error executing the command", e);
        }
    }

    private void broadcastMessage(String message) {
        proxy.sendMessage(Component.text(message));
    }

    private Map<String, String> loadConfig() {
        try {
            Path configFile = dataDirectory.resolve("config.yml");

            if (!Files.exists(configFile)) {
                try (InputStream is = getResourceAsStream("config.yml")) {
                    Files.copy(is, configFile);
                }
            }

            Yaml yaml = new Yaml();
            try (InputStream inputStream = new FileInputStream(configFile.toFile())) {
                return yaml.load(inputStream);
            }
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            return null;
        }
    }

    private InputStream getResourceAsStream(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
