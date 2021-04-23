package dev.w1zzrd.invtweaks.command;

import dev.w1zzrd.logging.LoggerFactory;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

import static org.bukkit.Material.*;

public class SortCommandExecutor implements CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SortCommandExecutor.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Command must be run by a player");
            return false;
        }

        final Player player = (Player) sender;

        final BlockState target = player.getTargetBlock(null, 6).getState();

        if (!(target instanceof Chest)) return false;

        final Chest chest = (Chest) target;

        final Inventory chestInventory = chest.getBlockInventory();
        mergeStacks(chestInventory);
        sortInventory(chestInventory);

        return true;
    }

    private static void sortInventory(final Inventory inventory) {
        inventory.setContents(Arrays.stream(inventory.getContents())
                .sorted(new ItemStackComparator())
                .toArray(ItemStack[]::new)
        );
    }

    private static void mergeStacks(final Inventory inventory) {
        final HashMap<ItemStack, Integer> count = new HashMap<>();
        for (final ItemStack stack : inventory) {
            final Optional<ItemStack> tracked = count.keySet().stream().filter(stack::isSimilar).findFirst();

            if (tracked.isPresent())
                count.put(tracked.get(), count.get(tracked.get()) + stack.getAmount());
            else
                count.put(stack, stack.getAmount());
        }

        for (int i = inventory.getSize() - 1; i >= 0; --i) {
            final ItemStack current = Objects.requireNonNull(inventory.getItem(i));
            final Optional<ItemStack> tracked = count.keySet().stream().filter(current::isSimilar).findFirst();

            // Should always be true but I don't know Spigot, so I'm gonna play it safe
            if (tracked.isPresent()) {
                final ItemStack key = tracked.get();
                final int amount = count.get(tracked.get());

                if (amount == 0) {
                    inventory.setItem(i, new ItemStack(Material.AIR));
                } else {
                    final int currentAmount = current.getAmount();

                    if (current.getAmount() < current.getMaxStackSize()) {
                        final int newAmount = Math.min(currentAmount + amount, current.getMaxStackSize());
                        current.setAmount(newAmount);

                        // Update remaining count of given material
                        count.put(key, amount - (newAmount - currentAmount));
                    }
                }
            } else {
                logger.warning("Found untracked ItemStack while merging stacks");
            }
        }
    }

    private static class ItemStackComparator implements Comparator<ItemStack> {
        @Override
        public int compare(ItemStack o1, ItemStack o2) {
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
        }
    }
}
