package me.happyman.Other;

import me.happyman.worlds.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.happyman.Plugin.*;
import static me.happyman.worlds.WorldType.getWorldType;

public class HealAndDamage implements CommandExecutor
{
    public static final String DAMAGE_CMD_NAME = "damage";
    public static final String HEAL_CMD_NAME = "heal";

    public HealAndDamage()
    {
        setExecutor(DAMAGE_CMD_NAME, this);
        setExecutor(HEAL_CMD_NAME, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player p = null;
        if (sender instanceof Player)
        {
            p = (Player)sender;
        }
        if (matchesCommand(label, DAMAGE_CMD_NAME))
        {
            double damageAmount = 3.5;
            if (!(sender instanceof Player) && args.length == 0)
            {
                displayHelpMessage(sender, label);
            }
            else if (args.length == 0)
            {
                p.damage(damageAmount);
            }
            else
            {
                Player victim = Bukkit.getPlayer(args[0]);

                if (victim != null)
                {
                    if (getWorldType(victim.getWorld()) == WorldType.SMASH)
                    {
                        sender.sendMessage(ChatColor.RED + "That player is in a Smash world and cannot be harmed! Try /dmg!");
                    }
                    else
                    {
                        victim.damage(damageAmount);
                    }
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                }
            }
            return true;
        }
        else if (matchesCommand(label, HEAL_CMD_NAME))
        {
            if (!(sender instanceof Player) && args.length == 0)
            {
                displayHelpMessage(sender, label);
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
                    sender.sendMessage(loggerPrefix() + ChatColor.RED + "Player not found!");
                }
            }
            return true;
        }
        return false;
    }
}
