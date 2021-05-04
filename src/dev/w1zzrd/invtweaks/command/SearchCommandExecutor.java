package dev.w1zzrd.invtweaks.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static dev.w1zzrd.invtweaks.command.CommandUtils.assertTrue;

public class SearchCommandExecutor implements CommandExecutor {

    private static final Logger logger = Bukkit.getLogger();

    // TODO: Move to config (magic values)
    private static final String
        ERR_NOT_PLAYER = "Command must be run by an in-game player",
        ERR_NO_ARG = "Command expects an item or block name as an argument",
        ERR_UNKNOWN = "Unknown item/block name \"%s\"",
        ERR_NO_INVENTORIES = "No inventories could be found";

    private static final int RADIUS_X = 8, RADIUS_Y = 8, RADIUS_Z = 8;


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Material target;
        if (assertTrue(sender instanceof Player, ERR_NOT_PLAYER, sender) ||
                assertTrue(args.length == 1, ERR_NO_ARG, sender) ||
                assertTrue((target = Material.getMaterial(args[0])) != null, String.format(ERR_UNKNOWN, args[0]), sender)
        ) return false;

        assert target != null;
        assert sender instanceof Player;
        final Player player = (Player) sender;

        final List<BlockState> matches = searchBlocks(
                player.getLocation(),
                player.getWorld(),
                RADIUS_X, RADIUS_Y, RADIUS_Z,
                Material.CHEST, Material.SHULKER_BOX
        );

        // Ensure we found inventory-holding blocks
        if (assertTrue(matches.size() != 0, ERR_NO_INVENTORIES, sender))
            return false;

        final InventoryHolder result;

        FIND_RESULT:
        {
            for (final BlockState check : matches) {
                final InventoryHolder holder;
                if (check instanceof Chest)
                    holder = Objects.requireNonNull(((Chest) check).getBlockInventory().getHolder()).getInventory().getHolder();
                else if (check instanceof ShulkerBox)
                    holder = ((ShulkerBox) check).getInventory().getHolder();
                else {
                    logger.info("Found non-matching block");
                    continue;
                }

                assert holder != null;

                if (holder instanceof DoubleChest) {
                    final InventoryHolder left = Objects.requireNonNull(((DoubleChest) holder).getLeftSide());
                    final InventoryHolder right = Objects.requireNonNull(((DoubleChest) holder).getRightSide());

                    if (left.getInventory().contains(target) || right.getInventory().contains(target)) {
                        result = holder;
                        break FIND_RESULT;
                    }
                } else if (holder.getInventory().contains(target)) {
                    result = holder;
                    break FIND_RESULT;
                }
            }
            assertTrue(false, "Could not find inventory with target item/block", sender);
            return false;
        }

        if (result instanceof DoubleChest) {
            final DoubleChest dChest = (DoubleChest) result;

            //final InventoryView view = player.openInventory(dChest.getInventory());
            System.out.println("Opened inventory");
            player.openInventory(new SearchInventoryView(dChest, player, ((Chest)dChest.getLeftSide()).getCustomName() == null ? "Large Chest" : Objects.requireNonNull(((Container) result).getCustomName())));
        } else {
            player.openInventory(result.getInventory());
        }

        return true;
    }

    private List<BlockState> searchBlocks(final Location centre, final World world, final int rx, final int ry, final int rz, final Material... targets) {
        if (targets.length == 0)
            return Collections.emptyList();

        final int x = centre.getBlockX(), y = centre.getBlockY(), z = centre.getBlockZ();
        final ArrayList<BlockState> matches = new ArrayList<>();
        for (int dx = -rx; dx < rx; ++dx)
            for (int dy = -ry; dy < ry; ++dy)
                CHECK_Z:
                for (int dz = -rz; dz < rz; ++dz) {
                    final Block check = world.getBlockAt(x + dx, y + dy, z + dz);
                    final Material checkMaterial = check.getType();
                    for (Material target : targets)
                        if (target == checkMaterial) {
                            matches.add(check.getState());
                            continue CHECK_Z;
                        }
                }

        return matches;
    }

    public static class SearchInventoryView extends InventoryView {

        private final DoubleChest dChest;
        private final Player player;
        private final String title;

        SearchInventoryView(final DoubleChest dChest, final Player player, final String title) {
            this.dChest = dChest;
            this.player = player;
            this.title = title;
        }

        @Override
        public Inventory getTopInventory() {
            return dChest.getInventory();
        }

        @Override
        public Inventory getBottomInventory() {
            return player.getInventory();
        }

        @Override
        public HumanEntity getPlayer() {
            return player;
        }

        @Override
        public InventoryType getType() {
            return InventoryType.CHEST;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }
}
