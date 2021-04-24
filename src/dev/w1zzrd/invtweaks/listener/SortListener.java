package dev.w1zzrd.invtweaks.listener;

import dev.w1zzrd.logging.LoggerFactory;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

import java.util.logging.Logger;

public class SortListener implements Listener {

    private static final Logger logger = LoggerFactory.getLogger(SortListener.class);

    @EventHandler
    public void onPlayerInteractEvent(final PlayerInteractEvent event) {
        final PlayerInventory playerInventory = event.getPlayer().getInventory();


        if (event.hasBlock() &&
                (event.getClickedBlock().getState() instanceof Chest || event.getClickedBlock().getState() instanceof ShulkerBox) &&
                event.getPlayer().isSneaking() &&
                playerInventory.getItemInMainHand().getType().name().endsWith("_SWORD")
        )
            event.getPlayer().performCommand("sort");
    }
}
