package dev.w1zzrd.invtweaks.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

import java.util.stream.Stream;

import static dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue;

public class ReWoolCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (assertTrue(args.length == 0 || sender instanceof Player, "Only players can run this command", sender))
            return true;

        final Stream<Sheep> sheep = Commands.getCommandEntityRadius(sender, args, 0, Sheep.class);
        if (sheep == null)
            return true;

        sheep.forEach(it -> it.setSheared(false));

        return true;
    }
}
