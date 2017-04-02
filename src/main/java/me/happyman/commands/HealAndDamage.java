package me.happyman.commands;

import me.happyman.source;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Timothy on 12/16/2016.
 *
 * @version 2016.12.16
 * @author HappyMan
 */
public class HealAndDamage implements CommandExecutor
{
    private final source plugin;
    public static final String DAMAGE_CMD_NAME = "damage";
    public static final String HEAL_CMD_NAME = "heal";

    public HealAndDamage(source plugin)
    {
        this.plugin = plugin;

        plugin.setExecutor(DAMAGE_CMD_NAME, this);
        plugin.setExecutor(HEAL_CMD_NAME, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player p = null;
        if (sender instanceof Player)
        {
            p = (Player)sender;
        }
        if (plugin.matchesCommand(label, DAMAGE_CMD_NAME))
        {
            double damageAmount = 3.5;
            if (!(sender instanceof Player) && args.length == 0)
            {
                plugin.displayHelpMessage(sender, label);
            }
            else if (args.length == 0)
            {
                p.damage(damageAmount);
            }
            else
            {
                if (Bukkit.getPlayer(args[0]) != null)
                {
                    Bukkit.getPlayer(args[0]).damage(damageAmount);
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, HEAL_CMD_NAME))
        {
            if (!(sender instanceof Player) && args.length == 0)
            {
                plugin.displayHelpMessage(sender, label);
            }
            else if (args.length == 0)
            {
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.setSaturation(7);
                p.sendMessage(ChatColor.GREEN + "You have healed yourself!");
            }
            else
            {
                for (Player player : Bukkit.getOnlinePlayers())
                {
                    if (player.getName().equalsIgnoreCase(args[0]))
                    {
                        sender.sendMessage("You have healed " + player.getDisplayName() + ChatColor.WHITE + "!");
                        player.setHealth(player.getMaxHealth());
                        player.sendMessage(ChatColor.GREEN + "You have been healed by " + sender.getName() + "!");
                        return true;
                    }
                }
                if (sender instanceof Player)
                {
                     sender.sendMessage(ChatColor.RED + "Player not found!");
                }
                else
                {
                    sender.sendMessage(plugin.loggerPrefix() + ChatColor.RED + "Player not found!");
                }
            }
            return true;
        }
        return false;
    }
}
