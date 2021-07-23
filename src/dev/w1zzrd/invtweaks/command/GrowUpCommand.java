package dev.w1zzrd.invtweaks.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.Player;

import java.util.stream.Stream;

import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue;

public class GrowUpCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (assertTrue(args.length == 0 || sender instanceof Player, "Only players can run this command", sender))
            return true;

        final Stream<Breedable> breedables = Commands.getCommandEntityRadius(sender, args, 0, Breedable.class);
        if (breedables == null)
            return true;

        breedables.filter(it -> !it.getAgeLock() && !it.isAdult()).forEach(Ageable::setAdult);

        return true;
    }
}
