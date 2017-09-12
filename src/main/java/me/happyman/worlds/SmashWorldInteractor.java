package me.happyman.worlds;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SmashWorldInteractor
{
    private static final int HIT_TICKS = 20; //The number of ticks between each hit for players
    protected static final int TOURNEY_TOP_PERCENT = 30; //0-100, this is the percentage of players that will act on to the next tournament level in a tournament game.
    private static final float VOTE_KICK_PERCENT = 0.75F;
    private static HashMap<World, List<Player>> readyPlayers = new HashMap<World, List<Player>>();

    private static HashMap<Player, List<Player>> whoToKick = new HashMap<Player, List<Player>>();



    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    protected static void updateHitCooldown(Player p)
    {
        if (SmashWorldManager.isSmashWorld(p.getWorld()))
        {
            p.setMaximumNoDamageTicks(HIT_TICKS);
        }
        else
        {
            p.setMaximumNoDamageTicks(20);
        }
    }

    /**
     * Function: updateHitCooldown
     * Purpose: Updates the hit cooldown for everyone in the given world
     * @param w - The world for which we would like to update everyone's hit cooldown
     */
    protected static void updateHitCooldown(World w)
    {
        for (Player p : w.getPlayers())
        {
            updateHitCooldown(p);
        }
    }

    protected static void addPlayerWhoIHate(Player hater, Player hated)
    {
        if (!whoToKick.containsKey(hater))
        {
            whoToKick.put(hater, new ArrayList<Player>());
        }
        if (!whoToKick.get(hater).contains(hated))
        {
            whoToKick.get(hater).add(hated);
        }
    }

    protected static boolean isHated(Player p, World w)
    {
        if (SmashWorldManager.isSmashWorld(w))
        {
            int haters = 0;
            List<Player> whosInThere = w.getPlayers();
            for (Player potentialHater : whosInThere)
            {
                if (whoToKick.containsKey(potentialHater) && whoToKick.get(potentialHater).contains(p))
                {
                    haters++;
                }
            }
            return haters >= 2 && (float)haters/(whosInThere.size()) >= VOTE_KICK_PERCENT;
        }
        return false;
    }

    protected static void forgetWhoIHate(Player p)
    {
        if (whoToKick.containsKey(p))
        {
            whoToKick.remove(p);
        }
    }

    protected static void forgetReadyState(Player p, World w)
    {
        if (readyPlayers.containsKey(w) && readyPlayers.get(w).contains(p))
        {
            readyPlayers.get(w).remove(p);
        }
    }

    public static boolean isReady(Player p, World w)
    {
        if (!readyPlayers.containsKey(w))
        {
            readyPlayers.put(w, new ArrayList<Player>());
        }
        return readyPlayers.get(w).contains(p);
    }

    protected static boolean setReady(Player p, World w, boolean ready)
    {
        boolean alreadyReady = isReady(p, w);
        if (alreadyReady && !ready)
        {
            readyPlayers.get(w).remove(p);
            return true;
        }
        else if (!alreadyReady && ready)
        {
            readyPlayers.get(w).add(p);
            return true;
        }
        return false;
    }

    protected static int getNumberOfReadyPlayers(World w)
    {
        if (!readyPlayers.containsKey(w))
        {
            return 0;
        }
        return readyPlayers.get(w).size();
    }

    protected static void forgetReadyState(World w)
    {
        if (readyPlayers.containsKey(w))
        {
            readyPlayers.remove(w);
        }
    }
}
