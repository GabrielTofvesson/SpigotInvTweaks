package dev.w1zzrd.invtweaks.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CapitatorCommand implements CommandExecutor {
    private final Enchantment capitatorEnchantment;

    public CapitatorCommand(final Enchantment capitatorEnchantment) {
        this.capitatorEnchantment = capitatorEnchantment;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (capitatorEnchantment == null) {
            sender.spigot().sendMessage(CommandUtils.errorMessage("Tree capitation is disabled!"));
            return true;
        }

        if (sender instanceof final Player caller) {
            final ItemStack stack = caller.getInventory().getItemInMainHand();

            if (stack.getType().name().endsWith("_AXE")) {
                if (stack.containsEnchantment(capitatorEnchantment)) {
                    stack.removeEnchantment(capitatorEnchantment);
                    sender.spigot().sendMessage(CommandUtils.successMessage("Item is now a regular axe"));
                } else {
                    stack.addEnchantment(capitatorEnchantment, 1);
                    sender.spigot().sendMessage(CommandUtils.successMessage("Item is now a capitator axe"));
                }
            } else
                sender.spigot().sendMessage(CommandUtils.errorMessage("Only axes can be tree capitators!"));
        } else
            sender.spigot().sendMessage(CommandUtils.errorMessage("Only players can create tree capitators!"));

        return true;
    }
}
