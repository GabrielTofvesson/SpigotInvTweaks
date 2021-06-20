package dev.w1zzrd.invtweaks.listener;

import dev.w1zzrd.invtweaks.InvTweaksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.lang.reflect.Constructor;
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

                    final Class<?> enumHand = interact.getParameterTypes()[2];

                    final Field enumHandA = enumHand.getDeclaredField("a");
                    enumHandA.setAccessible(true);

                    final Class<?> movingObjectPositionBlock = interact.getParameterTypes()[3];
                    final Method mopb_a = findDeclaredMethod(movingObjectPositionBlock, "a", 3);
                    if (mopb_a == null) {
                        Bukkit.getLogger().warning(InvTweaksPlugin.LOG_PLUGIN_NAME + " Cannot find movingObjectPositionBlock function: ghost click will not function!");
                        return;
                    }
                    mopb_a.setAccessible(true);

                    final Constructor<?> newVec3D = mopb_a.getParameterTypes()[0].getConstructor(double.class, double.class, double.class);
                    newVec3D.setAccessible(true);

                    final Field enumDirection = findDeclaredField(mopb_a.getParameterTypes()[1], "e");
                    if (enumDirection == null) {
                        Bukkit.getLogger().warning(InvTweaksPlugin.LOG_PLUGIN_NAME + " Cannot find EnumDirection value 'e': ghost click will not function!");
                        return;
                    }
                    enumDirection.setAccessible(true);

                    final Constructor<?> newBlockPosition = mopb_a.getParameterTypes()[2].getDeclaredConstructor(int.class, int.class, int.class);
                    newBlockPosition.setAccessible(true);

                    interact.invoke(
                            nmsBlock,
                            world.get(b),
                            entity.get(clickEvent.getPlayer()),
                            enumHandA.get(null),
                            mopb_a.invoke(
                                    null,
                                    newVec3D.newInstance(
                                            clickEvent.getPlayer().getLocation().getX(),
                                            clickEvent.getPlayer().getLocation().getY(),
                                            clickEvent.getPlayer().getLocation().getZ()
                                    ),
                                    enumDirection.get(null),
                                    newBlockPosition.newInstance(b.getX(), b.getY(), b.getZ())
                            )
                    );
                } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException | NoSuchMethodException | InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Method findDeclaredMethod(final Class<?> in, final String name, final int paramLen) {
        for (final Method check : in.getDeclaredMethods())
            if (check.getParameterTypes().length == paramLen && check.getName().equals(name))
                return check;

        return null;
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
