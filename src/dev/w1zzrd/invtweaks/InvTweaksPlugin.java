package dev.w1zzrd.invtweaks;

import dev.w1zzrd.invtweaks.command.*;
import dev.w1zzrd.invtweaks.enchantment.CapitatorEnchantment;
import dev.w1zzrd.invtweaks.listener.*;
import dev.w1zzrd.invtweaks.serialization.MagnetConfig;
import dev.w1zzrd.invtweaks.serialization.MagnetData;
import dev.w1zzrd.invtweaks.serialization.SearchConfig;
import dev.w1zzrd.spigot.wizcompat.enchantment.EnchantmentRegistryEntry;
import dev.w1zzrd.spigot.wizcompat.enchantment.ServerEnchantmentRegistry;
import dev.w1zzrd.spigot.wizcompat.serialization.PersistentData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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

    private static final String ENCHANTMENT_CAPITATOR_NAME = "Capitator";

    private final Logger logger = Bukkit.getLogger();

    // Command executor references in case I need them or something idk
    private SortCommandExecutor sortCommandExecutor;
    private MagnetCommandExecutor magnetCommandExecutor;
    private SearchCommandExecutor searchCommandExecutor;
    private NamedChestCommand namedChestCommandExecutor;
    private CapitatorCommand capitatorCommand;
    private PersistentData data;
    private EnchantmentRegistryEntry<CapitatorEnchantment> capitatorEnchantment = null;

    @Override
    public void onEnable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin enabled");

        enablePersistentData();
        initEnchantments();
        initCommands();
        initEvents();
    }

    @Override
    public void onDisable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin disabled");

        disableEvents();
        disableCommands();
        disableEnchantments();
        disablePersistentData();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        getConfig().options().copyDefaults(true);

        if (magnetCommandExecutor != null)
            magnetCommandExecutor.reloadConfig();

        if (searchCommandExecutor != null)
            searchCommandExecutor.reloadConfig();
    }

    /**
     * Get a reference to the persistent data store object for this plugin
     * @return An instance of {@link PersistentData} for this plugin
     */
    public PersistentData getPersistentData() {
        return data;
    }

    private void initEnchantments() {
        if (getConfig().getBoolean("capitator", true))
            capitatorEnchantment = ServerEnchantmentRegistry.registerEnchantment(
                    this,
                    new CapitatorEnchantment(
                            ENCHANTMENT_CAPITATOR_NAME,
                            new NamespacedKey(this, ENCHANTMENT_CAPITATOR_NAME)
                    )
            );
    }

    private void disableEnchantments() {
        if (getConfig().getBoolean("capitator", true))
            ServerEnchantmentRegistry.unRegisterEnchantment(this, capitatorEnchantment);

        capitatorEnchantment = null;
    }

    /**
     * Initialize event listeners for this plugin
     */
    private void initEvents() {
        final boolean activateCapitator = getConfig().getBoolean("capitator", true);
        final boolean activateGhostClick = getConfig().getBoolean("ghostClick", true);

        final PluginManager pluginManager = getServer().getPluginManager();

        if (activateGhostClick)
            pluginManager.registerEvents(new GhostClickListener(), this);
        pluginManager.registerEvents(new StackReplaceListener(this), this);
        pluginManager.registerEvents(new SortListener(), this);
        pluginManager.registerEvents(new MagnetismListener(magnetCommandExecutor), this);
        pluginManager.registerEvents(new TabCompletionListener(), this);
        pluginManager.registerEvents(new TreeCapitatorListener(activateCapitator ? capitatorEnchantment.getEnchantment() : null), this);
    }

    /**
     * Do whatever is necessary to disable event listeners
     */
    private void disableEvents() {
        // Un-register all listeners
        HandlerList.unregisterAll(this);
    }

    /**
     * Initialize commands registered by this plugin
     */
    private void initCommands() {
        final boolean activateCapitator = getConfig().getBoolean("capitator", true);

        sortCommandExecutor = new SortCommandExecutor();
        magnetCommandExecutor = new MagnetCommandExecutor(this, "magnet", getPersistentData());
        searchCommandExecutor = new SearchCommandExecutor(this, "search");
        namedChestCommandExecutor = new NamedChestCommand(this);

        if (activateCapitator)
            capitatorCommand = new CapitatorCommand(capitatorEnchantment.getEnchantment());

        // TODO: Bind command by annotation
        Objects.requireNonNull(getCommand("sort")).setExecutor(sortCommandExecutor);
        Objects.requireNonNull(getCommand("magnet")).setExecutor(magnetCommandExecutor);
        Objects.requireNonNull(getCommand("search")).setExecutor(searchCommandExecutor);
        Objects.requireNonNull(getCommand("chestname")).setExecutor(namedChestCommandExecutor);

        if (activateCapitator)
            Objects.requireNonNull(getCommand("capitator")).setExecutor(capitatorCommand);
    }

    /**
     * Do whatever is necessary to disable commands and their execution
     */
    private void disableCommands() {
        magnetCommandExecutor.onDisable();

        capitatorCommand = null;
        namedChestCommandExecutor = null;
        searchCommandExecutor = null;
        magnetCommandExecutor = null;
        sortCommandExecutor = null;
    }

    /**
     * Register type serializers/deserializers for configurations and YAML files
     *
     * @see #unregisterSerializers()
     */
    private void registerSerializers() {
        ConfigurationSerialization.registerClass(MagnetConfig.class);
        ConfigurationSerialization.registerClass(MagnetData.class);
        ConfigurationSerialization.registerClass(SearchConfig.class);
    }

    /**
     * Unregister type serializers/deserializers for configurations and YAML files
     *
     * @see #registerSerializers()
     */
    private void unregisterSerializers() {
        ConfigurationSerialization.unregisterClass(MagnetConfig.class);
        ConfigurationSerialization.unregisterClass(MagnetData.class);
        ConfigurationSerialization.unregisterClass(SearchConfig.class);
    }

    /**
     * Initialize persistent data storage sources and handlers
     */
    private void enablePersistentData() {
        registerSerializers();

        getConfig().options().copyDefaults(true);

        saveConfig();

        // Implicit load
        data = new PersistentData(PERSISTENT_DATA_NAME, this);
    }

    /**
     * De-activate and finalize persistent data storage sources and handlers
     */
    private void disablePersistentData() {
        data.saveData();
        data = null;

        //saveConfig();

        unregisterSerializers();
    }
}
