package me.happyman.utils;

import me.happyman.commands.SmashManager;
import me.happyman.source;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class VelocityModifier implements Listener
{
    public static final float GRAVITY = .0948f;//0.092f;//0.08
    public static final float VELOCITY_TERMINAL = 6.07f;//5.7f;//3.92
    public static final float MAX_PLAYER_LAUNCH_Y_EXPERIMENTAL = 60F;
    private static final int TICK_COOLDOWN = 10;
    private static ArrayList<Player> disallowedJumpers = new ArrayList<Player>();
    private static source plugin;

    private static HashMap<Player, ArrayList<Integer>> kbTasks = new HashMap<Player, ArrayList<Integer>>();

    public VelocityModifier(source plugin)
    {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void cancelKnockback(Player p)
    {
        if (kbTasks.containsKey(p))
        {
            for (int task : kbTasks.get(p))
            {
                Bukkit.getScheduler().cancelTask(task);
            }
            kbTasks.remove(p);
        }
    }

    private static double computeTimeNeededToReachY(double Y)
    {
        final double tolerance = 0.000000001f;
        final int max_it = 60;
        int iterations = 0;
        double lowerBound = 0f;
        double upperBound = 100f;
        double t = -1;
        //Portal middle of lower and upper bound
        while (lowerBound <= upperBound && iterations < max_it)
        {
            t = (lowerBound + upperBound)/2;
            //get value at Portal
            double numerator = GRAVITY*(Y+t*VELOCITY_TERMINAL);
            double denominator = VELOCITY_TERMINAL*VELOCITY_TERMINAL*(Math.exp(GRAVITY*t/VELOCITY_TERMINAL)-1);
            double valueToBeReaching1 = numerator/denominator;
            //depending on Portal, update bounds
            if (valueToBeReaching1 < 1 - tolerance) //need to look left
            {
                upperBound = t - tolerance;
            }
            else if (valueToBeReaching1 > 1 + tolerance) //need to look right
            {
                lowerBound = t + tolerance;
            }
            else //we found it
            {
                //Bukkit.broadcastMessage(ChatColor.BLUE + "" + Portal + " ticks to peak");
                return t;
            }
            iterations++;
        }
        return t;
    }

    private static double computeVyInitialFromTime(double timeToReach)
    {
        return VELOCITY_TERMINAL*(Math.exp(GRAVITY*timeToReach/VELOCITY_TERMINAL)-1);
    }

    public static double computeVyInitial(double yPeak)
    {
        double result = computeVyInitialFromTime(computeTimeNeededToReachY(yPeak));
//        if (result > VELOCITY_TERMINAL)
//        {
//            //Bukkit.broadcastMessage(ChatColor.RED + "TOO HIGH (" + result + " blocks/tick)");
//        }
//        else
//        {
//            //Bukkit.broadcastMessage(ChatColor.GOLD + "VyInitial: " + result + " blocks/tick");
//        }
        return result;
    }

    public static Vector computeInitialLaunch(double xPeak, double yPeak, double zPeak)
    {
        double timeToPeak = computeTimeNeededToReachY(yPeak);
        double yInitial = computeVyInitialFromTime(timeToPeak);
        double rPeak;
        double rInitial;
        double xInitial;
        double zInitial;
        if (xPeak != 0 || zPeak != 0)
        {
            rPeak = Math.sqrt(xPeak*xPeak + zPeak*zPeak);
            rInitial = GRAVITY*rPeak/((1f-Math.exp(-GRAVITY*timeToPeak/VELOCITY_TERMINAL))*VELOCITY_TERMINAL);
            xInitial = rInitial * xPeak / rPeak;
            zInitial = rInitial * zPeak / rPeak;
        }
        else
        {
            xInitial = 0;
            zInitial = 0;
        }
        return new Vector(xInitial, yInitial, zInitial);
    }

    @EventHandler
    public void onSpongeStep(PlayerMoveEvent e)
    {
        final Player p = e.getPlayer();
        Block blockOfFoot = p.getLocation().getBlock();
        if (!disallowedJumpers.contains(p) && p.getLocation().getBlock().getRelative(0, -1, 0).getType().equals(Material.SPONGE))
        {
            disallowedJumpers.add(p);
            Vector launchPower = computeLaunchPower(blockOfFoot);
            float blocksPerSponge = 8;
            float launchPowerY = (float)computeVyInitial(launchPower.getY()*blocksPerSponge);
            float curY = (float)launchPower.getY();
            launchPower.multiply(launchPowerY/curY);
            setPlayerVelocity(p, launchPower);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run()
                {
                    if (disallowedJumpers.contains(p))
                    {
                        disallowedJumpers.remove(p);
                    }
                }
            }, TICK_COOLDOWN);
        }
    }


    public static void setPlayerVelocity(final Player p, final Vector velocity)
    {
        float length = (float)velocity.length();
        if (length > 3.92)
        {
            //Bukkit.broadcastMessage(ChatColor.RED + ""  + velocity);
            if (!kbTasks.containsKey(p))
            {
                kbTasks.put(p, new ArrayList<Integer>());
            }
            final int delay = 2;
            final int iterations = (int)((velocity.length()/3.92f)*3.2f);
            //Bukkit.broadcastMessage("" + iterations);
            final Vector sampleV = velocity.multiply(3.92f/length);
            //Bukkit.broadcastMessage(""  + sampleV);

            int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                int i = 0;
                public void run()
                {
                    if (i < iterations)
                    {
                        Bukkit.getScheduler().callSyncMethod(plugin, new Callable()
                        {
                            public String call()
                            {
                                //Bukkit.getPlayer("HappyMan").sendMessage("knocking " + damagedPlayer.getName() + " back");
                                p.setVelocity(sampleV);
                                return "";
                            }
                        });
                        i++;
                    }
                }
            }, 0, delay);

            SmashManager.getPlugin().cancelTaskAfterDelay(task, iterations*delay);
            kbTasks.get(p).add(task);
        }
        else
        {
            p.setVelocity(velocity);
        }
    }

    private Vector computeLaunchPower(Block blockOfFoot)
    {
        return computeLaunchPower(new Vector(0, 0, 0), blockOfFoot.getRelative(0, -1, 0), blockOfFoot.getLocation().toVector(), (new ArrayList<Block>()));
    }

    private Vector computeLaunchPower(Vector parentRelToCenter, Block childBlock, Vector centerBlockAbsoluteLocation, ArrayList<Block> spongeList)
    {
        Vector launchToAdd = new Vector(0, 0, 0);
        Vector childRelToCenter = childBlock.getLocation().toVector().subtract(centerBlockAbsoluteLocation);
        if (childRelToCenter.length() > parentRelToCenter.length() && childBlock.getType().equals(Material.SPONGE) && !spongeList.contains(childBlock))
        {
            spongeList.add(childBlock);
            Vector parentCopy = new Vector(parentRelToCenter.getX(), parentRelToCenter.getY(), parentRelToCenter.getZ());
            launchToAdd.add(parentCopy.subtract(childRelToCenter));

            launchToAdd.add(computeLaunchPower(childRelToCenter, childBlock.getRelative(0, 0, -1), centerBlockAbsoluteLocation, spongeList));
            launchToAdd.add(computeLaunchPower(childRelToCenter, childBlock.getRelative(0, 0, 1), centerBlockAbsoluteLocation, spongeList));
            launchToAdd.add(computeLaunchPower(childRelToCenter, childBlock.getRelative(0, -1, 0), centerBlockAbsoluteLocation, spongeList));
            launchToAdd.add(computeLaunchPower(childRelToCenter, childBlock.getRelative(0, 1, 0), centerBlockAbsoluteLocation, spongeList));
            launchToAdd.add(computeLaunchPower(childRelToCenter, childBlock.getRelative(-1, 0, 0), centerBlockAbsoluteLocation, spongeList));
            launchToAdd.add(computeLaunchPower(childRelToCenter, childBlock.getRelative(1, 0, 0), centerBlockAbsoluteLocation, spongeList));
        }

        return launchToAdd;
    }
}
