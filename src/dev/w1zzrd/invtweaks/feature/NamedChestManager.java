package dev.w1zzrd.invtweaks.feature;

import dev.w1zzrd.invtweaks.serialization.ChestNameConfig;
import dev.w1zzrd.spigot.wizcompat.serialization.PersistentData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

import static dev.w1zzrd.spigot.wizcompat.block.Chests.*;
import static dev.w1zzrd.spigot.wizcompat.packet.EntityCreator.*;

public final class NamedChestManager {
    private static final String PATH_NAMED_CHESTS = "namedChests";

    private final RenderRegistry renders = new RenderRegistry();

    private final ChestNameConfig config;

    public NamedChestManager(final PersistentData data) {
        config = data.loadData(PATH_NAMED_CHESTS, ChestNameConfig::new);
    }

    public boolean hasNamedChest(final World world, final Location loc) {
        return getChestNameAt(world, loc) != null;
    }

    public boolean hasNamedChest(final Location loc) {
        return hasNamedChest(Objects.requireNonNull(loc.getWorld()), loc);
    }

    public boolean hasNamedChest(final Chest chest) {
        final Block left = getLeftChest(chest);

        return hasNamedChest(left.getWorld(), left.getLocation());
    }

    private String getChestName(final Block block) {
        return getChestNameAt(block.getWorld(), getLeftChest((Chest)block.getState()).getLocation());
    }

    private void setChestName(final Block block, final String name) {
        addChestName(block.getWorld(), block.getLocation(), name);
    }

    private void addChestName(final World world, final Location location, final String name) {
        config.getEntry(world.getUID()).add(location, name);
    }

    public String getChestNameAt(final World world, final Location location) {
        return config.getEntry(world.getUID()).getName(location);
    }

    public String getChestNameAt(final Location location) {
        return getChestNameAt(Objects.requireNonNull(location.getWorld()), location);
    }

    public void untrackPlayer(final Player player) {
        renders.removeRender(player.getUniqueId());
    }

    public void renderTags(final Chunk chunk, final int chunkRadius) {
        chunk.getWorld()
                .getPlayers()
                .stream()
                .filter(it -> {
                    final Chunk playerChunk = it.getLocation().getChunk();

                    return Math.abs(playerChunk.getX() - chunk.getX()) <= chunkRadius && Math.abs(playerChunk.getZ() - chunk.getZ()) <= chunkRadius;
                })
                .forEach(player -> renderTags(player, chunkRadius));

        final ChestNameConfig.ChestNameWorldEntry.ChestNameChunkEntry chestChunk = config.getChunkEntry(
                chunk.getWorld().getUID(),
                chunk.getX(),
                chunk.getZ()
        );

        if (chestChunk != null)
            chestChunk.setDirty(false);
    }

    public void renderTags(final Player target, final int chunkRadius) {
        final UUID worldID = target.getWorld().getUID();

        renders.updateRenders(
                target,
                chunkRadius,
                addedChunk -> {
                    final ChestNameConfig.ChestNameWorldEntry.ChestNameChunkEntry chunk = config.getChunkEntry(worldID, addedChunk.getRender().x(), addedChunk.getRender().z());
                    if (chunk == null)
                        return;

                    final int baseX = chunk.getX() << 4;
                    final int baseZ = chunk.getZ() << 4;

                    chunk.streamEntries().forEach(entry -> {
                        final Object entity = entry.getEntity(() -> {
                            final Location loc = getCenterChestLocation(target.getWorld().getBlockAt(baseX + entry.getChunkX(), entry.getY(), baseZ + entry.getChunkZ()));

                           final Object newEntity = createFakeSlime(target);
                           setEntityCollision(newEntity, false);
                           setEntityInvulnerable(newEntity, true);
                           setEntityLocation(newEntity, loc.getX(), loc.getY(), loc.getZ(), 0f, 0f);
                           setEntityCustomName(newEntity, entry.getName());
                           setEntityCustomNameVisible(newEntity, true);

                           return newEntity;
                        });

                        sendEntitySpawnPacket(target, entity);
                        sendEntityMetadataPacket(target, entity);
                    });
                },
                removedChunk -> {
                    final ChestNameConfig.ChestNameWorldEntry.ChestNameChunkEntry chunk = config.getChunkEntry(worldID, removedChunk.getRender().x(), removedChunk.getRender().z());
                    if (chunk == null)
                        return;

                    chunk.streamEntries().forEach(entry -> {
                        final Object entity = entry.getEntity(() -> null);

                        if (entity != null)
                            sendEntityDespawnPacket(target, getEntityID(entity));
                    });
                });
    }

    public void removeTag(final World world, final Location location) {
        final ChestNameConfig.ChestNameWorldEntry.ChestNameChunkEntry.ChestNameConfigEntry entry = config.getEntryAt(world.getUID(), location);

        if (entry != null) {
            final ChunkRenderEntry chunk = renders.getChunk(location.getChunk().getX(), location.getChunk().getZ(), false);

            if (chunk != null) {
                final ChestNameConfig.ChestNameWorldEntry.ChestNameChunkEntry chestChunk = config.getChunkEntry(world.getUID(), chunk.getRender().x(), chunk.getRender().z());

                if (chestChunk != null)
                    chestChunk.removeEntry(entry);

                chunk.streamRenders().forEach(it -> {
                    final Player player = Bukkit.getPlayer(it.getRender());

                    if (player == null)
                        renders.removeRender(it.getRender());
                    else
                        sendEntityDespawnPacket(player, getEntityID(entry.getEntity(() -> null)));
                });

                if (chestChunk != null) {
                    chestChunk.setDirty(false);

                    // Make sure we don't leave blank data in persistent data file
                    final ChestNameConfig.ChestNameWorldEntry worldEntry = config.getEntry(world.getUID());
                    worldEntry.deleteEmptyChunk(chestChunk);
                    config.deleteEmptyWorld(worldEntry);
                }
            }
        }
    }

    public void removeTag(final Location location) {
        removeTag(Objects.requireNonNull(location.getWorld()), location);
    }

    public void addTag(final World world, final Location location, final String name) {
        config.getEntry(world.getUID(), true).add(location, name);
    }

    public void addTag(final Chest chest, final String name) {
        final Block left = getLeftChest(chest);

        addTag(left.getWorld(), left.getLocation(), name);
    }

    public void removeTag(final Chest chest) {
        final Block left = getLeftChest(chest);

        removeTag(left.getWorld(), left.getLocation());
    }

    private static Location getCenterChestLocation(final Block chestBlock) {
        if (isDoubleChest(chestBlock)) {
            final Location left = getBlockCenter(getLeftChest((Chest) chestBlock.getState()));
            final Location right = getBlockCenter(getRightChest((Chest) chestBlock.getState()));

            return new Location(left.getWorld(), (left.getX() + right.getX()) / 2.0, left.getY() + 0.2, (left.getZ() + right.getZ()) / 2.0);
        } else {
            return getBlockCenter(chestBlock).add(0.0, 0.2, 0.0);
        }
    }

    private static Location getBlockCenter(final Block block) {
        return block.getLocation().add(0.5, 0, 0.5);
    }

    private final class RenderRegistry {
        private final List<PlayerRenderEntry> playerRegistry = new ArrayList<>();
        private final List<ChunkRenderEntry> chunkRegistry = new ArrayList<>();

        public void addRender(final Player target, final int chunkX, final int chunkZ) {
            final PlayerRenderEntry player = getPlayer(target.getUniqueId(), true);
            final ChunkRenderEntry chunk = getChunk(chunkX, chunkZ, true);

            assert player != null;
            player.addRender(chunk);

            assert chunk != null;
            chunk.addRender(player);
        }

        public void removeRender(final Player target) {
            removeRender(target.getUniqueId());
        }

        void removeRender(final UUID offlinePlayer) {
            final PlayerRenderEntry player = getPlayer(offlinePlayer, false);

            if (player != null) {
                player.streamRenders().forEach(this::doRemoveChunk);
                doRemovePlayer(player);
            }
        }

        public void removeRender(final int chunkX, final int chunkZ) {
            final ChunkRenderEntry chunk = getChunk(chunkX, chunkZ, false);

            if (chunk != null) {
                chunk.streamRenders().forEach(this::doRemovePlayer);
                doRemoveChunk(chunk);
            }
        }

        private void doRemoveChunk(final ChunkRenderEntry chunk) {
            final int index = Collections.binarySearch(chunkRegistry, chunk);

            if (index >= 0)
                chunkRegistry.remove(index);
        }

        private void doRemovePlayer(final PlayerRenderEntry player) {
            final int index = Collections.binarySearch(playerRegistry, player);

            if (index >= 0)
                playerRegistry.remove(index);
        }

        public void updateRenders(final Player target, final int chunkRadius, final ChunkEntryChangeHandler onAdd, final ChunkEntryChangeHandler onRemove) {
            final PlayerRenderEntry player = getPlayer(target.getUniqueId(), true);
            final UUID worldID = target.getWorld().getUID();
            final int chunkX = target.getLocation().getBlockX() >> 4;
            final int chunkZ = target.getLocation().getBlockZ() >> 4;

            final int xMax = chunkX + chunkRadius;
            final int xMin = chunkX - chunkRadius;
            final int zMax = chunkZ + chunkRadius;
            final int zMin = chunkZ - chunkRadius;

            assert player != null;
            final List<ChunkRenderEntry> toRemove = new ArrayList<>();
            player.streamRenders()
                    .filter(chunk -> {
                                final ChestNameConfig.ChestNameWorldEntry.ChestNameChunkEntry chestChunk = config.getChunkEntry(worldID, chunk.getRender().x(), chunk.getRender().z());

                                return chunk.getRender().x() < xMax ||
                                        chunk.getRender().x() < xMin ||
                                        chunk.getRender().z() > zMax ||
                                        chunk.getRender().z() < zMin ||
                                        (chestChunk != null && chestChunk.isDirty());
                            }
                    )
                    .forEach(chunk -> {
                        toRemove.add(chunk);
                        onRemove.onChange(chunk);
                    });
            toRemove.forEach(player::removeRender);

            for (int x = xMin; x <= xMax; ++x)
                for (int z = zMin; z <= zMax; ++z) {
                    final ChunkRenderEntry chunk = getChunk(x, z, true);

                    assert chunk != null;
                    if (player.addRender(chunk))
                        onAdd.onChange(chunk);

                    chunk.addRender(player);
                }
        }

        public Stream<PlayerRenderEntry> streamPlayers() {
            return playerRegistry.stream();
        }

        public Stream<ChestNameConfig.ChestNameWorldEntry.ChestNameChunkEntry> streamEntries(final Player target) {
            final PlayerRenderEntry player = getPlayer(target.getUniqueId(), false);
            final UUID worldID = target.getWorld().getUID();

            if (player == null)
                return null;
            else
                return player.streamRenders()
                        .map(chunkEntry -> config.getChunkEntry(worldID, chunkEntry.getRender().x(), chunkEntry.getRender().z()));
        }

        private ChunkRenderEntry getChunk(final int chunkX, final int chunkZ, final boolean addIfMissing) {
            final ChunkRenderEntry find = new ChunkRenderEntry(chunkX, chunkZ);

            final int index = Collections.binarySearch(chunkRegistry, find);

            if (index >= 0)
                return chunkRegistry.get(index);
            else if (addIfMissing) {
                chunkRegistry.add(-(index + 1), find);
                return find;
            }

            return null;
        }

        private PlayerRenderEntry getPlayer(final UUID player, final boolean addIfMissing) {
            final PlayerRenderEntry find = new PlayerRenderEntry(player);

            final int index = Collections.binarySearch(playerRegistry, find);

            if (index >= 0)
                return playerRegistry.get(index);
            else if (addIfMissing) {
                playerRegistry.add(-(index + 1), find);
                return find;
            }

            return null;
        }
    }


    private interface RenderEntry<T extends Comparable<T>, R> extends Comparable<RenderEntry<T, R>> {
        T getRender();
        boolean addRender(final R r);
        boolean removeRender(final R r);
        boolean containsRender(final R r);
        Stream<R> streamRenders();

        @Override
        default int compareTo(final RenderEntry<T, R> o) {
            return getRender().compareTo(o.getRender());
        }
    }

    private static final class PlayerRenderEntry implements RenderEntry<UUID, ChunkRenderEntry> {
        private final UUID player;
        private final List<ChunkRenderEntry> chunks = new ArrayList<>();

        public PlayerRenderEntry(final UUID player) {
            this.player = player;
        }

        @Override
        public UUID getRender() {
            return player;
        }

        @Override
        public boolean addRender(final ChunkRenderEntry chunk) {
            final int index = Collections.binarySearch(chunks, chunk);

            if (index < 0) {
                chunks.add(-(index + 1), chunk);
                return true;
            }

            return false;
        }

        @Override
        public boolean removeRender(final ChunkRenderEntry chunk) {
            final int index = Collections.binarySearch(chunks, chunk);

            if (index >= 0) {
                chunks.remove(index);
                return true;
            }

            return false;
        }

        @Override
        public boolean containsRender(final ChunkRenderEntry chunkRenderEntry) {
            return Collections.binarySearch(chunks, chunkRenderEntry) >= 0;
        }

        @Override
        public Stream<ChunkRenderEntry> streamRenders() {
            return chunks.stream();
        }
    }


    private static final class ChunkRenderEntry implements RenderEntry<ChunkRenderEntry.ChunkCoordinate, PlayerRenderEntry> {
        private final ChunkCoordinate coords;
        private final List<PlayerRenderEntry> players = new ArrayList<>();

        public ChunkRenderEntry(final int chunkX, final int chunkZ) {
            coords = new ChunkCoordinate(chunkX, chunkZ);
        }


        @Override
        public ChunkCoordinate getRender() {
            return coords;
        }

        @Override
        public boolean addRender(final PlayerRenderEntry player) {
            final int index = Collections.binarySearch(players, player);

            if (index < 0) {
                players.add(-(index + 1), player);
                return true;
            }

            return false;
        }

        @Override
        public boolean removeRender(final PlayerRenderEntry player) {
            final int index = Collections.binarySearch(players, player);

            if (index >= 0) {
                players.remove(index);
                return true;
            }

            return false;
        }

        @Override
        public boolean containsRender(final PlayerRenderEntry chunkRenderEntry) {
            return Collections.binarySearch(players, chunkRenderEntry) >= 0;
        }

        @Override
        public Stream<PlayerRenderEntry> streamRenders() {
            return players.stream();
        }

        public record ChunkCoordinate(int x, int z) implements Comparable<ChunkCoordinate> {
            @Override
            public int compareTo(final ChunkCoordinate o) {
                final int compX = Integer.compare(x, o.x);

                if (compX == 0)
                    return Integer.compare(z, o.z);

                return compX;
            }
        }
    }

    private interface ChunkEntryChangeHandler {
        void onChange(final ChunkRenderEntry chunk);
    }
}
