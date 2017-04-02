package me.happyman.utils;

import me.happyman.source;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;

public abstract class Verifier implements Listener
{
    private static HashMap<Player, String> decisions;
    private static HashMap<Player, Verifier> pendingVerifiers;
    public static source plugin;

    public abstract void handleResponse(Player p, String response);

    public Verifier(source plugIn)
    {
        plugin = plugIn;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        pendingVerifiers = new HashMap<Player, Verifier>();
        decisions = new HashMap<Player, String>();
    }

    public static HashMap<Player, Verifier> getPendingVerifiers()
    {
        return pendingVerifiers;
    }

    public static boolean isVerifier(Player p)
    {
        return getPendingVerifiers().containsKey(p);
    }

    public HashMap<Player, String> getDecisions()
    {
        return decisions;
    }

    public String getDecision(Player p)
    {
        if (getDecisions().containsKey(p))
        {
            return getDecisions().get(p);
        }
        return "";
    }

    protected void bindVerifier(Player p, Verifier v)
    {
        getPendingVerifiers().put(p, v);
    }

    public void releaseVerifier(final Player p)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (getDecisions().containsKey(p))
                {
                    getDecisions().remove(p);
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage(plugin.loggerPrefix() + ChatColor.YELLOW + "Chat input" +
                            " was already disassociated from the player!");
                }

                if (isVerifier(p))
                {
                    getPendingVerifiers().remove(p);
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage(plugin.loggerPrefix() + ChatColor.YELLOW + "Pending verifier" +
                            " was already removed from the pending verifier list!");
                }
            }
        }, 2);
    }
}
