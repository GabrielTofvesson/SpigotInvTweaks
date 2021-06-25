package dev.w1zzrd.invtweaks.serialization;

import dev.w1zzrd.spigot.wizcompat.serialization.SimpleReflectiveConfigItem;
import dev.w1zzrd.spigot.wizcompat.serialization.UUIDList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Persistent data pertaining to /magnet command
 */
public class MagnetData extends SimpleReflectiveConfigItem {

    /**
     * A sorted list of all active magnets
     */
    private volatile UUIDList activeMagnetsUUIDS; // Serializer
    private final transient List<UUID> activeMagnets;
    private final transient List<UUID> activeMagnetsView;

    // Online magnets are a subset of active magnets. The redundancy in the collections is to preserve performance
    private final transient List<UUID> onlineMagnets = new ArrayList<>();
    private final transient List<UUID> onlineMagnetsView = Collections.unmodifiableList(onlineMagnets);

    /**
     * Construct persistent magnet data from serialized data
     * @param mappings Mappings to deserialize
     */
    public MagnetData(final Map<String, Object> mappings) {
        super(mappings);

        activeMagnets = activeMagnetsUUIDS.uuids;
        activeMagnetsView = Collections.unmodifiableList(activeMagnets);

        ensureListIntegrity();
    }

    /**
     * Toggle magnet state for a given player
     * @param magnet Player to toggle state for
     * @return True if player is a magnet after this method call, else false
     */
    public boolean toggleMagnet(final UUID magnet) {
        final int index = Collections.binarySearch(activeMagnets, magnet);

        // See JavaDoc: Collections.binarySearch
        if (index < 0) {
            activeMagnets.add(-(index + 1), magnet);
            addEnabledOnline(magnet);
            return true;
        }
        else {
            activeMagnets.remove(index);
            removeDisabledOnline(magnet);
            return false;
        }
    }

    /**
     * Add a player as a magnet
     * @param magnet Player to activate magnetism for
     * @return True if list of magnets was modified, else false
     */
    public boolean addMagnet(final UUID magnet) {
        final int index = Collections.binarySearch(activeMagnets, magnet);

        // Insert magnet at correct place in list to keep it sorted
        if (index < 0) {
            activeMagnets.add(-(index + 1), magnet);
            addMagnet(magnet);
            return true;
        }

        return false;
    }

    /**
     * Remove a player from magnet list
     * @param magnet Player to disable magnetism for
     * @return True if list of magnets was modified, else false
     */
    public boolean removeMagnet(final UUID magnet) {
        final int index = Collections.binarySearch(activeMagnets, magnet);

        if (index >= 0) {
            activeMagnets.remove(index);
            removeDisabledOnline(magnet);
            return true;
        }

        return false;
    }

    /**
     * Remove many players from magnet list
     * @param magnets Players to disable magnetism for
     * @return True if list of magnets was modified, else false
     */
    public boolean removeMagnets(final Iterable<UUID> magnets) {
        boolean changed = false;
        for(final UUID uuid : magnets) {
            final int index = Collections.binarySearch(activeMagnets, uuid);
            if (index < 0)
                continue;

            activeMagnets.remove(index);
            removeDisabledOnline(uuid);

            changed = true;
        }
        return changed;
    }

    /**
     * Check if a player is a magnet
     * @param check Player to check
     * @return True if player is a magnet, else false
     * @see #isOnlineMagnet(UUID)
     */
    public boolean isMagnet(final UUID check) {
        return Collections.binarySearch(activeMagnets, check) >= 0;
    }

    /**
     * Check if player is a magnet <em>and</em> is online
     * @param check Player to check
     * @return True if player is online and a magnet
     * @see #isMagnet(UUID)
     */
    public boolean isOnlineMagnet(final UUID check) {
        return Collections.binarySearch(onlineMagnets, check) >= 0;
    }

    /**
     * Get view of players with magnetism enabled
     * @return Unmodifiable list of magnets
     */
    public List<UUID> getActiveMagnetsView() {
        return activeMagnetsView;
    }

    /**
     * Get view of online players with magnetism enabled
     * @return Unmodifiable list of online magnets
     */
    public List<UUID> getOnlineMagnetsView() {
        return onlineMagnetsView;
    }

    /**
     * Get amount of magnets
     * @return Number of total magnets
     */
    public int activeMagnets() {
        return activeMagnets.size();
    }

    /**
     * Get amount of online magnets
     * @return Number of online magnets
     */
    public int onlineMagnets() {
        return onlineMagnets.size();
    }

    /**
     * Add player to online magnet list if player is online
     * @param magnet Player to potentially add
     */
    private void addEnabledOnline(final UUID magnet) {
        if (isPlayerOnline(magnet))
            addOnline(magnet);
    }

    /**
     * Add player to online magnet list if player is a magnet
     * @param magnet Player to potentially add
     * @return True if player is a magnet, else false
     */
    public boolean addLoginOnline(final UUID magnet) {
        if (isMagnet(magnet)) {
            addOnline(magnet);
            return true;
        }

        return false;
    }

    /**
     * Add player to online magnet list
     * @param magnet Player to add
     */
    private void addOnline(final UUID magnet) {
        onlineMagnets.add(-(Collections.binarySearch(onlineMagnets, magnet) + 1), magnet);
    }

    /**
     * Remove player from online magnet list if they are online
     * @param magnet Player to remove from list
     */
    private void removeDisabledOnline(final UUID magnet) {
        if (isPlayerOnline(magnet))
            removeOnline(magnet);
    }

    /**
     * Remove player from online magnet list on logout
     * @param magnet Player to remove
     * @return True if player was an online magnet, else false
     */
    public boolean removeLogoutOnline(final UUID magnet) {
        if (isOnlineMagnet(magnet)) {
            removeOnline(magnet);
            return true;
        }

        return false;
    }

    /**
     * Remove a player from the online magnet list
     * @param magnet Player to remove
     */
    private void removeOnline(final UUID magnet) {
        onlineMagnets.remove(Collections.binarySearch(onlineMagnets, magnet));
    }

    /**
     * Potentially expensive method for checking and correcting out-of-order elements in the sorted magnet list.
     * This exists because I do not doubt that some idiot will try and manually edit the data files (probably me),
     * without taking into consideration the fact that the list *must* be sorted for binary search to work (kind of).
     * <br><br>
     * This method has O(n) complexity if the list is sorted, else (probably) O(n log n). Timsort (default
     * {@link Arrays#sort} implementation) is used, as it is assumed that the list is only out of order if a
     * small total number of elements are out of order
     */
    private void ensureListIntegrity() {
        CHECK_SORTED:{
            for (int i = activeMagnets.size() - 2; i >= 0; --i)
                if (activeMagnets.get(i).compareTo(activeMagnets.get(i + 1)) >= 0)
                    break CHECK_SORTED;

            // All elements were in order
            return;
        }

        // Someone probably manually added a UUID to the beginning (or end) of the list or something...
        activeMagnets.sort(Comparator.naturalOrder());
    }


    /**
     * Check if a given player is online
     * @param check Player to check
     * @return True if player is online, else false
     */
    private static boolean isPlayerOnline(final UUID check) {
        final Player player = Bukkit.getPlayer(check);
        return player != null && player.isOnline();
    }

    /**
     * Create an empty {@link MagnetData} configuration object
     * @return Blank (valid) object
     */
    public static MagnetData blank() {
        return new MagnetData(Collections.singletonMap("activeMagnetsUUIDS", new UUIDList()));
    }
}
