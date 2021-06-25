package dev.w1zzrd.invtweaks.command;

import dev.w1zzrd.invtweaks.InvTweaksPlugin;
import dev.w1zzrd.invtweaks.serialization.SearchConfig;
import dev.w1zzrd.spigot.wizcompat.command.ConfigurableCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static dev.w1zzrd.invtweaks.listener.TabCompletionListener.getMaterialMatching;
import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue;

/**
 * Handler for executions of /search command
 */
public class SearchCommandExecutor extends ConfigurableCommandExecutor<SearchConfig> {

    private static final Logger logger = Bukkit.getLogger();

    // TODO: Move to config (magic values)
    private static final String
        ERR_NOT_PLAYER = "Command must be run by an in-game player",
        ERR_NO_ARG = "Command expects an item or block name as an argument",
        ERR_UNKNOWN = "Unknown item/block name \"%s\"",
        ERR_NO_INVENTORIES = "No inventories could be found";


    public SearchCommandExecutor(final Plugin plugin, final String path) {
        super(plugin, path);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Material targetMaterial;
        if (assertTrue(sender instanceof Player, ERR_NOT_PLAYER, sender) ||
                assertTrue(args.length == 1, ERR_NO_ARG, sender) ||
                assertTrue((targetMaterial = getMaterialMatching(args[0])) != null, String.format(ERR_UNKNOWN, args[0]), sender)
        ) return true;

        assert targetMaterial != null;
        assert sender instanceof Player;
        final Player player = (Player) sender;

        final SearchConfig config = getConfig();

        final List<BlockState> matches = searchBlocks(
                player.getLocation(),
                player.getWorld(),
                config.getSearchRadiusX(), config.getSearchRadiusY(), config.getSearchRadiusZ(),
                Material.CHEST, Material.SHULKER_BOX
        );

        // Ensure we found inventory-holding blocks
        if (assertTrue(matches.size() != 0, ERR_NO_INVENTORIES, sender))
            return true;

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
                    logger.info(InvTweaksPlugin.LOG_PLUGIN_NAME + " Found non-matching block");
                    continue;
                }

                assert holder != null;

                if (holder instanceof DoubleChest) {
                    final InventoryHolder left = Objects.requireNonNull(((DoubleChest) holder).getLeftSide());
                    final InventoryHolder right = Objects.requireNonNull(((DoubleChest) holder).getRightSide());

                    if (left.getInventory().contains(targetMaterial) || right.getInventory().contains(targetMaterial)) {
                        result = holder;
                        break FIND_RESULT;
                    }
                } else if (holder.getInventory().contains(targetMaterial)) {
                    result = holder;
                    break FIND_RESULT;
                }
            }
            assertTrue(false, "Could not find inventory with target item/block", sender);
            return true;
        }

        if (result instanceof DoubleChest) {
            final DoubleChest dChest = (DoubleChest) result;

            // Black magic to make chest lid animation behave for double chests
            try {
                final Field tileField = dChest.getInventory().getClass().getDeclaredField("tile");
                tileField.setAccessible(true);

                final Object tile = tileField.get(dChest.getInventory());

                final Field tecField = tile.getClass().getDeclaredField("tileentitychest");
                tecField.setAccessible(true);

                final Field entityField = player.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("entity");
                entityField.setAccessible(true);

                final Object entity = entityField.get(player);

                final Method openContainerMethod = entity.getClass().getDeclaredMethod("openContainer", tile.getClass().getInterfaces()[0]);
                openContainerMethod.setAccessible(true);

                openContainerMethod.invoke(entity, tile);

                final Field activeContainerField = entity.getClass().getSuperclass().getDeclaredField("activeContainer");
                activeContainerField.setAccessible(true);

                final Object activeContainer = activeContainerField.get(entity);

                final Field checkReachableField = activeContainer.getClass().getSuperclass().getDeclaredField("checkReachable");
                checkReachableField.setAccessible(true);

                // Disable reach checks for container while it is open
                checkReachableField.set(activeContainer, false);

                return true;
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                logger.fine(InvTweaksPlugin.LOG_PLUGIN_NAME + " Could not use internal openContainer method; chest lids may stay open");
            }
        }

        // Default behaviour for non-double-chest inventories
        // (plus fallback for double-chests on "unsupported" versions)
        player.openInventory(result.getInventory());

        return true;
    }

    private List<BlockState> searchBlocks(
            final Location centre,
            final World world,
            final int rx,
            final int ry,
            final int rz,
            final Material... targets
    ) {
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
}
