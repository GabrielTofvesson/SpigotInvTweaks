package dev.w1zzrd.invtweaks.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;

public final class CommandUtils {
    private CommandUtils() { throw new UnsupportedOperationException("Functional class"); }

    public static boolean assertTrue(final boolean condition, final String message, final CommandSender sender) {
        if (!condition) {
            final TextComponent errorMessage = new TextComponent(message);
            errorMessage.setColor(ChatColor.DARK_RED);
            sender.spigot().sendMessage(errorMessage);
        }

        return !condition;
    }

    public static BaseComponent errorMessage(final String message) {
        final BaseComponent component = new TextComponent(message);
        component.setColor(ChatColor.DARK_RED);
        return component;
    }

    public static BaseComponent successMessage(final String message) {
        final BaseComponent component = new TextComponent(message);
        component.setColor(ChatColor.GREEN);
        return component;
    }
}
