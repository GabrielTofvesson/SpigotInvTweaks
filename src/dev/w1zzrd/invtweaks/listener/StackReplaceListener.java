package dev.w1zzrd.invtweaks.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.logging.Logger;

import static dev.w1zzrd.invtweaks.InvTweaksPlugin.LOG_PLUGIN_NAME;

/**
 * Handler for events/interactions that should trigger stack replacements in a players inventory
 */
public class StackReplaceListener implements Listener {

    /**
     * Max index for main player inventory
     */
    private static final int MAX_MAIN_INV = 35;


    private final Logger logger = Bukkit.getLogger();

    @EventHandler
    public void onBlockPlacedEvent(final BlockPlaceEvent event) {
        if (findAndMoveSimilarStack(event.getItemInHand(), event.getHand(), event.getPlayer().getInventory(), CompareFunc.defaultFunc()))
            logger.fine(LOG_PLUGIN_NAME + " Moved stack into empty hand for player " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerItemBreakEvent(final PlayerItemBreakEvent event) {
        final PlayerInventory inv = event.getPlayer().getInventory();
        final ItemStack used = event.getBrokenItem();

        final EquipmentSlot slot;
        if (used.equals(inv.getBoots())) slot = EquipmentSlot.FEET;
        else if (used.equals(inv.getLeggings())) slot = EquipmentSlot.LEGS;
        else if (used.equals(inv.getChestplate())) slot = EquipmentSlot.CHEST;
        else if (used.equals(inv.getHelmet())) slot = EquipmentSlot.HEAD;
        else if (used.equals(inv.getItemInOffHand())) slot = EquipmentSlot.OFF_HAND;
        else if (used.equals(inv.getItemInMainHand())) slot = EquipmentSlot.HAND;
        else return; // No clue what broke

        if (findAndMoveSimilarStack(used, slot, inv, CompareFunc.toolFunc()))
            logger.fine(LOG_PLUGIN_NAME + " Moved tool into empty hand for player " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerThrowSnowballEvent(final ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrowableProjectile &&
                event.getEntity().getShooter() instanceof Player) {

            final ThrowableProjectile projectile = (ThrowableProjectile) event.getEntity();
            final Player thrower = (Player) event.getEntity().getShooter();

            assert thrower != null;
            final PlayerInventory inventory = thrower.getInventory();
            final ItemStack stack = projectile.getItem();

            final EquipmentSlot slot;
            if (inventory.getItemInMainHand().getType() == stack.getType())
                slot = EquipmentSlot.HAND;
            else if (inventory.getItemInOffHand().getType() == stack.getType())
                slot = EquipmentSlot.OFF_HAND;
            else return;

            if (inventory.getItem(slot).getAmount() == 1 &&
                    findAndMoveSimilarStack(stack, slot, inventory, CompareFunc.defaultFunc()))
                logger.fine(LOG_PLUGIN_NAME + " Moved snowball into empty hand for player " + thrower.getName());
        }
    }

    /**
     * Attempt to find and move a similar stack in the inventory to the one given, using the supplied comparison
     * function
     * @param usedItemStack Stack to find a replacement for
     * @param target Inventory slot to move stack to
     * @param inventory Inventory to search for similar items in
     * @param compareFunc Function to use when determining similarity between stacks
     * @return True if a similar stack was found and moved, else false
     */
    private boolean findAndMoveSimilarStack(
            final ItemStack usedItemStack,
            final EquipmentSlot target,
            final PlayerInventory inventory,
            final CompareFunc compareFunc) {
        if (usedItemStack.getAmount() == 1) {
            for (int i = Math.min(inventory.getSize() - 1, MAX_MAIN_INV); i >= 0 ; --i) {
                final ItemStack checkStack = inventory.getItem(i);
                if (i != inventory.getHeldItemSlot() && compareFunc.isSimilar(usedItemStack, checkStack)) {
                    // To prevent race-condition dupes, remove item before putting it in players hand
                    inventory.setItem(i, new ItemStack(Material.AIR, 0));
                    inventory.setItem(target, checkStack);

                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Functional interface for determining when two item stacks are equivalent
     *
     * Note: The comparison should represent an equivalence relation (especially, it should be symmetric)
     */
    private interface CompareFunc {
        boolean isSimilar(final ItemStack i1, final ItemStack i2);

        static CompareFunc defaultFunc() { return ItemStack::isSimilar; }
        static CompareFunc toolFunc() {
            return (i1, i2) -> {
                final boolean isNull1 = i1 == null;
                final boolean isNull2 = i2 == null;

                if (isNull1 || isNull2)
                    return isNull1 == isNull2; // This should really never return true

                final Material m1 = i1.getType();
                final Material m2 = i2.getType();

                // If the material name has an underscore, it probably has material type alternatives
                if (m1.name().contains("_"))
                    return m2.name().endsWith(m1.name().substring(i1.getType().name().lastIndexOf('_')));
                else
                    return m2.equals(m1);
            };
        }
    }
}
