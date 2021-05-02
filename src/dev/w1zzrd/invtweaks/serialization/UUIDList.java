package dev.w1zzrd.invtweaks.serialization;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;
import java.util.stream.Collectors;

public class UUIDList implements ConfigurationSerializable {

    public final List<UUID> uuids;

    public UUIDList(final List<UUID> backingList) {
        uuids = backingList;
    }

    public UUIDList() {
        this(new ArrayList<>());
    }

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
