package dev.w1zzrd.invtweaks;

import dev.w1zzrd.invtweaks.command.SortCommandExecutor;
import dev.w1zzrd.invtweaks.listener.SortListener;
import dev.w1zzrd.invtweaks.listener.StackReplaceListener;
import dev.w1zzrd.logging.LoggerFactory;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

public class InvTweaksPlugin extends JavaPlugin {

    private final Logger logger = LoggerFactory.getLogger(InvTweaksPlugin.class);


    @Override
    public void onEnable() {
        logger.info("Inventory Tweaks enabled");

        Objects.requireNonNull(getCommand("sort")).setExecutor(new SortCommandExecutor());

        final PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new StackReplaceListener(), this);
        pluginManager.registerEvents(new SortListener(), this);
    }

    @Override
    public void onDisable() {
        logger.info("Inventory Tweaks disabled");

        HandlerList.unregisterAll(this);
    }
}
