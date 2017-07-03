package me.happyman.Listeners;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.worlds.SmashWorldInteractor;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static me.happyman.worlds.SmashWorldInteractor.getAvaliableWorlds;
import static me.happyman.worlds.SmashWorldManager.*;

public class TabCompletion implements TabCompleter
{
    public TabCompletion()
    {
        setCompleter(SmashKitManager.KIT_CMD);
        setCompleter(GOTO_WORLD_CMD);
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
            else if (SmashManager.getPlugin().matchesCommand(label, GOTO_WORLD_CMD))
            {
                if (label.equalsIgnoreCase(GOTO_WORLD_CMD) || label.equalsIgnoreCase("world"))
                {
                    boolean special = SmashWorldInteractor.hasSpecialPermissions((Player)sender);
                    for (World w : Bukkit.getWorlds())
                    {
                        if (special || !SmashWorldManager.isSmashWorld(w))
                        {
                            completions.add(w.getName());
                        }
                    }
                }
                else
                {
                    for (World w : getAvaliableWorlds((Player)sender))
                    {
                        if (args.length > 0)
                        {
                            if (args[0].toLowerCase().startsWith(("" + SMASH_WORLD_PREFIX.charAt(0)).toLowerCase()))
                            {
                                if (w.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                                {
                                    completions.add(w.getName());
                                }
                            }
                            else if (SmashWorldInteractor.getShortWorldName(w).toLowerCase().startsWith(args[0].toLowerCase()))
                            {
                                completions.add(SmashWorldInteractor.getShortWorldName(w));
                            }
                        }
                        else if (args.length == 0)
                        {
                            completions.add(w.getName());
                        }
                    }
                }
                return completions;
            }
        }
        return null;
    }
}
