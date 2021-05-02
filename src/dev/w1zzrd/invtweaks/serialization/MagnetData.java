package dev.w1zzrd.invtweaks.serialization;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

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

    public MagnetData(final Map<String, Object> mappings) {
        super(mappings);

        activeMagnets = activeMagnetsUUIDS.uuids;
        activeMagnetsView = Collections.unmodifiableList(activeMagnets);

        ensureListIntegrity();
    }

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

    public boolean removeMagnet(final UUID magnet) {
        final int index = Collections.binarySearch(activeMagnets, magnet);

        if (index >= 0) {
            activeMagnets.remove(index);

            removeDisabledOnline(magnet);

            return true;
        }

        return false;
    }

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

    public boolean isMagnet(final UUID check) {
        return Collections.binarySearch(activeMagnets, check) >= 0;
    }

    public boolean isOnlineMagnet(final UUID check) {
        return Collections.binarySearch(onlineMagnets, check) >= 0;
    }

    public List<UUID> getActiveMagnetsView() {
        return activeMagnetsView;
    }

    public List<UUID> getOnlineMagnetsView() {
        return onlineMagnetsView;
    }

    public int activeMagnets() {
        return activeMagnets.size();
    }

    public int onlineMagnets() {
        return onlineMagnets.size();
    }

    private void addEnabledOnline(final UUID magnet) {
        if (isPlayerOnline(magnet))
            addOnline(magnet);
    }

    public boolean addLoginOnline(final UUID magnet) {
        if (isMagnet(magnet)) {
            addOnline(magnet);
            return true;
        }

        return false;
    }

    private void addOnline(final UUID magnet) {
        onlineMagnets.add(-(Collections.binarySearch(onlineMagnets, magnet) + 1), magnet);
    }

    private void removeDisabledOnline(final UUID magnet) {
        if (isPlayerOnline(magnet))
            removeOnline(magnet);
    }

    public boolean removeLogoutOnline(final UUID magnet) {
        if (isOnlineMagnet(magnet)) {
            removeOnline(magnet);
            return true;
        }

        return false;
    }

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


    private static boolean isPlayerOnline(final UUID check) {
        final Player player = Bukkit.getPlayer(check);
        return player != null && player.isOnline();
    }


    public static MagnetData blank() {
        return new MagnetData(Collections.singletonMap("activeMagnetsUUIDS", new UUIDList()));
    }
}
