package dev.w1zzrd.invtweaks.config;

import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;

public class MagnetConfig extends SimpleReflectiveConfigItem {
    private double radius;
    private int interval;
    private int subdivide;

    public MagnetConfig(final Map<String, Object> mappings) {
        super(mappings);
    }

    public double getRadius() {
        return radius;
    }

    public int getInterval() {
        return interval;
    }

    public int getSubdivide() {
        return subdivide;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setSubdivide(int subdivide) {
        this.subdivide = subdivide;
    }

    public static MagnetConfig getDefault(final Plugin plugin, final String path) {
        return (MagnetConfig) Objects.requireNonNull(plugin.getConfig().getDefaults()).get(path);
    }
}
