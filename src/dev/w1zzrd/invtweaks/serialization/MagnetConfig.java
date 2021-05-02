package dev.w1zzrd.invtweaks.serialization;

import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;

/**
 * Type representation of persistent configuration for /magnet
 */
public class MagnetConfig extends SimpleReflectiveConfigItem {
    /**
     * Radius from player to check for items
     */
    private double radius;

    /**
     * Interval (in ticks) to check for items around magnet players
     */
    private int interval;

    /**
     * How many subsets to divide the active player list into when running magnetism check (for performance)
     */
    private int subdivide;

    public MagnetConfig(final Map<String, Object> mappings) {
        super(mappings);
    }

    /**
     * Get item search radius
     * @return Item search radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Get item search interval (in ticks)
     * @return Item search interval
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Get item search list subdivisions
     * @return Item search list subdivisions
     */
    public int getSubdivide() {
        return subdivide;
    }

    /**
     * Set item search radius
     * @param radius Radius
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * Set item search interval (in ticks)
     * @param interval Interval
     */
    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * Set item search list subdivisions
     * @param subdivide Subdivisions
     */
    public void setSubdivide(int subdivide) {
        this.subdivide = subdivide;
    }

    /**
     * Load default configuration values from the given plugin
     * @param plugin Plugin to load defaults for
     * @param path Path in default configuration to load values from
     * @return Instance containing default configuration values
     */
    public static MagnetConfig getDefault(final Plugin plugin, final String path) {
        return (MagnetConfig) Objects.requireNonNull(plugin.getConfig().getDefaults()).get(path);
    }
}
