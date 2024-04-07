package net.themodcraft.tmc_server_start_cloud_v1;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.config.Configuration;
import com.velocitypowered.api.proxy.config.YamlConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "tmc_server_start_cloud_V1", name = "TMC Server Start Cloud", version = "1.0.0", authors = {"TheModCraft"})
public class TMC_Server_Start_Cloud_V1 implements Command {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger;

    @Inject
    public TMC_Server_Start_Cloud_V1(ProxyServer proxy, @DataDirectory Path dataDirectory, Logger logger) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    @Override
    public void execute(CommandSource source, String[] args) {
        if (args.length != 1) {
            source.sendMessage(NamedTextColor.RED, "Usage: /startserver <servername>");
            return;
        }

        String serverName = args[0];

        Configuration configuration = getConfig();
        if (configuration != null) {
            String startCommandPath = configuration.getString("servers." + serverName + ".startCommand");

            if (startCommandPath != null) {
                File startCommandFile = new File(startCommandPath);

                if (startCommandFile.exists()) {
                    executeCommand(source, startCommandFile.getAbsolutePath());
                    source.sendMessage(NamedTextColor.GREEN, "Starting server: " + serverName);
                    broadcastMessage(NamedTextColor.YELLOW + "Server " + serverName + " is starting!");
                } else {
                    source.sendMessage(NamedTextColor.RED, "Start command file not found: " + startCommandPath);
                }
            } else {
                source.sendMessage(NamedTextColor.RED, "Server not found in the config.");
            }
        } else {
            source.sendMessage(NamedTextColor.RED, "Error loading configuration");
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
            source.sendMessage(NamedTextColor.RED, "Error executing the command: " + commandPath);
            logger.error("Error executing the command", e);
        }
    }

    private void broadcastMessage(String message) {
        proxy.broadcast(message);
    }

    private Configuration getConfig() {
        try {
            Path configFile = dataDirectory.resolve("config.yml");

            if (!Files.exists(configFile)) {
                try (InputStream is = getResourceAsStream("config.yml")) {
                    Files.copy(is, configFile);
                }
            }

            return YamlConfiguration.load(configFile);
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            return null;
        }
    }
}
