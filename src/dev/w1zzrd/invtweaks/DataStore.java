package dev.w1zzrd.invtweaks;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for persistent data storage for a plugin
 */
public class DataStore {

    private static final Logger logger = Bukkit.getLogger();

    private final File storeFile;
    private final FileConfiguration config;

    /**
     * Create a data store with the given name. This will attempt to load a yaml file named after the store
     * @param storeName Name of the store to load. File will be named storeName + ".yml"
     * @param plugin Plugin to associate the data store with
     */
    public DataStore(final String storeName, final Plugin plugin) {
        storeFile = new File(plugin.getDataFolder(), storeName + ".yml");
        config = YamlConfiguration.loadConfiguration(storeFile);

        // Save config in case it doesn't exist
        saveData();
    }

    /**
     * Load a value from the data store
     * @param path Path in the file to load the data from
     * @param defaultValue Getter for a default value, in case the data does not exist in the store
     * @param <T> Type of the data to load
     * @return Data at the given path, if available, else the default value
     */
    public <T extends ConfigurationSerializable> T loadData(final String path, final DefaultGetter<T> defaultValue) {
        final T value = (T) config.get(path);
        return value == null ? defaultValue.get() : value;
    }

    /**
     * Save data at a given path in the store
     * @param path Path to store data at
     * @param value Data to store
     * @param <T> Type of {@link ConfigurationSerializable} data to store
     */
    public <T extends ConfigurationSerializable> void storeData(final String path, final T value) {
        config.set(path, value);
    }

    /**
     * Save the current data store in memory to persistent memory
     */
    public void saveData() {
        try {
            config.save(storeFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, Logger.GLOBAL_LOGGER_NAME + " Could not save data due to an I/O error", e);
        }
    }

    /**
     * Reload data store from persistent memory, overwriting any current state
     */
    public void loadData() {
        try {
            config.load(storeFile);
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.SEVERE, Logger.GLOBAL_LOGGER_NAME + " Could not load data due to an I/O error", e);
        }
    }

    /**
     * Functional interface for constructing default values
     * @param <T> Type to construct
     */
    public interface DefaultGetter<T> {
        /**
         * Instantiate default value
         * @return Default value that was instantiated
         */
        T get();
    }
}
