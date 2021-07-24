package dev.w1zzrd.invtweaks.listener;

import dev.w1zzrd.spigot.wizcompat.packet.Players;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Objects;

public class SignEditListener implements Listener {
    @EventHandler
    public void onSignClick(final PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                Objects.requireNonNull(event.getClickedBlock()).getState() instanceof Sign &&
                event.getPlayer().isSneaking()) { // Sneak-right-click to edit sign
            Players.openSignEditor(event.getPlayer(), event.getClickedBlock().getLocation());
        }
    }
}
