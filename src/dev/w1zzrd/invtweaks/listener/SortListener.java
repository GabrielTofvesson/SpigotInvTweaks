package dev.w1zzrd.invtweaks.listener;

import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;
import java.util.logging.Logger;

import static dev.w1zzrd.invtweaks.InvTweaksPlugin.LOG_PLUGIN_NAME;

public class SortListener implements Listener {

    private static final Logger logger = Bukkit.getLogger();

    @EventHandler
    public void onPlayerInteractEvent(final PlayerInteractEvent event) {
        final PlayerInventory playerInventory = event.getPlayer().getInventory();


        /* Criteria for a successful /sort trigger (in order of occurrence in code):
         *   1. Player must be attempting to interact with a block
         *   2. Player must be attempting to interact with a Chest or Shulker Box
         *   3. Player must be sneaking
         *   4. Player must be holding a sword in their main hand
         *   5. Event is not triggered for players off-hand
         */
        if (event.hasBlock() &&
                (Objects.requireNonNull(event.getClickedBlock()).getState() instanceof Chest ||
                        event.getClickedBlock().getState() instanceof ShulkerBox) &&
                event.getPlayer().isSneaking() &&
                playerInventory.getItemInMainHand().getType().name().endsWith("_SWORD") &&
                event.getHand() != EquipmentSlot.OFF_HAND
        ) {
            logger.fine(LOG_PLUGIN_NAME + " Triggering /sort for player " + event.getPlayer().getName());
            event.getPlayer().performCommand("sort");
        }
    }
}
