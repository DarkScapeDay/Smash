package me.happyman.utils;

import me.happyman.commands.ForcefieldManager;
import me.happyman.source;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class Forcefield
{
    private int forcefieldDamageTask;
    private int forcefieldWarningTask;
    private final source plugin;

    public Forcefield(source plugin)
    {
        this.plugin = plugin;
        forcefieldDamageTask = 0;
        forcefieldWarningTask = 0;
    }

    /**
     * Is the player outside of the forcefield?
     * @param p - The player we want to check
     * @return true if the player is outside of the forcefield
     */
    private static boolean isOutsideForcefield(Player p, World world, float[] params, boolean constrainCeiling)
    {
        double x = p.getLocation().getX();
        double y = p.getLocation().getY();
        double z = p.getLocation().getZ();

        for (String exemptTeam : ForcefieldManager.getExemptTeams())
        {
            for (Team playerTeam : p.getScoreboard().getTeams())
            {
                if (playerTeam.getName().equals(exemptTeam))
                {
                    return false;
                }
            }
        }

        if (p.getWorld().equals(world))
        {
            if (params.length == 4) //sphere
            {
                return Math.sqrt(Math.pow(x-params[0], 2) + Math.pow(y-params[1], 2) + Math.pow(z-params[2], 2)) > params[3] && (constrainCeiling || Math.sqrt(Math.pow(x-params[0], 2) + Math.pow(z-params[2], 2)) > params[3]/2);
            }
            else if (params.length == 5) //cylinder
            {
                return Math.sqrt(Math.pow(x-params[0], 2) + Math.pow(z-params[2], 2)) > params[3] || constrainCeiling && Math.abs(y-params[1]) > params[4];
            }
            //custom box
            return x < params[0] || x > params[1] || constrainCeiling && (y < params[2] || y > params[3]) || z < params[4] || z > params[5];
        }
        return false;
    }

    /**
     *
     *
     */
    public void disableForcefield()
    {
        plugin.getServer().getScheduler().cancelTask(forcefieldDamageTask);
        plugin.getServer().getScheduler().cancelTask(forcefieldWarningTask);
        forcefieldDamageTask = 0;
        forcefieldWarningTask = 0;
    }


    public boolean isEnabled()
    {
        return forcefieldDamageTask != 0;
    }

    /**
     * This one's for freestyle forcefields
     *
     * @param ffdamage - the damage the forcefield will do after each interval of time
     * @param interval - the 1/f of the forcefield damages
     * @param delay - the delay before the forcefield will actually be enabled
     */
    public void enableForcefield(World world, float[] params, float ffdamage, long interval, long delay)
    {
        disableForcefield();

        forcefieldDamageTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, getForcefieldDamageRunnable(world, params, ffdamage), delay*20, interval*20);
        forcefieldWarningTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, getForcefieldWarningRunnable(world, params), delay*20, 10);
    }

    /**
     * This one's for an McPVP-style forcefield
     */
    public void enableForcefield(World world)
    {
        float[] params = {-500, 500, 0, 128, -500, 500};
        enableForcefield(world, params,7, 0, 0);
    }

    private Runnable getForcefieldDamageRunnable(final World world, final float[] params, final float ffdamage)
    {
        return new Runnable()
        {
            public void run()
            {
                for (Player p : Bukkit.getOnlinePlayers())
                { //plugin.getServer().getOnlinePlayers()
                    if (isOutsideForcefield(p, world, params, true))
                    {
                        p.damage(ffdamage);
                    }
                }
            }
        };
    }

    private Runnable getForcefieldWarningRunnable(final World world, final float[] params)
    {
        return new Runnable()
        {
            public void run()
            {
                for (Player p : Bukkit.getOnlinePlayers())
                { //plugin.getServer().getOnlinePlayers()
                    if (isOutsideForcefield(p, world, params, false) && !p.isDead())
                    {
                        p.sendMessage(ChatColor.RED + "You are outside the forcefield! RUN!!!");
                    }
                }
            }
        };
    }
}