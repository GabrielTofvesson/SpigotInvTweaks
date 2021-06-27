package dev.w1zzrd.invtweaks.serialization;

import dev.w1zzrd.spigot.wizcompat.serialization.SimpleReflectiveConfigItem;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Stream;

public class ChestNameConfig extends SimpleReflectiveConfigItem {

    private List<ChestNameWorldEntry> worldEntries;

    public ChestNameConfig(Map<String, Object> mappings) {
        super(mappings);
    }

    public ChestNameConfig() {
        this(Collections.emptyMap());
        worldEntries = new ArrayList<>();
    }

    public ChestNameWorldEntry getEntry(final UUID worldID, final boolean addIfMissing) {
        final int index = indexOf(worldID);

        if (index >= 0)
            return worldEntries.get(index);
        else if (addIfMissing) {
            final ChestNameWorldEntry entry = new ChestNameWorldEntry(worldID);
            worldEntries.add(-(index + 1), entry);
            return entry;
        }

        return null;
    }

    public ChestNameWorldEntry getEntry(final UUID worldID) {
        return getEntry(worldID, true);
    }

    public ChestNameWorldEntry.ChestNameChunkEntry getChunkEntry(final UUID worldID, final int chunkX, final int chunkZ) {
        final int index = indexOf(worldID);

        if (index >= 0)
            return worldEntries.get(index).getChunk(chunkX, chunkZ);

        return null;
    }

    public ChestNameWorldEntry.ChestNameChunkEntry.ChestNameConfigEntry getEntryAt(final UUID worldID, final Location location) {
        final ChestNameWorldEntry.ChestNameChunkEntry chunk = getChunkEntry(worldID, location.getBlockX() >> 4, location.getBlockZ() >> 4);

        if (chunk != null) {
            return chunk.getEntry(location.getBlockX(), location.getBlockY(), location.getBlockZ(), false);
        }

        return null;
    }

    public boolean contains(final UUID worldID) {
        return getEntry(worldID) != null;
    }

    public void add(final UUID worldID) {
        getEntry(worldID);
    }

    public void remove(final UUID worldID) {
        final int index = indexOf(worldID);

        if (index >= 0)
            worldEntries.remove(index);
    }

    private int indexOf(final UUID worldID) {
        return Collections.binarySearch(worldEntries, new ChestNameWorldEntry(worldID));
    }

    public void deleteEmptyWorld(final ChestNameWorldEntry world) {
        if (!world.hasEntries()) {
            final int index = Collections.binarySearch(worldEntries, world);

            if (index >= 0)
                worldEntries.remove(index);
        }
    }


    public static final class ChestNameWorldEntry extends SimpleReflectiveConfigItem implements Comparable<ChestNameWorldEntry> {
        private String worldIDStr;
        private final transient UUID worldID;
        private List<ChestNameChunkEntry> chunks;

        /**
         * Required constructor for deserializing data
         *
         * @param mappings Data to deserialize
         */
        public ChestNameWorldEntry(Map<String, Object> mappings) {
            super(mappings);
            worldID = UUID.fromString(worldIDStr);
        }

        ChestNameWorldEntry(final UUID worldID) {
            super(Collections.emptyMap());
            this.worldID = worldID;
            worldIDStr = worldID.toString();
            chunks = new ArrayList<>();
        }

        public UUID getWorldID() {
            return worldID;
        }

        @Override
        public int compareTo(final ChestNameWorldEntry o) {
            return getWorldID().compareTo(o.getWorldID());
        }

        public String getName(final Location location) {
            final ChestNameChunkEntry chunk = getChunk(location, false);

            if (chunk == null)
                return null;
            else
                return chunk.getName(location);
        }

        public boolean contains(final Location location) {
            return getName(location) != null;
        }

        public boolean hasEntries() {
            return chunks.size() > 0;
        }

        public void add(final Location location, final String name) {
            Objects.requireNonNull(getChunk(location, true)).add(location, name);
        }

        public void remove(final Location location) {
            final int index = indexOf(location);

            if (index >= 0)
                chunks.remove(index);
        }

        private int indexOf(final Location location) {
            return Collections.binarySearch(chunks, new ChestNameChunkEntry(location));
        }

        private int indexOf(final int chunkX, final int chunkZ) {
            return Collections.binarySearch(chunks, new ChestNameChunkEntry(chunkX, chunkZ));
        }

        private ChestNameChunkEntry getChunk(final Location location, final boolean addIfMissing) {
            final int index = indexOf(location);

            if (index >= 0)
                return chunks.get(index);
            else if (addIfMissing) {
                final ChestNameChunkEntry entry = new ChestNameChunkEntry(location);
                chunks.add(-(index + 1), entry);
                return entry;
            }

            return null;
        }

        private ChestNameChunkEntry getChunk(final int chunkX, final int chunkZ) {
            final int index = indexOf(chunkX, chunkZ);

            if (index >= 0)
                return chunks.get(index);
            else
                return null;
        }

        private void clearChunk(final Location location) {
            final int index = indexOf(location);

            if (index >= 0)
                chunks.remove(index);
        }

        public void deleteEmptyChunk(final ChestNameChunkEntry chunk) {
            if (!chunk.hasEntries()) {
                final int index = Collections.binarySearch(chunks, chunk);

                if (index >= 0)
                    chunks.remove(index);
            }
        }

        public static final class ChestNameChunkEntry extends SimpleReflectiveConfigItem implements Comparable<ChestNameChunkEntry> {
            private int x, z;
            private List<ChestNameConfigEntry> entries;
            private transient boolean dirty = false;

            public ChestNameChunkEntry(Map<String, Object> mappings) {
                super(mappings);
            }

            ChestNameChunkEntry(final int x, final int z) {
                super(Collections.emptyMap());
                this.x = x;
                this.z = z;
                entries = new ArrayList<>();
            }

            ChestNameChunkEntry(final Location location) {
                // Convert world coordinates to chunk coordinates
                this(location.getBlockX() >> 4, location.getBlockZ() >> 4);
            }

            public int getX() {
                return x;
            }

            public int getZ() {
                return z;
            }

            public boolean isDirty() {
                return dirty;
            }

            public void setDirty(final boolean dirty) {
                this.dirty = dirty;
            }

            public boolean hasEntries() {
                return entries.size() > 0;
            }

            public void add(final int x, final int y, final int z, final String name) {
                final ChestNameConfigEntry check = getEntry(x, y, z, true);

                assert check != null;
                check.setName(name);

                setDirty(true);
            }

            public void add(final Location location, final String name) {
                add(location.getBlockX(), location.getBlockY(), location.getBlockZ(), name);
            }

            public String getName(final Location location) {
                final ChestNameConfigEntry entry = getEntry(location.getBlockX(), location.getBlockY(), location.getBlockZ(), false);

                if (entry == null)
                    return null;
                else
                    return entry.getName();
            }

            private ChestNameConfigEntry getEntry(final int x, final int y, final int z, final boolean createIfMissing) {
                final ChestNameConfigEntry find = new ChestNameConfigEntry(x, y, z);

                final int index = indexOf(find);

                if (index >= 0)
                    return entries.get(index);
                else if (createIfMissing) {
                    entries.add(-(index + 1), find);
                    return find;
                }

                return null;
            }

            public void removeEntry(final ChestNameConfigEntry entry) {
                final int index = indexOf(entry);

                if (index >= 0) {
                    entries.remove(index);
                    setDirty(true);
                }
            }

            public Stream<ChestNameConfigEntry> streamEntries() {
                return entries.stream();
            }

            private int indexOf(final ChestNameConfigEntry find) {
                return Collections.binarySearch(entries, find);
            }

            @Override
            public int compareTo(final ChestNameChunkEntry o) {
                final int compX = Integer.compare(x, o.x);

                if (compX == 0)
                    return Integer.compare(z, o.z);

                return compX;
            }

            public static final class ChestNameConfigEntry extends SimpleReflectiveConfigItem implements Comparable<ChestNameConfigEntry> {
                private transient Object entity;
                private transient int locInt;
                private String loc;
                private String name;

                public ChestNameConfigEntry(Map<String, Object> mappings) {
                    super(mappings);
                    locInt = Integer.parseInt(loc, 16);
                }

                ChestNameConfigEntry(final int x, final int y, final int z, final String name) {
                    super(Collections.emptyMap());
                    locInt = ((y & 0xFFF) << 8) | ((x & 0xF) << 4) | (z & 0xF);
                    loc = Integer.toString(locInt, 16);
                    this.name = name;
                }

                ChestNameConfigEntry(final int x, final int y, final int z) {
                    this(x, y, z, null);
                }

                ChestNameConfigEntry(final Location location, final String name) {
                    this(location.getBlockX() & 0xF, location.getBlockY(), location.getBlockZ() & 0xF, name);
                }

                ChestNameConfigEntry(final Location location) {
                    this(location, null);
                }

                public Object getEntity(final EntityCreator creator) {
                    if (entity == null)
                        entity = creator.createFakeEntity();

                    return entity;
                }

                public int getChunkX() {
                    return (locInt >>> 4) & 0xF;
                }

                public int getChunkZ() {
                    return locInt & 0xF;
                }

                public int getY() {
                    return (locInt >>> 8) & 0xFFF;
                }

                public String getName() {
                    return name;
                }

                void setName(final String name) {
                    this.name = name;
                }

                @Override
                public int compareTo(final ChestNameConfigEntry o) {
                    return Integer.compare(locInt, o.locInt);
                }

                public interface EntityCreator {
                    Object createFakeEntity();
                }
            }
        }
    }
}
