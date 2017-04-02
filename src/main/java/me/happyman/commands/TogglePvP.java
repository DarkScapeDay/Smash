package me.happyman.commands;

import me.happyman.source;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TogglePvP implements CommandExecutor
{
    private final source plugin;
    private final String cmdName;

    public TogglePvP(source plugin)
    {
        this.plugin = plugin;

        cmdName = "pvp";
        plugin.setExecutor(cmdName, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (plugin.matchesCommand(label, cmdName))
        {
            World worldToAffect = null;
            if (args.length >= 2)
            {
                for (int i = 0; i < plugin.getServer().getWorlds().size(); i++)
                {
                    if (plugin.getServer().getWorlds().get(i).getName().equalsIgnoreCase(args[1]))
                    {
                        worldToAffect = plugin.getServer().getWorlds().get(i);
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
                    worldToAffect = plugin.getServer().getWorlds().get(0);
                }
            }
            if (args.length >= 1)
            {
                if (!plugin.isTrue(args[0]) && !plugin.isFalse(args[0]))
                {
                    plugin.displayHelpMessage(sender, label);
                    return true;
                }
                else if (plugin.isTrue(args[0]))
                {
                    worldToAffect.setPVP(true);
                }
                else if (plugin.isFalse(args[0]))
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
