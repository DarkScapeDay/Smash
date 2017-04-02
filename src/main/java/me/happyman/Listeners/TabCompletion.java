package me.happyman.Listeners;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabCompletion implements TabCompleter
{
    public TabCompletion()
    {
        setCompleter(SmashKitManager.KIT_CMD);
        setCompleter(SmashWorldManager.GOTO_WORLD_CMD);
    }

    public void setCompleter(String label)
    {
        PluginCommand cmd = SmashManager.getPlugin().getCommand(label);
        cmd.setTabCompleter(this);
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            List<String> completions = new ArrayList<String>();
            if (SmashManager.getPlugin().matchesCommand(label, SmashKitManager.KIT_CMD))
            {
                for (String kitName : SmashKitManager.getKits())
                {
                    if ((args.length == 0  || kitName.toLowerCase().startsWith(args[0].toLowerCase())) && SmashKitManager.canUseKit((Player)sender, kitName))
                    {
                        completions.add(kitName);
                    }
                }
                return completions;
            }
            else if (SmashManager.getPlugin().matchesCommand(label, SmashWorldManager.GOTO_WORLD_CMD))
            {
                for (World w : SmashWorldManager.getAvaliableWorlds((Player)sender))
                {
                    if (args.length > 0)
                    {
                        if (args[0].toLowerCase().startsWith(("" + SmashWorldManager.SMASH_WORLD_PREFIX.charAt(0)).toLowerCase()))
                        {
                            if (w.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                            {
                                completions.add(w.getName());
                            }
                        }
                        else if (SmashWorldManager.getShortWorldName(w).toLowerCase().startsWith(args[0].toLowerCase()))
                        {
                            completions.add(SmashWorldManager.getShortWorldName(w));
                        }
                    }
                    else if (args.length == 0)
                    {
                        completions.add(w.getName());
                    }
                }
                return completions;
            }
        }
        return null;
    }
}
