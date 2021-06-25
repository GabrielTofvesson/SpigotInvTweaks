package dev.w1zzrd.invtweaks.serialization;

import java.util.Map;

public class ChestNameConfig extends SimpleReflectiveConfigItem {

    private Map<String, String> locs;

    /**
     * Required constructor for deserializing data
     *
     * @param mappings Data to deserialize
     */
    public ChestNameConfig(Map<String, Object> mappings) {
        super(mappings);
    }
}
