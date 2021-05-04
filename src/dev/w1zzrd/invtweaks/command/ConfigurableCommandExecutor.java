package dev.w1zzrd.invtweaks.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.Plugin;

public abstract class ConfigurableCommandExecutor<T extends ConfigurationSerializable> implements CommandExecutor {
    private final Plugin plugin;
    private final String path;
    private T config;

    public ConfigurableCommandExecutor(
            final Plugin plugin,
            final String path
    ) {
        this.plugin = plugin;
        this.path = path;
        reloadConfig();
    }

    public void reloadConfig() {
        config = (T) plugin.getConfig().get(path, plugin.getConfig().getDefaults().get(path));
    }

    protected T getConfig() {
        return config;
    }

    protected Plugin getPlugin() {
        return plugin;
    }
}
