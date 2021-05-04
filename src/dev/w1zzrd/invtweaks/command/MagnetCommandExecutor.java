package dev.w1zzrd.invtweaks.command;

import dev.w1zzrd.invtweaks.DataStore;
import dev.w1zzrd.invtweaks.serialization.MagnetConfig;
import dev.w1zzrd.invtweaks.serialization.MagnetData;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.w1zzrd.invtweaks.InvTweaksPlugin.LOG_PLUGIN_NAME;

/**
 * Handler for executions of /magnet command
 */
public class MagnetCommandExecutor extends ConfigurableCommandExecutor<MagnetConfig> {

    private static final Logger logger = Bukkit.getLogger();

    private final DataStore data;
    private final MagnetData magnetData;

    private int divIndex = 0;

    private BukkitTask refreshTask = null;

    /**
     * Initialize the magnet executor and manger
     * @param plugin Owner plugin for this executor
     */
    public MagnetCommandExecutor(final Plugin plugin, final String path, final DataStore data) {
        super(plugin, path);
        this.data = data;
        this.magnetData = data.loadData("magnets", MagnetData::blank);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;

        final boolean isMagnetActive = toggleMagnet((Player) sender);

        logger.fine(LOG_PLUGIN_NAME + " Player " + sender.getName() + (isMagnetActive ? " " : " de-") + "activated magnet");
        sender.spigot().sendMessage(new TextComponent((isMagnetActive ? "Enabled" : "Disabled") + " magnetism"));

        return true;
    }

    /**
     * Toggle magnet state for the given player
     * @param player Player to toggle state for
     * @return True if, after this method call, the UUID is part of the list of active magnets, else false.
     */
    public boolean toggleMagnet(final Player player) {
        return toggleMagnet(player.getUniqueId());
    }

    /**
     * Toggle magnet state for the given UUID
     * @param uuid UUID to toggle state for
     * @return True if, after this method call, the UUID is part of the list of active magnets, else false.
     */
    public boolean toggleMagnet(final UUID uuid) {
        boolean result = false;
        try {
             result = magnetData.toggleMagnet(uuid);
             return result;
        } finally {
            updateMagnetismTask(result);
        }
    }


    /**
     * Get an list of the currently active magnets
     * @return Unmodifiable list view of the active magnets
     */
    public List<UUID> getActiveMagnets() {
        return magnetData.getActiveMagnetsView();
    }

    /**
     * Remove a player from the list of active magnets
     * @param player Player to remove from active magnets
     * @return True if a call to this method changed the list of active magnets
     */
    public boolean removeMagnet(final Player player) {
        return removeMagnet(player.getUniqueId());
    }

    /**
     * Remove a UUID from the list of active magnets
     * @param uuid UUID to remove from active magnets
     * @return True if a call to this method changed the list of active magnets
     */
    public boolean removeMagnet(final UUID uuid) {
        try {
            return magnetData.removeMagnet(uuid);
        } finally {
            updateMagnetismTask(true);
        }
    }

    /**
     * Remove all given UUIDs from the list of active magnets
     * @param uuids UUIDs to remove from active magnets
     * @return True if a call to this method changed the list of active magnets
     */
    public boolean removeMagnets(final Iterable<UUID> uuids) {
        try {
            return magnetData.removeMagnets(uuids);
        } finally {
            updateMagnetismTask(true);
        }
    }

    /**
     * Add a player to the list of active magnets
     * @param player Player to mark as a magnet
     * @return True if a call to this method changed the list of active magnets
     */
    public boolean addMagnet(final Player player) {
        return addMagnet(player.getUniqueId());
    }

    /**
     * Add a UUID to the list of active magnets
     * @param uuid UUID to mark as a magnet
     * @return True if a call to this method changed the list of active magnets
     */
    public boolean addMagnet(final UUID uuid) {
        try {
            return magnetData.addMagnet(uuid);
        } finally {
            updateMagnetismTask(false);
        }
    }

    /**
     * Check if a given player is a magnet
     * @param player Player to check magnet state for
     * @return True if the given player is marked as a magnet
     */
    public boolean isMagnet(final Player player) {
        return isMagnet(player.getUniqueId());
    }

    /**
     * Check if a given UUID is registered as a magnet
     * @param uuid UUID of a player to check
     * @return True if the given UUID is marked as a magnet
     */
    public boolean isMagnet(final UUID uuid) {
        return magnetData.isMagnet(uuid);
    }

    /**
     * Check if a magnet refresh task needs to be registered for magnet players<br><br>
     *
     * A refresh is created if there are active magnet players, there it no active refresh task and the plugin is
     * enabled. The refresh task is cancelled and state reset if there are no active magnet players, or the plugin is
     * disabled.
     */
    public void updateMagnetismTask(final boolean checkOnline) {
        final MagnetConfig config = getConfig();
        final Plugin plugin = getPlugin();

        if (refreshTask == null && (!checkOnline || magnetData.onlineMagnets() > 0) && plugin.isEnabled()) {
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::taskApplyMagnetism, 0, config.getInterval());
            logger.info(LOG_PLUGIN_NAME + " Activated magnetism check task");
        }
        else if (refreshTask != null && ((checkOnline && magnetData.onlineMagnets() == 0) || !plugin.isEnabled())) {
            Bukkit.getScheduler().cancelTask(refreshTask.getTaskId());
            refreshTask = null;
            divIndex = 0;
            logger.info(LOG_PLUGIN_NAME + " De-activated magnetism check task");
        }
    }

    /**
     * Task for teleporting items to magnet players
     */
    private void taskApplyMagnetism() {
        final List<UUID> toRemove = new ArrayList<>();

        final List<UUID> activeMagnets = magnetData.getOnlineMagnetsView();
        final int size = activeMagnets.size();

        final MagnetConfig config = getConfig();

        final int subdivide = config.getSubdivide();
        final double sqRadius = config.getRadius();

        // Iterate over a subdivision of the active magnets
        for (int index = divIndex; index < size; index += subdivide) {
            final UUID uuid = activeMagnets.get(index);
            final Player player = Bukkit.getPlayer(uuid);

            // If the given magnet has logged out, remove the player after the loop
            if (player == null) toRemove.add(uuid);
            else {
                final Location playerLocation = player.getLocation();

                // Get all items near the player that can don't have a pickup delay and teleport them to the player
                player.getWorld()
                        .getNearbyEntities(playerLocation, sqRadius, sqRadius, sqRadius)
                        .stream()
                        .filter(it -> it instanceof Item)
                        .filter(it -> ((Item) it).getPickupDelay() <= 0)
                        .forEach(it -> it.teleport(playerLocation));
            }
        }

        // Remove logged-out players
        removeMagnets(toRemove);

        // Update subdivision to check next iteration
        divIndex = (divIndex + 1) % subdivide;
    }

    /**
     * Event handler for when a potential magnet player logs out
     * @param magnet Player to check magnet state for
     */
    public void onMagnetLogout(final UUID magnet) {
        if (magnetData.removeLogoutOnline(magnet))
            updateMagnetismTask(true);
    }

    /**
     * Event handler for when a potential magnet player logs in
     * @param magnet Player to check magnet state for
     */
    public void onMagnetLogin(final UUID magnet) {
        if (magnetData.addLoginOnline(magnet))
            updateMagnetismTask(false);
    }

    /**
     * Event handler for saving magnet data to persistent data store
     */
    public void onDisable() {
        data.storeData("magnets", magnetData);
    }
}
