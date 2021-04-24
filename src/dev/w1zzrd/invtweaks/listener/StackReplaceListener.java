package dev.w1zzrd.invtweaks.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.logging.Logger;

import static dev.w1zzrd.invtweaks.InvTweaksPlugin.LOG_PLUGIN_NAME;

public class StackReplaceListener implements Listener {

    private static final int MAX_MAIN_INV = 35;


    private final Logger logger = Bukkit.getLogger();

    @EventHandler
    public void onBlockPlacedEvent(final BlockPlaceEvent event) {
        final ItemStack usedItemStack = event.getItemInHand();
        if (usedItemStack.getAmount() == 1) {
            final PlayerInventory inv = event.getPlayer().getInventory();
            for (int i = Math.min(inv.getSize() - 1, MAX_MAIN_INV); i >= 0 ; --i) {
                final ItemStack checkStack = inv.getItem(i);
                if (i != inv.getHeldItemSlot() && usedItemStack.isSimilar(checkStack)) {
                    // To prevent race-condition dupes, remove item before putting it in players hand
                    inv.setItem(i, new ItemStack(Material.AIR, 0));
                    inv.setItem(event.getHand(), checkStack);

                    logger.fine(LOG_PLUGIN_NAME + " Moved stack into empty hand for player " + event.getPlayer().getName());

                    break;
                }
            }
        }
    }
}
