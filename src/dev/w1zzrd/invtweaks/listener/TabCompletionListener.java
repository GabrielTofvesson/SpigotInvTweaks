package dev.w1zzrd.invtweaks.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Listener for providing tab completions for all commands in this plugin
 */
public class TabCompletionListener implements Listener {

    private static final List<NamespacedKey> materialTypes = Arrays.stream(Material.values())
            .map(Material::getKey)
            .sorted(Comparator.comparing(NamespacedKey::toString))
            .collect(Collectors.toUnmodifiableList());

    private static final boolean multiNS;


    static {
        String ns = null;
        for (final Material mat  : Material.values()) {
            if (ns == null) ns = mat.getKey().getNamespace();
            else if (!ns.equals(mat.getKey().getNamespace())) {
                ns = null;
                break;
            }
        }

        multiNS = ns == null;

    }




    @EventHandler
    public void onTabCompleteEvent(final TabCompleteEvent event) {
        if (event.getSender() instanceof Player) {
            final String buffer = event.getBuffer();
            final List<String> completions = event.getCompletions();

            if (buffer.startsWith("/search ")) {
                final String[] split = buffer.split(" ");

                if (split.length > 2) {
                    completions.clear();
                    event.setCancelled(true);
                } else if (split.length == 2) {
                    completions.addAll(getMatching(split[1]).collect(Collectors.toList()));
                } else {
                    completions.clear();
                    completions.addAll(materialTypes.stream().map(it -> multiNS ? it.toString() : it.getKey()).collect(Collectors.toList()));
                }
            } else if (buffer.startsWith("/magnet ")) {
                completions.clear();
            } else if (buffer.startsWith("/sort ")) {
                completions.clear();
            }
        }
    }


    public static Stream<String> getMatching(final String arg) {
        final String[] key = arg.split(":", 2);

        return Arrays.stream(Material.values())
                .filter(it -> (key.length == 1 || it.getKey().getNamespace().equals(key[0])) &&
                        it.getKey().getKey().contains(key[key.length - 1]))
                .map(Material::getKey)
                .map(it -> key.length == 1 ? it.getKey() : it.toString());
    }

    public static Stream<Material> getAllMaterialsMatching(final String arg) {
        final String[] key = arg.split(":", 2);

        return Arrays.stream(Material.values())
                .filter(it -> (key.length == 1 || it.getKey().getNamespace().equals(key[0])) &&
                        it.getKey().getKey().contains(key[key.length - 1]));
    }

    public static Material getMaterialMatching(final String arg) {
        final List<Material> mats = getAllMaterialsMatching(arg).collect(Collectors.toList());

        if (mats.size() != 1)
            return null;

        return mats.get(0);
    }
}
