package me.happyman.commands;

import me.happyman.source;
import me.happyman.utils.Verifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class CommandVerifier extends Verifier
{
    private static HashMap<Player, String> sentCmds;

    public CommandVerifier(source plugin)
    {
        super(plugin);
        sentCmds = new HashMap<Player, String>();
    }

    public static HashMap<Player, String> getAssociatedCmds()
    {
        return sentCmds;
    }

    public void bindVerifier(Player p, String label, String[] args)
    {
        super.bindVerifier(p, this);
        String sentCmd = label;
        for (String arg : args)
        {
            sentCmd += " " + arg;
        }
        getAssociatedCmds().put(p, sentCmd);
    }

    public String getSentCmd(Player p)
    {
        if (hasSentCmd(p))
        {
            return getAssociatedCmds().get(p);
        }
        return "";
    }

    public static boolean hasSentCmd(Player p)
    {
        return getAssociatedCmds().containsKey(p);
    }

    @Override
    public void releaseVerifier(Player p)
    {
        super.releaseVerifier(p);
        if (getAssociatedCmds().containsKey(p))
        {
            getAssociatedCmds().remove(p);
        }
    }

    @Override
    public void handleResponse(Player p, String response)
    {
        getDecisions().put(p, response);
        if (getAssociatedCmds().containsKey(p))
        {
            p.performCommand(getAssociatedCmds().get(p));
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(plugin.loggerPrefix() + ChatColor.RED + "Error! Could not find " +
                    "what command the player literally just performed!");
            releaseVerifier(p);
        }
    }
}
