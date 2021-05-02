package dev.w1zzrd.invtweaks.serialization;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serializable dataclass holding a collection of {@link UUID} objects
 */
public class UUIDList implements ConfigurationSerializable {

    /**
     * This is public to decrease performance overhead
     */
    public final List<UUID> uuids;

    /**
     * Wrap a backing list of {@link UUID} objects to enable configuration serialization
     * @param backingList Modifiable, backing list of {@link UUID} objects
     */
    public UUIDList(final List<UUID> backingList) {
        uuids = backingList;
    }

    /**
     * Create a blank list of {@link UUID} objects
     */
    public UUIDList() {
        this(new ArrayList<>());
    }

    /**
     * Deserialize serialized UUID strings
     * @param values Data to deserialize
     */
    public UUIDList(final Map<String, Object> values) {
        this();
        if (values.containsKey("values"))
            uuids.addAll(((Collection<String>)values.get("values")).stream().map(UUID::fromString).collect(Collectors.toSet()));
    }

    @Override
    public Map<String, Object> serialize() {
        return Collections.singletonMap("values", uuids.stream().map(UUID::toString).collect(Collectors.toList()));
    }
}
