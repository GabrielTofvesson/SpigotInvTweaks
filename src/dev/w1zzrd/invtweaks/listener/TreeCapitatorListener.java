package dev.w1zzrd.invtweaks.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.*;

import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.errorMessage;
import static org.bukkit.Material.*;

public class TreeCapitatorListener implements Listener {

    private static final int MAX_SEARCH_BLOCKS = 69420;
    private static final List<Material> targetMaterials = Arrays.asList(
            ACACIA_LOG,
            OAK_LOG,
            BIRCH_LOG,
            JUNGLE_LOG,
            DARK_OAK_LOG,
            SPRUCE_LOG,
            CRIMSON_STEM,
            WARPED_STEM,
            ACACIA_WOOD,
            OAK_WOOD,
            BIRCH_WOOD,
            JUNGLE_WOOD,
            DARK_OAK_WOOD,
            SPRUCE_WOOD,
            CRIMSON_STEM,
            WARPED_STEM,
            CRIMSON_HYPHAE,
            WARPED_HYPHAE,
            STRIPPED_ACACIA_LOG,
            STRIPPED_OAK_LOG,
            STRIPPED_BIRCH_LOG,
            STRIPPED_JUNGLE_LOG,
            STRIPPED_DARK_OAK_LOG,
            STRIPPED_SPRUCE_LOG,
            STRIPPED_CRIMSON_STEM,
            STRIPPED_WARPED_STEM,
            STRIPPED_CRIMSON_HYPHAE,
            STRIPPED_WARPED_HYPHAE,

            ACACIA_LEAVES,
            OAK_LEAVES,
            BIRCH_LEAVES,
            JUNGLE_LEAVES,
            DARK_OAK_LEAVES,
            SPRUCE_LEAVES,
            NETHER_WART_BLOCK,
            WARPED_WART_BLOCK,
            SHROOMLIGHT
    );

    private static final List<Material> leaves = Arrays.asList(
            ACACIA_LEAVES,
            OAK_LEAVES,
            BIRCH_LEAVES,
            JUNGLE_LEAVES,
            DARK_OAK_LEAVES,
            SPRUCE_LEAVES,
            NETHER_WART_BLOCK,
            WARPED_WART_BLOCK,
            SHROOMLIGHT
    );

    private final Enchantment capitatorEnchantment;
    private final double hungerPerBlock;
    private final int minHunger;

    public TreeCapitatorListener(final Enchantment capitatorEnchantment, final double hungerPerBlock, final int minHunger) {
        this.capitatorEnchantment = capitatorEnchantment;
        this.hungerPerBlock = hungerPerBlock;
        this.minHunger = minHunger;
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final ItemStack handTool = event.getPlayer().getInventory().getItemInMainHand();
        if (event.isCancelled() || !handTool.containsEnchantment(capitatorEnchantment))
            return;

        // Check if capitator functionality is prevented by hunger
        if (event.getPlayer().getFoodLevel() < minHunger) {
            event.getPlayer().spigot().sendMessage(errorMessage("You are too tired to fell the tree"));
            return;
        }

        if (handTool.getItemMeta() instanceof final Damageable tool) {
            if (!leaves.contains(event.getBlock().getType()) && targetMaterials.contains(event.getBlock().getType())) {
                int logBreakCount = 0;

                for (final Block found : findAdjacent(event.getBlock(), getMaxUses(handTool, tool)))
                    if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
                        found.setType(AIR);
                    else {
                        if (!leaves.contains(found.getType()))
                            ++logBreakCount;

                        found.breakNaturally(handTool);
                    }

                if (event.getPlayer().getGameMode() != GameMode.CREATIVE && !applyDamage(handTool, logBreakCount)) {
                    event.getPlayer().getInventory().setItemInMainHand(new ItemStack(AIR));
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }

                if (event.getPlayer().getGameMode() == GameMode.ADVENTURE || event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                    final int hunger = (int) Math.round(hungerPerBlock * logBreakCount);
                    event.getPlayer().setFoodLevel(Math.max(0, event.getPlayer().getFoodLevel() - hunger));
                }

                event.setCancelled(true);
            }
        }
    }

    private static int getMaxUses(final ItemStack stack, final Damageable tool) {
        return ((stack.getEnchantmentLevel(Enchantment.DURABILITY) + 1) * (stack.getType().getMaxDurability()) - tool.getDamage()) + 1;
    }

    private static boolean applyDamage(final ItemStack stack, final int brokenBlocks) {
        final ItemMeta meta = stack.getItemMeta();

        if (meta instanceof final Damageable toolMeta) {

            final int unbreakingDivider = stack.getEnchantmentLevel(Enchantment.DURABILITY) + 1;
            final int round = brokenBlocks % unbreakingDivider;
            final int dmg = (brokenBlocks / unbreakingDivider) + (round != 0 ? 1 : 0);

            if (dmg > (stack.getType().getMaxDurability() - toolMeta.getDamage()))
                return false;

            toolMeta.setDamage(toolMeta.getDamage() + dmg);

            stack.setItemMeta(meta);
        }
        return true;
    }

    private static List<Block> findAdjacent(final Block start, final int softMax) {
        List<Block> frontier = new ArrayList<>();
        final List<Block> matches = new ArrayList<>();

        frontier.add(start);
        matches.add(start);

        int total = 1;
        int softMaxCount = 1;

        // Keep finding blocks until we have no new matches
        while (frontier.size() > 0 && total < MAX_SEARCH_BLOCKS && softMaxCount < softMax) {
            final long result = addAdjacents(frontier, matches, total, softMaxCount, softMax);
            total = (int) (result >>> 32);
            softMaxCount = (int) (result & 0xFFFFFFFFL);
        }

        return matches;
    }

    private static long addAdjacents(final List<Block> frontier, final List<Block> collect, int total, int softMax, final int softMaxCap) {
        final List<Block> newFrontier = new ArrayList<>();

        OUTER:
        for (final Block check : frontier)
            for (int x = -1; x <= 1; ++x)
                for (int y = -1; y <= 1; ++y)
                    for (int z = -1; z <= 1; ++z)
                        if ((x | y | z) != 0) {
                            final Block offset = offset(collect, check, x, y, z);

                            if (offset != null && !binaryContains(newFrontier, offset)) {
                                binaryInsert(collect, offset);
                                binaryInsert(newFrontier, offset);

                                if (!leaves.contains(offset.getType())) {
                                    ++softMax;

                                    if (softMax >= softMaxCap)
                                        break OUTER;
                                }
                            }

                            // Short-circuit for max search
                            ++total;
                            if (total == MAX_SEARCH_BLOCKS)
                                break OUTER;
                        }

        frontier.clear();
        frontier.addAll(newFrontier);

        return (((long)total) << 32) | (((long) softMax) & 0xFFFFFFFFL);
    }

    private static Block offset(final List<Block> checked, final Block source, int x, int y, int z) {
       final Block offset = source.getWorld().getBlockAt(source.getLocation().add(x, y, z));

        if (targetMaterials.contains(offset.getType()) && (!leaves.contains(source.getType()) || !leaves.contains(offset.getType())) && !binaryContains(checked, offset))
            return offset;

        return null;
    }

    private static boolean binaryContains(final List<Block> collection, final Block find) {
        return binarySearchBlock(collection, find) >= 0;
    }

    private static void binaryInsert(final List<Block> collection, final Block insert) {
        final int index = binarySearchBlock(collection, insert);

        if (index >= 0)
            return;

        collection.add(-(index + 1), insert);
    }

    private static void binaryRemove(final List<Block> collection, final Block remove) {
        final int index = binarySearchBlock(collection, remove);

        if (index < 0)
            return;

        collection.remove(index);
    }

    private static int binarySearchBlock(final List<Block> collection, final Block find) {
        return Collections.binarySearch(collection, find, TreeCapitatorListener::blockCompare);
    }

    private static int blockCompare(final Block b1, final Block b2) {
        return coordinateCompare(b1.getX(), b1.getY(), b1.getZ(), b2.getX(), b2.getY(), b2.getZ());
    }
    private static int coordinateCompare(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
        final int x = Integer.compare(x1, x2);
        if (x != 0)
            return x;

        final int y = Integer.compare(y1, y2);
        if (y != 0)
            return y;

        return Integer.compare(z1, z2);
    }
}
