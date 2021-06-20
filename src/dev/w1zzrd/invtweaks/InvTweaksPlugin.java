package dev.w1zzrd.invtweaks;

import dev.w1zzrd.invtweaks.command.CapitatorCommand;
import dev.w1zzrd.invtweaks.command.MagnetCommandExecutor;
import dev.w1zzrd.invtweaks.command.SearchCommandExecutor;
import dev.w1zzrd.invtweaks.command.SortCommandExecutor;
import dev.w1zzrd.invtweaks.enchantment.CapitatorEnchantment;
import dev.w1zzrd.invtweaks.listener.*;
import dev.w1zzrd.invtweaks.serialization.MagnetConfig;
import dev.w1zzrd.invtweaks.serialization.MagnetData;
import dev.w1zzrd.invtweaks.serialization.SearchConfig;
import dev.w1zzrd.invtweaks.serialization.UUIDList;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Map;
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
    private CapitatorCommand capitatorCommand;
    private DataStore data;
    private NamespacedKey capitatorEnchantmentKey;
    private Enchantment capitatorEnchantment;

    @Override
    public void onEnable() {
        logger.fine(LOG_PLUGIN_NAME + " Plugin enabled");

        initEnchantments();
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
        disableEnchantments();
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
     * @return An instance of {@link DataStore} for this plugin
     */
    public DataStore getPersistentData() {
        return data;
    }

    private void initEnchantments() {
        capitatorEnchantmentKey = new NamespacedKey(this, ENCHANTMENT_CAPITATOR_NAME);
        capitatorEnchantment = new CapitatorEnchantment(ENCHANTMENT_CAPITATOR_NAME, capitatorEnchantmentKey);

        try {
            final Field acceptingField = Enchantment.class.getDeclaredField("acceptingNew");
            acceptingField.setAccessible(true);

            acceptingField.set(null, true);

            Enchantment.registerEnchantment(capitatorEnchantment);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void disableEnchantments() {
        try {
            final Field byKeyField = Enchantment.class.getDeclaredField("byKey");
            final Field byNameField = Enchantment.class.getDeclaredField("byName");

            byKeyField.setAccessible(true);
            byNameField.setAccessible(true);

            final Object byKey = byKeyField.get(null);
            final Object byName = byNameField.get(null);

            if (byKey instanceof final Map<?, ?> byKeyMap && byName instanceof final Map<?, ?> byNameMap) {
                byKeyMap.remove(capitatorEnchantmentKey);
                byNameMap.remove(ENCHANTMENT_CAPITATOR_NAME);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        capitatorEnchantment = null;
        capitatorEnchantmentKey = null;
    }

    /**
     * Initialize event listeners for this plugin
     */
    private void initEvents() {
        final PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new GhostClickListener(), this);
        pluginManager.registerEvents(new StackReplaceListener(this), this);
        pluginManager.registerEvents(new SortListener(), this);
        pluginManager.registerEvents(new MagnetismListener(magnetCommandExecutor), this);
        pluginManager.registerEvents(new TabCompletionListener(), this);
        pluginManager.registerEvents(new TreeCapitatorListener(capitatorEnchantment), this);
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
        sortCommandExecutor = new SortCommandExecutor();
        magnetCommandExecutor = new MagnetCommandExecutor(this, "magnet", getPersistentData());
        searchCommandExecutor = new SearchCommandExecutor(this, "search");
        capitatorCommand = new CapitatorCommand(capitatorEnchantment);

        // TODO: Bind command by annotation
        Objects.requireNonNull(getCommand("sort")).setExecutor(sortCommandExecutor);
        Objects.requireNonNull(getCommand("magnet")).setExecutor(magnetCommandExecutor);
        Objects.requireNonNull(getCommand("search")).setExecutor(searchCommandExecutor);
        Objects.requireNonNull(getCommand("capitator")).setExecutor(capitatorCommand);
    }

    /**
     * Do whatever is necessary to disable commands and their execution
     */
    private void disableCommands() {
        magnetCommandExecutor.onDisable();

        capitatorCommand = null;
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
        ConfigurationSerialization.registerClass(UUIDList.class);
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
        ConfigurationSerialization.unregisterClass(UUIDList.class);
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
        data = new DataStore(PERSISTENT_DATA_NAME, this);
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
