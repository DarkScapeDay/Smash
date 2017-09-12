package me.happyman.utils;

import me.happyman.Plugin;
import me.happyman.SpecialItems.SpecialItemTypes.SpeedChanger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;

public class SmashEntityTracker implements Listener
{
    public static final int ARROW_TRACKING_TIME = 40;
//    private static final HashMap<World, HashMap<Entity, Player>> culprits = new HashMap<World, HashMap<Entity, Player>>();
//    private static final HashMap<World, HashMap<Entity, String>> weaponNames = new HashMap<World, HashMap<Entity, String>>();
//    private static final HashMap<World, HashMap<Entity, Integer>> powerLevels = new HashMap<World, HashMap<Entity, Integer>>();
    private static final HashMap<Player, Float> speedFactors = new HashMap<Player, Float>();

    private static final HashMap<Player, List<SpeedChanger>> speedTasks = new HashMap<Player, List<SpeedChanger>>();;
    private static final HashMap<Player, Float> playerSpeeds = new HashMap<Player, Float>();
    private static final HashMap<Player, Vector> playerMovementVectors = new HashMap<Player, Vector>();
    private static final List<Player> crouchingPlayers = new ArrayList<Player>();
//    private static final HashMap<World, HashMap<Entity, Player>> entityOwners = new HashMap<World, HashMap<Entity, Player>>(); //The owners of entities in the various worlds who shouldn't be damaged by those entities
    private static final Vector zero = new Vector().zero();
    private static final HashMap<Player, Long> timeOfLastMove = new HashMap<Player, Long>();
    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED = 0.075F; //The fly speed for before and after Smash games start

    public SmashEntityTracker()
    {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    //**************


    public static void resetSpeedAlteredPlayer(Player p)
    {
        SmashEntityTracker.resetSpeedFactor(p);
        if (speedTasks.containsKey(p))
        {
            for (SpeedChanger item : speedTasks.get(p))
            {
                item.performResetAction(p);
            }
            speedTasks.remove(p);
        }
    }

    public static float getSpeedFactor(Player p)
    {
        Float speedFactor = speedFactors.get(p);
        return speedFactor == null ? 1F : speedFactor;
    }

    public static void forgetSpeedFactors(World w)
    {
        for (Player p : w.getPlayers())
        {
            resetSpeedFactor(p);
        }
//        if (culprits.containsKey(w))
//        {
//            culprits.remove(w);
//        }
//        if (weaponNames.containsKey(w))
//        {
//            weaponNames.remove(w);
//        }
//        if (powerLevels.containsKey(w))
//        {
//            powerLevels.remove(w);
//        }
//        if (entityOwners.containsKey(w))
//        {
//            entityOwners.remove(w);
//        }
    }

    //True if speed factor changed
    @CheckReturnValue
    public static boolean setSpeedFactor(Player p, float factor)
    {
        try
        {
            float walkFactor = factor*DEFAULT_WALK_SPEED;
            float flyFactor = factor*DEFAULT_FLY_SPEED;
            if (flyFactor <= 1F && flyFactor >= 0 && walkFactor <= 1F && walkFactor >= 0)
            {
                p.setWalkSpeed(walkFactor);
                if (p.isFlying())
                {
                    p.setFlySpeed(flyFactor);
                }
                speedFactors.put(p, factor);
                return true;
            }
        }
        catch (IllegalArgumentException e)
        {
            sendErrorMessage("Error! Tried to validate speed factor and failed!");
            //don't do anything; you've reached max
//                final float newFactor;
//                if (DEFAULT_FLY_SPEED < DEFAULT_WALK_SPEED)
//                {
//                    p.setWalkSpeed(1F);
//                    newFactor = 1F/DEFAULT_WALK_SPEED;
//                    p.setFlySpeed(DEFAULT_FLY_SPEED/DEFAULT_WALK_SPEED);
//                }
//                else if (DEFAULT_WALK_SPEED < DEFAULT_FLY_SPEED)
//                {
//                    p.setFlySpeed(1F);
//                    newFactor = 1F/DEFAULT_FLY_SPEED;
//                    p.setWalkSpeed(DEFAULT_WALK_SPEED/DEFAULT_FLY_SPEED);
//                }
//                else
//                {
//                    p.setWalkSpeed(1F);
//                    p.setFlySpeed(1F);
//                    newFactor = 1F/DEFAULT_FLY_SPEED;
//                }
//                speedFactors.put(p, newFactor);
        }
        return false;
    }

    public static boolean multiplySpeedFactor(Player p, float multiplyFactorBy)
    {
        return setSpeedFactor(p, getSpeedFactor(p)*multiplyFactorBy);
    }

    public static void resetSpeedFactor(Player p)
    {
        p.setWalkSpeed(DEFAULT_WALK_SPEED);
        p.setFlySpeed(DEFAULT_FLY_SPEED);
        speedFactors.remove(p);
    }

    @CheckReturnValue
    public static boolean putSpeedFactor(Player p, float factorToMultiply, SpeedChanger item)
    {
        if (setSpeedFactor(p, getSpeedFactor(p)*factorToMultiply))
        {
            if (!speedTasks.containsKey(p))
            {
                speedTasks.put(p, new ArrayList<SpeedChanger>());
            }
            if (!speedTasks.get(p).contains(item))
            {
                speedTasks.get(p).add(item);
            }
            return true;
        }
        return false;
    }

    public static void removeSpeedFactor(Player p, float factorToDivide)
    {
        setSpeedFactor(p, getSpeedFactor(p)/factorToDivide);
        p.setFlySpeed(p.getFlySpeed()/factorToDivide);
    }

    //**************
//
//    /**
//     * Function: setImmuneEntityOwner
//     * Purpose: To make it so that the entity will not damage the player.
//     * @param e - The Entity which the player will not be damaged by
//     * @param p - The player who should not be damaged by the entity
//     */
//    private static void setImmuneEntityOwner(Player p, Entity e)
//    {
//        if (!entityOwners.containsKey(p.getWorld()))
//        {
//            entityOwners.put(p.getWorld(), new HashMap<Entity, Player>());
//        }
//        entityOwners.get(p.getWorld()).put(e, p);
//    }
//
//    /**
//     * Function: hasImmuneEntityOwner
//     * Purpose: To determine whether the entity in question has a player who should be damaged
//     * @param e - The Entity in question
//     * @return - True if the entity shouldn't be damaging a particular player
//     */
//    private static boolean hasImmuneEntityOwner(Entity e)
//    {
//        return entityOwners.containsKey(e.getWorld()) && entityOwners.get(e.getWorld()).containsKey(e);
//    }

    //**************

//    public static void addCulprit(Player p, Monster damager, String weaponName, int powerLevel)
//    {
//        addCulprit(p, damager, weaponName, powerLevel, false);
//    }
//
//    public static void addCulprit(Player p, Entity damager, String weaponName, int powerLevel, boolean makeImmune)
//    {
//        if (makeImmune)
//        {
//            setImmuneEntityOwner(p, damager);
//        }
//        if (!culprits.containsKey(p.getWorld()))
//        {
//            culprits.put(p.getWorld(), new HashMap<Entity, Player>());
//        }
//        if (!weaponNames.containsKey(p.getWorld()))
//        {
//            weaponNames.put(p.getWorld(), new HashMap<Entity, String>());
//        }
//        if (!powerLevels.containsKey(p.getWorld()))
//        {
//            powerLevels.put(p.getWorld(), new HashMap<Entity, Integer>());
//        }
//        culprits.get(p.getWorld()).put(damager, p);
//        weaponNames.get(p.getWorld()).put(damager, weaponName);
//        powerLevels.get(p.getWorld()).put(damager, powerLevel);
////    }
//    public static class Culprit extends WorldType.RangeAttackSource
//    {
//        private final int powerLevel;
//        private final boolean immuneToOwn;
//
//        public Culprit(SpecialItem item, LivingEntity user, int powerLevel, boolean makeImmune)
//        {
//            super(item, user);
//            this.powerLevel = powerLevel;
//            this.immuneToOwn = makeImmune;
//        }
//
//        public boolean isImmuneToOwn()
//        {
//            return immuneToOwn;
//        }
//
//        public int powerLevel()
//        {
//            return powerLevel;
//        }
//    }
//
//    public static String getWeaponName(Entity damager)
//    {
//        sendErrorMessage("Warning! called getWeaponName!");
//        WorldType.RangeAttackSource source = WorldManager.getAttackSource(damager);
//        if (source != null)
//        {
//            return source.getSpecialItem().getItemStack().getItemMeta().getDisplayName();
//        }
//        sendErrorMessage("Attempted to get the weaponName for " + damager.toString() + ", but there was none!");
//        return damager.getType().name().toLowerCase();
//    }
//
//    public static String getCulpritName(Entity damager)
//    {
//        sendErrorMessage("Warning! called getCulpritName!");
//        WorldType.RangeAttackSource source = WorldManager.getAttackSource(damager);
//        if (source != null)
//        {
//            return source.getAttacker().getName();
//        }
////        if (culprits.containsKey(damager.getWorld()) && culprits.get(damager.getWorld()).containsKey(damager))
////        {
////            return culprits.get(damager.getWorld()).get(damager).getName();
////        }
//        sendErrorMessage("Error! Attempted to get the culprit for " + damager.toString() + ", but there was none!");
//        return null;
//    }
//
////    public static int getCulpritDamage(Entity damager)
////    {
////        if (powerLevels.containsKey(damager.getWorld()) &&  powerLevels.get(damager.getWorld()).containsKey(damager))
////        {
////            return powerLevels.get(damager.getWorld()).get(damager);
////        }
////        sendErrorMessage("Error! Attempted to get the power level for " + damager.toString() + ", but there was none!");
////        return -1;
////    }
//
//    public static boolean hasCulprit(Entity damager)
//    {
//        return WorldManager.getAttackSource(damager) != null;//culprits.containsKey(damager.getWorld()) && culprits.get(damager.getWorld()).containsKey(damager);
//    }

    //***********************

    public static boolean isAfk(Player p)
    {
        Long timeSinceLastMove = timeOfLastMove.get(p);
        if (timeSinceLastMove == null || timeSinceLastMove + 10000 > Plugin.getMillisecond())
        {
            if (timeSinceLastMove != null)
            {
                forgetSpeed(p);
            }
            return false;
        }
        return p.getVelocity().lengthSquared() < 0.01;
    }

    public static float getSpeed(Player p)
    {
        if (!playerSpeeds.containsKey(p) || isAfk(p))
        {
            forgetSpeed(p);
        }
        if (!playerSpeeds.containsKey(p))
        {
            return 0;
        }
        return playerSpeeds.get(p);
    }

    public static void setSpeedToZero(Player p)
    {
        //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity from setting to zero");
        p.setVelocity(new Vector().zero());
    }

    public static void forgetSpeed(Player p)
    {
        if (timeOfLastMove.containsKey(p))
        {
            timeOfLastMove.remove(p);
        }
        if (playerSpeeds.containsKey(p))
        {
            playerSpeeds.remove(p);
        }
        if (playerMovementVectors.containsKey(p))
        {
            playerMovementVectors.remove(p);
        }
    }

    public static Vector getSpeedVector(Player p)
    {
        if (!playerMovementVectors.containsKey(p) || isAfk(p))
        {
            forgetSpeed(p);
        }
        if (!playerMovementVectors.containsKey(p))
        {
            return zero;
        }
        return playerMovementVectors.get(p).clone();
    }

    public static boolean isCrouching(Player p)
    {
        return crouchingPlayers.contains(p);
    }

    public static void setCrouching(Player p, boolean crouching)
    {
        if (crouching && !crouchingPlayers.contains(p))
        {
            crouchingPlayers.add(p);
        }
        else if (!crouching && crouchingPlayers.contains(p))
        {
            crouchingPlayers.remove(p);
        }
    }

    public static void setSpeedToCurrentSpeed(Player p)
    {
        p.setWalkSpeed(getSpeedFactor(p)*0.2F);
        SmashManager.resetJumpBoost(p);
    }

    public static Block getBlockBelowEntity(Entity e)
    {
        return getBlockBelowLocation(e.getLocation());
    }

    public static Block getBlockBelowLocation(Location l)
    {
        if (!l.getWorld().getHighestBlockAt(l).getRelative(0, -1, 0).getType().equals(Material.AIR))
        {
            for (float y = (float)l.getY(); y >= 0; y--)
            {
                Block b = l.getWorld().getBlockAt(l.getBlockX(), Math.round(y), l.getBlockZ());
                if (!b.getType().equals(Material.AIR))
                {
                    return b;
                }
            }
        }
        return l.getBlock();

    }

    public static float getBlockDistanceBelowEntity(Entity e, int assumption)
    {
        Block b = SmashEntityTracker.getBlockBelowEntity(e);
        if (b != null && !b.getType().equals(Material.AIR))
        {
            return (float)(e.getLocation().getY() - b.getY());
        }
        return assumption;
    }

    @EventHandler
    public void crouchEvent(PlayerToggleSneakEvent event)
    {
        setCrouching(event.getPlayer(), event.isSneaking());
    }

    @EventHandler
    public void moveEvent(final PlayerMoveEvent e)
    {
        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                Player p = e.getPlayer();
                timeOfLastMove.put(p, Plugin.getMillisecond());
                playerSpeeds.put(p, (float)e.getFrom().distance(e.getTo()));
                playerMovementVectors.put(p, new Vector(
                        (float)e.getTo().getX() - (float)e.getFrom().getX(),
                        (float)e.getTo().getY() - (float)e.getFrom().getY(),
                        (float)e.getTo().getZ() - (float)e.getFrom().getZ()
                ));
                return "";
            }
        });
    }

    public static void performActionWhenSafe(final Player p, final AfkAction action, final int secondsMustBeStill, final String youMustWaitMessage, final String successMessage, final String failureMessage)
    {
        p.sendMessage(youMustWaitMessage);
        final int timeSample = 3;
        final int ticksMustBeStill = secondsMustBeStill*20;
        final int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
        {
            int tick = -timeSample;
            boolean dontDoIt = false;
            int second = 0;
            int secondsLeft = secondsMustBeStill;

            @Override
            public void run()
            {
                if (!dontDoIt)
                {
                    if (!isAfk(p))
                    {
                        dontDoIt = true;
                        p.sendMessage(failureMessage);
                    }
                    else
                    {
                        tick += timeSample;
                        if (tick >= ticksMustBeStill)
                        {
                            dontDoIt = true;
                            p.sendMessage(successMessage);
                            action.performAction();
                        }
                        else if (tick > second*20)
                        {
                            second++;
                            p.sendMessage("" + secondsLeft);
                            secondsLeft--;
                        }
                    }
                }
            }
        }, 0, timeSample);
        cancelTaskAfterDelay(task, secondsMustBeStill*20);
    }

    public abstract static class AfkAction
    {
        public abstract void performAction();
    }
}
