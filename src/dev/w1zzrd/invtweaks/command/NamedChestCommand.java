package dev.w1zzrd.invtweaks.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.*;
import static dev.w1zzrd.spigot.wizcompat.packet.EntityCreator.*;

public class NamedChestCommand implements CommandExecutor {

    private final Plugin plugin;

    public NamedChestCommand(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (assertTrue(sender instanceof Player && ((Player) sender).isOnline(), "Command can only be run by a player!", sender))
            return true;

        if (assertTrue(args.length != 0, "Expected a name for the chest", sender))
            return true;

        if (assertTrue(args.length == 1, "Too many arguments for command", sender))
            return true;

        final Player player = (Player) sender;

        final Block block = player.getTargetBlockExact(10);

        if (assertTrue(block != null && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST), "You must be targeting a chest", sender))
            return true;

        final Location loc = getCenterChestLocation(block);

        final Object entity = createFakeSlime(player);
        setSlimeSize(entity, 1);

        setEntityCollision(entity, false);
        setEntityCustomName(entity, args[0]);
        setEntityInvulnerable(entity, true);
        setEntityLocation(entity, loc.getX(), loc.getY(), loc.getZ(), 0f, 0f);
        setEntityCustomNameVisible(entity, true);

        sendEntitySpawnPacket(player, entity);
        sendEntityMetadataPacket(player, entity);

        final int entityID = getEntityID(entity);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendEntityDespawnPacket(player, entityID);
        }, 60);

        return true;
    }

    private static Location getCenterChestLocation(final Block chestBlock) {
        final InventoryHolder holder = Objects.requireNonNull(((Chest) chestBlock.getState()).getBlockInventory().getHolder()).getInventory().getHolder();

        if (holder instanceof final DoubleChest dChest) {
            final Location left = getBlockCenter(Objects.requireNonNull((Chest)dChest.getLeftSide()).getBlock());
            final Location right = getBlockCenter(Objects.requireNonNull((Chest)dChest.getRightSide()).getBlock());

            return new Location(left.getWorld(), (left.getX() + right.getX()) / 2.0, left.getY() + 0.2, (left.getZ() + right.getZ()) / 2.0);
        } else {
            return getBlockCenter(chestBlock).add(0.0, 0.2, 0.0);
        }
    }

    private static Location getBlockCenter(final Block block) {
        return block.getLocation().add(0.5, 0, 0.5);
    }
}
