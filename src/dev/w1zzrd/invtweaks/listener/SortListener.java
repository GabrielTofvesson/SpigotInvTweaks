package dev.w1zzrd.invtweaks.listener;

import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;
import java.util.logging.Logger;

import static dev.w1zzrd.invtweaks.InvTweaksPlugin.LOG_PLUGIN_NAME;

public class SortListener implements Listener {

    private static final Logger logger = Bukkit.getLogger();

    @EventHandler
    public void onPlayerInteractEvent(final PlayerInteractEvent event) {
        final PlayerInventory playerInventory = event.getPlayer().getInventory();


        if (event.hasBlock() &&
                (Objects.requireNonNull(event.getClickedBlock()).getState() instanceof Chest ||
                        event.getClickedBlock().getState() instanceof ShulkerBox) &&
                event.getPlayer().isSneaking() &&
                playerInventory.getItemInMainHand().getType().name().endsWith("_SWORD")
        ) {
            logger.fine(LOG_PLUGIN_NAME + " Triggering /sort for player " + event.getPlayer().getName());
            event.getPlayer().performCommand("sort");
        }
    }
}
