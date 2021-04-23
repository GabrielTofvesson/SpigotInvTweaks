package dev.w1zzrd.invtweaks.command;

import dev.w1zzrd.logging.LoggerFactory;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

import static org.bukkit.Material.*;

public class SortCommandExecutor implements CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SortCommandExecutor.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Since we rely on targeting an inventory either by what we're looking at or by whom it was called,
        // there is an implicit dependency on that the command is called by (at the very least) an entity
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command must be run by a player");
            return false;
        }

        final Player player = (Player) sender;

        // The block the player is currently looking at (if applicable)
        final BlockState target = player.getTargetBlock(null, 6).getState();

        // Sort appropriate inventory holder
        if (target instanceof Chest)
            sortChest((Chest) target);
        else if (target instanceof ShulkerBox)
            sortShulkerBox((ShulkerBox) target);
        else
            sortPlayer(player);

        return true;
    }

    /**
     * Sort the inventory of a Shulker Box
     * @param shulkerBox Shulker Box to sort
     */
    private static void sortShulkerBox(final ShulkerBox shulkerBox) {
        sortInventory(shulkerBox.getInventory());
    }

    /**
     * Sort the inventory of a chest. If the given chest is part of a double-chest, the inventory of both chests is
     * observed as one large inventory when sorting.
     * @param chest Chest (or stand-in for double-chest) to sort
     */
    private static void sortChest(final Chest chest) {
        final InventoryHolder chestInventoryHolder = chest.getBlockInventory().getHolder();

        assert chestInventoryHolder != null;

        // Get the inventory holder for the chest (in case we're dealing with a double-chest)
        final Inventory chestInventory = chestInventoryHolder.getInventory();
        final ItemStack[] toSort;

        // If we're sorting a double-chest, we need to combine the two inventories for sorting
        if (chestInventoryHolder instanceof DoubleChest) {
            final ItemStack[] c1 = ((DoubleChestInventory) chestInventory).getLeftSide().getContents();
            final ItemStack[] c2 = ((DoubleChestInventory) chestInventory).getRightSide().getContents();

            toSort = new ItemStack[c1.length + c2.length];
            System.arraycopy(c1, 0, toSort, 0, c1.length);
            System.arraycopy(c2, 0, toSort, c1.length, c2.length);
        } else {
            toSort = chestInventory.getContents();
        }

        // Merge all stacks and then sort them
        mergeStacks(toSort);
        sortStacks(toSort);

        // If we're sorting a double-chest, we need to separate the combined inventories again
        if (chestInventoryHolder instanceof DoubleChest) {
            final Inventory invLeft = ((DoubleChestInventory) chestInventory).getLeftSide();
            final Inventory invRight = ((DoubleChestInventory) chestInventory).getRightSide();

            final ItemStack[] stacksLeft = new ItemStack[invLeft.getSize()];
            final ItemStack[] stacksRight = new ItemStack[invRight.getSize()];

            System.arraycopy(toSort, 0, stacksLeft, 0, stacksLeft.length);
            System.arraycopy(toSort, stacksLeft.length, stacksRight, 0, stacksRight.length);

            invLeft.setContents(stacksLeft);
            invRight.setContents(stacksRight);
        } else {
            chestInventory.setContents(toSort);
        }
    }

    /**
     * Sort the main inventory of a player. Note that the hotbar is not included in the sorting, nor is the off hand or
     * any of the armour slots
     * @param player Player whose inventory is to be sorted
     */
    private static void sortPlayer(final Player player) {
        final ItemStack[] stacks = player.getInventory().getContents();

        final ItemStack[] sortable = Arrays.copyOfRange(stacks, 9, 36);
        mergeStacks(sortable);
        sortStacks(sortable);
        System.arraycopy(sortable, 0, stacks, 9, sortable.length);

        player.getInventory().setContents(stacks);
    }

    /**
     * Sort all slots in a given inventory
     * @param inventory Inventory to sort
     */
    private static void sortInventory(final Inventory inventory) {
        final ItemStack[] stacks = inventory.getContents();
        mergeStacks(stacks);
        sortStacks(stacks);
        inventory.setContents(stacks);
    }

    /**
     * Sort ItemStacks
     * @param stacks ItemStacks to sort
     */
    private static void sortStacks(final ItemStack[] stacks) {
        Arrays.sort(stacks, (o1, o2) -> {
            if (o1 == null || o2 == null)
                return o1 == o2 ? 0 : o1 == null ? 1 : -1;

            final Material m1 = o1.getType();
            final Material m2 = o2.getType();

            // Technically not a stable sort, but w/e
            if (m1 == AIR || m1 == CAVE_AIR || m1 == VOID_AIR)
                return m2 == AIR || m2 == CAVE_AIR || m2 == VOID_AIR ? 0 : -1;

            // Blocks appear earlier than items
            if (m1.isBlock() != m2.isBlock())
                return m1.isBlock() ? 1 : -1;

            // Stacks of similar type are organized according to stack size
            if (o1.isSimilar(o2))
                return Integer.compare(o1.getAmount(), o2.getAmount());

            // Differing stacks are sorted according to enum ordinal (arbitrary but I'm not manually designing an order)
            return Integer.compare(m1.ordinal(), m2.ordinal());
        });
    }

    /**
     * Merge disparate ItemStacks of the same type
     * @param stacks ItemStacks to attempt to merge
     */
    private static void mergeStacks(final ItemStack[] stacks) {
        final HashMap<ItemStack, Integer> count = new HashMap<>();
        for (final ItemStack stack : stacks) {
            if (stack == null)
                continue;

            final Optional<ItemStack> tracked = count.keySet().stream().filter(stack::isSimilar).findFirst();

            if (tracked.isPresent())
                count.put(tracked.get(), count.get(tracked.get()) + stack.getAmount());
            else
                count.put(stack, stack.getAmount());
        }

        for (int i = stacks.length - 1; i >= 0; --i) {
            final ItemStack current = stacks[i];
            if (current == null)
                continue;

            final Optional<ItemStack> tracked = count.keySet().stream().filter(current::isSimilar).findFirst();

            // Should always be true but I don't know Spigot, so I'm gonna play it safe
            if (tracked.isPresent()) {
                final ItemStack key = tracked.get();
                final int amount = count.get(tracked.get());

                if (amount == 0) {
                    stacks[i] = new ItemStack(Material.AIR);
                } else {
                    final int currentAmount = current.getAmount();

                    if (currentAmount < current.getMaxStackSize()) {
                        final int newAmount = Math.min(amount, current.getMaxStackSize());
                        current.setAmount(newAmount);

                        // Update remaining count of given material
                        count.put(key, amount - newAmount);
                    } else {
                        count.put(key, amount - currentAmount);
                    }
                }
            } else {
                logger.warning("Found untracked ItemStack while merging stacks");
            }
        }
    }
}
