package dev.w1zzrd.invtweaks.command;

import dev.w1zzrd.spigot.wizcompat.command.CommandUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static dev.w1zzrd.invtweaks.listener.TabCompletionListener.getMaterialMatching;
import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue;
import static dev.w1zzrd.spigot.wizcompat.packet.EntityCreator.*;

public final class FindCommandExecutor implements CommandExecutor {
    private static final int SEARCH_RADIUS = 3;
    private static final long DESPAWN_WAIT = 20 * 10;

    private final Plugin plugin;

    public FindCommandExecutor(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Material targetMaterial;

        if (assertTrue(sender instanceof Player, "Command can only be run by players", sender) ||
                assertTrue(args.length == 1, "Exactly one argument is expected", sender) ||
                assertTrue((targetMaterial = getMaterialMatching(args[0])) != null, String.format("Unknown item/block: %s", args[0]), sender))
            return true;

        final Player player = (Player) sender;

        final List<BlockState> matches = searchChunks(
                player.getLocation().getChunk(),
                SEARCH_RADIUS,
                Material.CHEST, Material.TRAPPED_CHEST, Material.SHULKER_BOX
        );

        final List<Integer> entities = matches.stream()
                .filter(it -> Arrays.stream(((Container)it).getSnapshotInventory().getContents()).filter(Objects::nonNull).map(ItemStack::getType).anyMatch(targetMaterial::equals))
                .map(state -> spawnMarker(player, state.getLocation().add(0.5, 0.2, 0.5)))
                .collect(Collectors.toList());

        if (assertTrue(entities.size() > 0, "No containers neardby contain that item/block", sender))
            return true;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            entities.forEach(it -> sendEntityDespawnPacket(player, it));
        }, DESPAWN_WAIT);

        return true;
    }

    private static int spawnMarker(final Player target, final Location location) {
        final Object entity = createFakeSlime(target);
        setSlimeSize(entity, 1);
        setEntityCollision(entity, false);
        setEntityInvisible(entity, true);
        setEntityInvulnerable(entity, true);
        setEntityGlowing(entity, true);
        setEntityLocation(entity, location.getX(), location.getY(), location.getZ(), 0f, 0f);

        sendEntitySpawnPacket(target, entity);
        sendEntityMetadataPacket(target, entity);

        return getEntityID(entity);
    }

    private static List<BlockState> searchChunks(final Chunk middle, final int radius, final Material... targets) {
        final int xMin = middle.getX() - radius;
        final int xMax = middle.getX() + radius;
        final int zMin = middle.getZ() - radius;
        final int zMax = middle.getZ() + radius;

        final World sourceWorld = middle.getWorld();

        final List<Material> targetMaterials = Arrays.asList(targets);
        final ArrayList<BlockState> collect = new ArrayList<>();
        for (int x = xMin; x <= xMax; ++x)
            for (int z = zMin; z <= zMax; ++z)
                Arrays.stream(sourceWorld.getChunkAt(x, z).getTileEntities()).filter(it -> targetMaterials.contains(it.getType())).forEach(collect::add);

        return collect;
    }
}
