package me.happyman.commands;

import me.happyman.Listeners.Soup;
import me.happyman.source;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class SoupManager implements CommandExecutor
{
    private final source plugin;
    public static final String soupEffyCmdName = "soupefficiency";
    public static final String SHAME_CMD_NAME = "fatshaming";
    public static HashMap<Player, Float> soupEfficiencyGoals;
    public static HashMap<Player, Boolean> fatShamingPreferences;

    public SoupManager(source plugin)
    {
        this.plugin = plugin;
        new Soup(this, plugin);
        soupEfficiencyGoals = new HashMap<Player, Float>();
        fatShamingPreferences = new HashMap<Player, Boolean>();

        plugin.setExecutor(soupEffyCmdName, this);
        plugin.setExecutor(SHAME_CMD_NAME, this);
    }

    public void updateQuickSoupEffy(Player p)
    {
        float dater;
        try
        {
            String data = plugin.getDatum(p, SoupManager.soupEffyCmdName);
            dater = Float.parseFloat(data);
            if (dater > 1)
            {
                dater /= 100;
            }
            if (!validPercentage(dater))
            {
                dater = 0;
                throw new NumberFormatException("Invalid percentage in file");
            }
        }
        catch (NumberFormatException e)
        {
            dater = 0;
            plugin.putDatum(p, SoupManager.soupEffyCmdName, 0);
        }
        soupEfficiencyGoals.put(p, dater);
    }

    public void updateQuickFat(Player p)
    {
        boolean wantsToBeShamed = plugin.getDatum(p, SHAME_CMD_NAME).equalsIgnoreCase("true");
        fatShamingPreferences.put(p, wantsToBeShamed);
    }

    public HashMap<Player, Boolean> fatPreferences()
    {
        return fatShamingPreferences;
    }

    public HashMap<Player, Float> soupPreferences()
    {
        return soupEfficiencyGoals;
    }

    public float getSoupPreference(Player p)
    {
        if (!soupPreferences().containsKey(p))
        {
            updateQuickSoupEffy(p);
        }
        return soupPreferences().get(p);
    }

    public boolean getFatPreference(Player p)
    {
        if (!fatPreferences().containsKey(p))
        {
            updateQuickFat(p);
        }
        return fatPreferences().get(p);
    }

    public boolean validPercentage(float i)
    {
        return i <= 1 && i >= 0;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "Error! You are not in-game.");
            return true;
        }
        if (plugin.matchesCommand(label, soupEffyCmdName))
        {
            Player p = (Player)sender;
            if (args.length == 0)
            {
                plugin.displayHelpMessage(sender, label);
            }
            else
            {
                float input;
                try
                {
                    input = Float.valueOf(args[0]);
                    if (input > 1)
                    {
                        input /= 100;
                    }
                    if (!validPercentage(input))
                    {
                        p.sendMessage(ChatColor.RED + "Error! Please enter a valid percentage (0-100%)!");
                        return true;
                    }
                    if (input < 0.01)
                    {
                        input = 0;
                    }
                    String s = String.valueOf(input*100);
                    if (input == 0)
                    {
                        s = "disabled";
                    }
                    else if (s.lastIndexOf(".") <= 3)
                    {
                        s = "" +  Math.round(input*100) + "%";
                    }
                    p.sendMessage(ChatColor.GREEN + "Soup efficiency preference set to " + s + "!");
                }
                catch (NumberFormatException e)
                {
                    if (plugin.isTrue(args[0]))
                    {
                        input = 1;
                        plugin.putDatum(p, soupEffyCmdName, input);
                        p.sendMessage(ChatColor.GREEN + "Soup notifications set to 100% efficiency!");
                    }
                    else if (plugin.isFalse(args[0]))
                    {
                        input = 0;
                        plugin.putDatum(p, soupEffyCmdName, 0.0);
                        p.sendMessage(ChatColor.GREEN + "Soup notifications disabled!");
                    }
                    else if ("help".equalsIgnoreCase(args[0]))
                    {
                        plugin.displayHelpMessage(sender, soupEffyCmdName);
                        return true;
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "Error! Please enter a numeric input!");
                        return true;
                    }
                }
                plugin.putDatum(p, soupEffyCmdName, input);
                updateQuickSoupEffy(p);
            }
            return true;
        }
        else if (plugin.matchesCommand(label, SHAME_CMD_NAME))
        {
            Player p = (Player)sender;
            if ((args.length > 0 && source.isTrue(args[0])) || (args.length == 0 && !source.isTrue(plugin.getDatum(p, SHAME_CMD_NAME))))
            {
                plugin.putDatum(p, SHAME_CMD_NAME, "true");
                updateQuickFat(p);
                p.sendMessage(ChatColor.GREEN + "Fat shaming enabled!");
            }
            else
            {
                plugin.putDatum(p, SHAME_CMD_NAME, "false");
                updateQuickFat(p);
                p.sendMessage(ChatColor.GREEN + "Fat shaming disabled!");
            }
            return true;
        }
        return false;
    }
}
