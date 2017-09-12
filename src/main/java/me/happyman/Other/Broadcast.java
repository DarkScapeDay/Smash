package me.happyman.Other;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.happyman.Plugin.*;

public class Broadcast implements CommandExecutor
{
    private final String cmdName;

    public Broadcast()
    {
        cmdName = "broadcast";
        setExecutor(cmdName, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (matchesCommand(label, cmdName))
        {
            if (args.length == 0)
            {
                displayHelpMessage(sender, label);
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
                /**/Bukkit.broadcastMessage("\n \n");
                if (sender instanceof Player)
                { //p.getGameMode().toString() p.sendMessage()
                    Player p = (Player)sender;
                    /**/Bukkit.broadcastMessage("> " + p.getDisplayName() + " - " + s);
                }
                else
                {
                    /**/Bukkit.broadcastMessage("> " + ChatColor.RED + "Server" + ChatColor.WHITE + " - " + s);
                }
                /**/Bukkit.broadcastMessage("\n \n");
            }
            return true;
        }
        return false;
    }
}
