package dev.w1zzrd.invtweaks.listener;

import dev.w1zzrd.invtweaks.feature.NamedChestManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import static dev.w1zzrd.invtweaks.listener.PlayerMoveRenderListener.RENDER_RADIUS;
import static dev.w1zzrd.spigot.wizcompat.block.Chests.getLeftChest;

public final class ChestBreakListener implements Listener {

    private final NamedChestManager manager;

    public ChestBreakListener(final NamedChestManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onChestBreak(final BlockBreakEvent event) {
        if (!event.isCancelled())
            processBlockEvent(event);
    }

    @EventHandler
    public void onChestPlace(final BlockPlaceEvent event) {
        if (!event.isCancelled())
            processBlockEvent(event);
    }

    private void processBlockEvent(final BlockEvent event) {
        if (event.getBlock().getType() == Material.CHEST || event.getBlock().getType() == Material.TRAPPED_CHEST) {
            final Chest chest = (Chest) event.getBlock().getState();
            manager.removeTag(chest);
            manager.renderTags(chest.getChunk(), RENDER_RADIUS);
        }
    }
}
