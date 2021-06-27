package dev.w1zzrd.invtweaks.command;

import dev.w1zzrd.invtweaks.feature.NamedChestManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static dev.w1zzrd.invtweaks.listener.PlayerMoveRenderListener.RENDER_RADIUS;
import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue;

public final class NamedChestCommand implements CommandExecutor {

    private final NamedChestManager manager;

    public NamedChestCommand(final NamedChestManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (assertTrue(sender instanceof Player && ((Player) sender).isOnline(), "Command can only be run by a player!", sender))
            return true;

        if (assertTrue(args.length <= 1, "Too many arguments for command", sender))
            return true;

        final Player player = (Player) sender;

        final Block block = player.getTargetBlockExact(10);

        if (assertTrue(block != null && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST), "You must be targeting a chest", sender))
            return true;

        final Chest chest = (Chest) block.getState();

        if (args.length == 0) {
            manager.removeTag(chest);
        } else {
            if (manager.hasNamedChest(chest))
                manager.removeTag(chest);

            manager.addTag(chest, args[0]);
        }

        manager.renderTags(chest.getChunk(), RENDER_RADIUS);
        return true;
    }
}
