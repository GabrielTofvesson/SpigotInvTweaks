package dev.w1zzrd.invtweaks.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.stream.Stream;

import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.errorMessage;

public final class Commands {
    private Commands() { throw new UnsupportedOperationException("Functional class"); }

    public static double getCommandRadius(final CommandSender sender, final String[] args, final int radiusIndex) {
        try {
            if (args.length > radiusIndex)
                return Double.parseDouble(args[radiusIndex]);
        } catch (NumberFormatException e) {
            sender.spigot().sendMessage(errorMessage(String.format("\"%s\" is not a number", args[radiusIndex])));
        }

        return Double.NaN;
    }

    public static <T extends Entity> Stream<T> getCommandEntityRadius(
            final CommandSender sender,
            final String[] args,
            final int radiusIndex,
            final Class<T> entityType
    ) {
        final double radius = Commands.getCommandRadius(sender, args, 0);
        if (Double.compare(radius, Double.NaN) == 0)
            return null;

        return
                (radius >= 0 ?
                        ((Player) sender).getNearbyEntities(radius, radius, radius).stream() :
                        Bukkit.getWorlds().stream().flatMap(it -> it.getEntities().stream()))
                        .filter(it -> entityType.isAssignableFrom(it.getClass()))
                        .map(it -> (T) it);
    }
}
