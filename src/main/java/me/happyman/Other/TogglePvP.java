package me.happyman.Other;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.happyman.Plugin.*;

public class TogglePvP implements CommandExecutor
{
    private static final String TOGGLE_PVP_CMD = "pvp";

    public TogglePvP()
    {
        setExecutor(TOGGLE_PVP_CMD, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (matchesCommand(label, TOGGLE_PVP_CMD))
        {
            World worldToAffect = null;
            if (args.length >= 2)
            {
                for (int i = 0; i < getPlugin().getServer().getWorlds().size(); i++)
                {
                    if (getPlugin().getServer().getWorlds().get(i).getName().equalsIgnoreCase(args[1]))
                    {
                        worldToAffect =  getPlugin().getServer().getWorlds().get(i);
                        break;
                    }
                }
                if (worldToAffect == null)
                {
                    sender.sendMessage(ChatColor.RED + "Error! World \"" + args[1] + "\" not found!");
                    return true;
                }
            }
            else
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    worldToAffect = p.getWorld();
                }
                else
                {
                    worldToAffect =  getPlugin().getServer().getWorlds().get(0);
                }
            }
            if (args.length >= 1)
            {
                if (!isTrue(args[0]) && !isFalse(args[0]))
                {
                    displayHelpMessage(sender, label);
                    return true;
                }
                else if (isTrue(args[0]))
                {
                    worldToAffect.setPVP(true);
                }
                else if (isFalse(args[0]))
                {
                    worldToAffect.setPVP(false);
                }
            }
            else
            {
                worldToAffect.setPVP(!worldToAffect.getPVP());
            }
            String worldAffected = "";
            if (!(sender instanceof Player) || args.length >= 2)
            {
                worldAffected = " in " + worldToAffect.getName();
            }
            String enabledDisabled = "";
            if (worldToAffect.getPVP())
            {
                enabledDisabled = "enabled";
            }
            else
            {
                enabledDisabled = "disabled";
            }
            sender.sendMessage(ChatColor.GREEN + "PvP " + enabledDisabled + worldAffected);
            return true;
        }
        return false;
    }
}
