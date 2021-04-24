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

    public static final String LOG_PLUGIN_NAME = "[InvTweaks]";

    private final Logger logger = Bukkit.getLogger();


    @Override
    public void onEnable() {
        logger.info(LOG_PLUGIN_NAME + " Plugin enabled");

        initCommands();
        initEvents();
    }

    @Override
    public void onDisable() {
        logger.info(LOG_PLUGIN_NAME + " Plugin disabled");

        // Un-register all listeners
        HandlerList.unregisterAll(this);
    }


    private void initCommands() {
        Objects.requireNonNull(getCommand("sort")).setExecutor(new SortCommandExecutor());
    }

    private void initEvents() {
        final PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new StackReplaceListener(), this);
        pluginManager.registerEvents(new SortListener(), this);
    }
}
