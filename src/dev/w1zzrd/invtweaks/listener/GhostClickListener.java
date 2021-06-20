package dev.w1zzrd.invtweaks.listener;

import dev.w1zzrd.invtweaks.InvTweaksPlugin;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.EnumHand;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GhostClickListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityClick(final PlayerInteractEntityEvent clickEvent) {

        if (clickEvent.getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR &&
                clickEvent.getPlayer().getInventory().getItemInOffHand().getType() == Material.AIR &&
                clickEvent.getHand() == EquipmentSlot.HAND) {
            final Block b = clickEvent.getPlayer().getTargetBlockExact(6);
            if (b != null) {
                try {
                    final Method getNMS = findDeclaredMethod(b.getClass(), "getNMS");
                    if (getNMS == null) {
                        Bukkit.getLogger().warning(InvTweaksPlugin.LOG_PLUGIN_NAME + " Cannot find NMS getter for blocks: ghost click will not function!");
                        return;
                    }
                    getNMS.setAccessible(true);

                    final Object nmsBlock = getNMS.invoke(b);

                    final Method interact = findDeclaredMethod(nmsBlock.getClass(), "interact");
                    if (interact == null) {
                        Bukkit.getLogger().warning(InvTweaksPlugin.LOG_PLUGIN_NAME + " Cannot find interact method for blocks: ghost click will not function!");
                        return;
                    }
                    interact.setAccessible(true);

                    final Field world = findDeclaredField(b.getClass(), "world");
                    if (world == null) {
                        Bukkit.getLogger().warning(InvTweaksPlugin.LOG_PLUGIN_NAME + " Cannot find world field for blocks: ghost click will not function!");
                        return;
                    }
                    world.setAccessible(true);

                    final Field entity = findDeclaredField(clickEvent.getPlayer().getClass(), "entity");
                    if (entity == null) {
                        Bukkit.getLogger().warning(InvTweaksPlugin.LOG_PLUGIN_NAME + " Cannot find entity field for players: ghost click will not function!");
                        return;
                    }
                    entity.setAccessible(true);

                    final BlockPosition pos = new BlockPosition(b.getX(), b.getY(), b.getZ());

                    interact.invoke(
                            nmsBlock,
                            world.get(b),
                            entity.get(clickEvent.getPlayer()),
                            EnumHand.a,
                            MovingObjectPositionBlock.a(
                                    new Vec3D(clickEvent.getPlayer().getLocation().getX(), clickEvent.getPlayer().getLocation().getY(), clickEvent.getPlayer().getLocation().getZ()),
                                    EnumDirection.e,
                                    pos
                            )
                    );
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Method findDeclaredMethod(Class<?> start, final String name) {
        do {
            for (final Method check : start.getDeclaredMethods())
                if (check.getName().equals(name))
                    return check;
            start = start.getSuperclass();
        } while(start != null);
        return null;
    }

    private static Field findDeclaredField(Class<?> start, final String name) {
        do {
            for (final Field check : start.getDeclaredFields())
                if (check.getName().equals(name))
                    return check;
            start = start.getSuperclass();
        } while(start != null);
        return null;
    }
}
