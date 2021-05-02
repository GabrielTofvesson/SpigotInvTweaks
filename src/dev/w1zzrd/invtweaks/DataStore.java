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

public class DataStore {

    private static final Logger logger = Bukkit.getLogger();

    private final File storeFile;
    private final FileConfiguration config;

    public DataStore(final String storeName, final Plugin plugin) {
        storeFile = new File(plugin.getDataFolder(), storeName + ".yml");
        config = YamlConfiguration.loadConfiguration(storeFile);

        // Save config in case it doesn't exist
        saveData();
    }

    public <T extends ConfigurationSerializable> T loadData(final String path, final DefaultGetter<T> defaultValue) {
        final T value = (T) config.get(path);
        return value == null ? defaultValue.get() : value;
    }

    public <T extends ConfigurationSerializable> void storeData(final String path, final T value) {
        config.set(path, value);
    }

    public void saveData() {
        try {
            config.save(storeFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, Logger.GLOBAL_LOGGER_NAME + " Could not save data due to an I/O error", e);
        }
    }

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
        T get();
    }
}
