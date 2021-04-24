package dev.w1zzrd.invtweaks;

import dev.w1zzrd.invtweaks.command.MagnetCommandExecutor;
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

    // TODO: Magic values: make a config
    private static final double MAGNET_DISTANCE = 8.0;
    private static final long MAGNET_INTERVAL = 5;
    private static final int MAGNET_SUBDIVIDE = 2;

    private final Logger logger = Bukkit.getLogger();

    // Command executor references in case I need them or something idk
    private SortCommandExecutor sortCommandExecutor;
    private MagnetCommandExecutor magnetCommandExecutor;

    @Override
    public void onEnable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin enabled");

        initCommands();
        initEvents();
    }

    @Override
    public void onDisable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin disabled");

        disableEvents();
        disableCommands();
    }


    /**
     * Initialize commands registered by this plugin
     */
    private void initCommands() {
        sortCommandExecutor = new SortCommandExecutor();
        magnetCommandExecutor = new MagnetCommandExecutor(this, MAGNET_DISTANCE, MAGNET_INTERVAL, MAGNET_SUBDIVIDE);

        // TODO: Bind command by annotation
        Objects.requireNonNull(getCommand("sort")).setExecutor(sortCommandExecutor);
        Objects.requireNonNull(getCommand("magnet")).setExecutor(magnetCommandExecutor);
    }

    /**
     * Initialize event listeners for this plugin
     */
    private void initEvents() {
        final PluginManager pluginManager = getServer().getPluginManager();

        // TODO: Register listeners by annotation
        pluginManager.registerEvents(new StackReplaceListener(), this);
        pluginManager.registerEvents(new SortListener(), this);
    }

    /**
     * Do whatever is necessary to disable commands and their execution
     */
    private void disableCommands() {
        sortCommandExecutor = null;
        magnetCommandExecutor = null;
    }

    /**
     * Do whatever is necessary to disable event listeners
     */
    private void disableEvents() {
        // Un-register all listeners
        HandlerList.unregisterAll(this);
    }
}
