package me.happyman.commands;

import me.happyman.Listeners.RainPreventor;
import me.happyman.source;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DisableWeather implements CommandExecutor
{
    private final source plugin;
    private final String cmdName;

    public DisableWeather(source plugin)
    {
        this.plugin = plugin;
        new RainPreventor(this.plugin);

        cmdName = "weather";
        plugin.setExecutor(cmdName, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (label.equalsIgnoreCase(cmdName))
        {
            String[] duration = new String[1];;
            if (args.length >= 2)
            {
                duration[0] = args[1];
            }
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "Error! You aren't even in a world! How am I supposed to know for which world to change the weather, huh?");
            }
            else if ((args.length != 2 && args.length != 1) || args.length >= 2 && !plugin.numericArgs(duration) || args[0].equalsIgnoreCase("help"))
            {
                plugin.displayHelpMessage(sender, cmdName);
            }
            else
            {  //This is just to show off the EventHandler.
                Player p = (Player)sender;
                sender.sendMessage(ChatColor.GRAY + "Weather is disabled! Contact HappyMan for technical support.");
                if (args[0].equalsIgnoreCase("rain"))
                {
                    p.getWorld().setStorm(true);
                }
                else if (args[0].equalsIgnoreCase("thunder"))
                {
                    p.getWorld().setThundering(true);
                }
                else if (args[0].equalsIgnoreCase("clear"))
                {
                    p.getWorld().setStorm(false);
                }
            }
            return true;
        }
        return false;
    }
}
