package dev.w1zzrd.invtweaks.serialization;

import dev.w1zzrd.spigot.wizcompat.serialization.SimpleReflectiveConfigItem;

import java.util.Map;

public class SearchConfig extends SimpleReflectiveConfigItem {

    private int searchRadiusX, searchRadiusY, searchRadiusZ;

    /**
     * Required constructor for deserializing data
     *
     * @param mappings Data to deserialize
     */
    public SearchConfig(Map<String, Object> mappings) {
        super(mappings);
    }

    public int getSearchRadiusX() {
        return searchRadiusX;
    }

    public int getSearchRadiusY() {
        return searchRadiusY;
    }

    public int getSearchRadiusZ() {
        return searchRadiusZ;
    }
}
