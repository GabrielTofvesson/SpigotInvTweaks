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
import java.util.stream.Collectors;

/**
 * Listener for providing tab completions for all commands in this plugin
 */
public class TabCompletionListener implements Listener {

    private static final List<NamespacedKey> materialTypes = Arrays.stream(Material.values())
            .map(Material::getKey)
            .sorted(Comparator.comparing(NamespacedKey::toString))
            .collect(Collectors.toUnmodifiableList());

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
                    completions.addAll(materialTypes.stream().map(NamespacedKey::toString).filter(it -> it.contains(split[1])).collect(Collectors.toList()));
                } else {
                    completions.clear();
                    completions.addAll(materialTypes.stream().map(NamespacedKey::toString).collect(Collectors.toList()));
                }
            } else if (buffer.startsWith("/magnet ")) {
                completions.clear();
            } else if (buffer.startsWith("/sort ")) {
                completions.clear();
            }
        }
    }
}
