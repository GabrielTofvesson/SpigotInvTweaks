package dev.w1zzrd.invtweaks.listener;

import dev.w1zzrd.invtweaks.command.MagnetCommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MagnetismListener implements Listener {

    private final MagnetCommandExecutor magnetCommandExecutor;

    public MagnetismListener(final MagnetCommandExecutor magnetCommandExecutor) {
        this.magnetCommandExecutor = magnetCommandExecutor;
    }

    @EventHandler
    public void onPlayerJoinEvent(final PlayerJoinEvent event) {
        magnetCommandExecutor.onMagnetLogin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDisconnectEvent(final PlayerQuitEvent event) {
        onLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKickedEvent(final PlayerKickEvent event) {
        onLeave(event.getPlayer());
    }

    private void onLeave(final Player player) {
        magnetCommandExecutor.onMagnetLogout(player.getUniqueId());
    }

    private boolean shouldNotifyExecutor(final Player player) {
        return magnetCommandExecutor.isMagnet(player);
    }
}
