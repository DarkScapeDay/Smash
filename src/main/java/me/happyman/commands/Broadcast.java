package me.happyman.commands;

import me.happyman.source;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Broadcast implements CommandExecutor
{
    private final source plugin;
    private final String cmdName;

    public Broadcast(source plugin)
    {
        this.plugin = plugin;

        cmdName = "broadcast";
        plugin.setExecutor(cmdName, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (plugin.matchesCommand(label, cmdName))
        {
            if (args.length == 0)
            {
                plugin.displayHelpMessage(sender, label);
            }
            else
            {
                String s = "";
                for (int i = 0; i < args.length; i++)
                {
                    s += args[i];
                    if (i < args.length - 1)
                    {
                        s += " ";
                    }
                }
                Bukkit.broadcastMessage("\n \n");
                if (sender instanceof Player)
                { //p.getGameMode().toString() p.sendMessage()
                    Player p = (Player)sender;
                    Bukkit.broadcastMessage("> " + p.getDisplayName() + " - " + s);
                }
                else
                {
                    Bukkit.broadcastMessage("> " + ChatColor.RED + "Server" + ChatColor.WHITE + " - " + s);
                }
                Bukkit.broadcastMessage("\n \n");
            }
            return true;
        }
        return false;
    }
}
