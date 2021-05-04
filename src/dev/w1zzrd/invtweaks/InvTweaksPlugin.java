package dev.w1zzrd.invtweaks;

import dev.w1zzrd.invtweaks.command.MagnetCommandExecutor;
import dev.w1zzrd.invtweaks.command.SearchCommandExecutor;
import dev.w1zzrd.invtweaks.command.SortCommandExecutor;
import dev.w1zzrd.invtweaks.listener.MagnetismListener;
import dev.w1zzrd.invtweaks.serialization.MagnetConfig;
import dev.w1zzrd.invtweaks.listener.SortListener;
import dev.w1zzrd.invtweaks.listener.StackReplaceListener;
import dev.w1zzrd.invtweaks.serialization.MagnetData;
import dev.w1zzrd.invtweaks.serialization.UUIDList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
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
    private static final String PERSISTENT_DATA_NAME = "data";

    private final Logger logger = Bukkit.getLogger();

    // Command executor references in case I need them or something idk
    private SortCommandExecutor sortCommandExecutor;
    private MagnetCommandExecutor magnetCommandExecutor;
    private SearchCommandExecutor searchCommandExecutor;
    private DataStore data;

    @Override
    public void onEnable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin enabled");

        enablePersistentData();
        initCommands();
        initEvents();
    }

    @Override
    public void onDisable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin disabled");

        disableEvents();
        disableCommands();
        disablePersistentData();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        getConfig().options().copyDefaults(true);

        if (magnetCommandExecutor != null)
            magnetCommandExecutor.reloadConfig();
    }

    /**
     * Get a reference to the persistent data store object for this plugin
     * @return An instance of {@link DataStore} for this plugin
     */
    public DataStore getPersistentData() {
        return data;
    }

    /**
     * Initialize commands registered by this plugin
     */
    private void initCommands() {
        sortCommandExecutor = new SortCommandExecutor();
        magnetCommandExecutor = new MagnetCommandExecutor(this, getPersistentData());
        searchCommandExecutor = new SearchCommandExecutor();

        // TODO: Bind command by annotation
        Objects.requireNonNull(getCommand("sort")).setExecutor(sortCommandExecutor);
        Objects.requireNonNull(getCommand("magnet")).setExecutor(magnetCommandExecutor);
        Objects.requireNonNull(getCommand("search")).setExecutor(searchCommandExecutor);
    }

    /**
     * Initialize event listeners for this plugin
     */
    private void initEvents() {
        final PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new StackReplaceListener(), this);
        pluginManager.registerEvents(new SortListener(), this);
        pluginManager.registerEvents(new MagnetismListener(magnetCommandExecutor), this);
    }

    /**
     * Do whatever is necessary to disable commands and their execution
     */
    private void disableCommands() {
        magnetCommandExecutor.onDisable();

        sortCommandExecutor = null;
        magnetCommandExecutor = null;
        searchCommandExecutor = null;
    }

    /**
     * Do whatever is necessary to disable event listeners
     */
    private void disableEvents() {
        // Un-register all listeners
        HandlerList.unregisterAll(this);
    }

    /**
     * Register type serializers/deserializers for configurations and YAML files
     *
     * @see #unregisterSerializers()
     */
    private void registerSerializers() {
        ConfigurationSerialization.registerClass(MagnetConfig.class);
        ConfigurationSerialization.registerClass(MagnetData.class);
        ConfigurationSerialization.registerClass(UUIDList.class);
    }

    /**
     * Unregister type serializers/deserializers for configurations and YAML files
     *
     * @see #registerSerializers()
     */
    private void unregisterSerializers() {
        ConfigurationSerialization.unregisterClass(MagnetConfig.class);
        ConfigurationSerialization.unregisterClass(MagnetData.class);
        ConfigurationSerialization.unregisterClass(UUIDList.class);
    }

    /**
     * Initialize persistent data storage sources and handlers
     */
    private void enablePersistentData() {
        registerSerializers();

        getConfig().options().copyDefaults(true);

        saveConfig();

        // Implicit load
        data = new DataStore(PERSISTENT_DATA_NAME, this);
    }

    /**
     * De-activate and finalize persistent data storage sources and handlers
     */
    private void disablePersistentData() {
        data.saveData();
        data = null;

        saveConfig();

        unregisterSerializers();
    }
}
