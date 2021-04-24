package dev.w1zzrd.invtweaks;

import dev.w1zzrd.invtweaks.command.SortCommandExecutor;
import dev.w1zzrd.invtweaks.listener.SortListener;
import dev.w1zzrd.invtweaks.listener.StackReplaceListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Main plugin class
 */
public final class InvTweaksPlugin extends JavaPlugin {

    /**
     * Plugin logging tag. This should be prepended to any log messages sent by this plugin
     */
    public static final String LOG_PLUGIN_NAME = "[InventoryTweaks]";

    private final Logger logger = Bukkit.getLogger();


    @Override
    public void onEnable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin enabled");

        initCommands();
        initEvents();
    }

    @Override
    public void onDisable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin disabled");

        // Un-register all listeners
        HandlerList.unregisterAll(this);
    }


    /**
     * Initialize commands registered by this plugin
     */
    private void initCommands() {
        Objects.requireNonNull(getCommand("sort")).setExecutor(new SortCommandExecutor());
    }

    /**
     * Initialize event listeners for this plugin
     */
    private void initEvents() {
        final PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new StackReplaceListener(), this);
        pluginManager.registerEvents(new SortListener(), this);
    }
}
