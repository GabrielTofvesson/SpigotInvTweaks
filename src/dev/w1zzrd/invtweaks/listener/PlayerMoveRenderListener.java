package dev.w1zzrd.invtweaks.listener;

import dev.w1zzrd.invtweaks.feature.NamedChestManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PlayerMoveRenderListener implements Listener {
    public static final int RENDER_RADIUS = 3;

    private final List<Chunk> trackers = new ArrayList<>();
    private final List<UUID> tracked = new ArrayList<>();
    private final NamedChestManager manager;

    public PlayerMoveRenderListener(final NamedChestManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        final int index = Collections.binarySearch(tracked, event.getPlayer().getUniqueId());
        final Player who = event.getPlayer();
        final Chunk chunk = who.getLocation().getChunk();

        if (index < 0) {
            final int actualIndex = -(index + 1);
            trackers.add(actualIndex, chunk);
            tracked.add(actualIndex, who.getUniqueId());
            triggerRender(who);
        }
        else if (!trackers.get(index).equals(event.getPlayer().getLocation().getChunk())) {
            trackers.set(index, chunk);
            triggerRender(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final int index = Collections.binarySearch(tracked, event.getPlayer().getUniqueId());

        // Should always be true
        if (index < 0) {
            trackers.add(-(index + 1), event.getPlayer().getLocation().getChunk());
            tracked.add(-(index + 1), event.getPlayer().getUniqueId());
            triggerRender(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        final int index = Collections.binarySearch(tracked, event.getPlayer().getUniqueId());

        // Should always be true
        if (index >= 0) {
            trackers.remove(index);
            tracked.remove(index);
            manager.untrackPlayer(event.getPlayer());
        }
    }

    private void triggerRender(final Player player) {
        manager.renderTags(player, RENDER_RADIUS);
    }
}
