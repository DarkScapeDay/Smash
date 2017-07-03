package me.happyman.utils;

import me.happyman.ItemTypes.SpeedChanger;
import me.happyman.commands.SmashManager;
import me.happyman.worlds.SmashWorldInteractor;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class SmashEntityTracker implements Listener
{
    public static final int ARROW_TRACKING_TIME = 40;
    private static HashMap<World, HashMap<Entity, Player>> culprits;
    private static HashMap<World, HashMap<Entity, String>> weaponNames;
    private static HashMap<World, HashMap<Entity, Integer>> powerLevels;
    private static HashMap<World, HashMap<Player, Float>> speedFactor;

    private static HashMap<Player, List<SpeedChanger>> speedTasks;
    private static HashMap<Player, Float> playerSpeeds;
    private static HashMap<Player, Vector> playerMovementVectors;
    private static List<Player> crouchingPlayers;
    private static HashMap<World, HashMap<Entity, Player>> entityOwners; //The owners of entities in the various worlds who shouldn't be damaged by those entities
    private static final Vector zero = new Vector().zero();;
    private static HashMap<Player, Long> timeOfLastMove;

    public SmashEntityTracker()
    {
        culprits = new HashMap<World, HashMap<Entity, Player>>();
        weaponNames = new HashMap<World, HashMap<Entity, String>>();
        powerLevels = new HashMap<World, HashMap<Entity, Integer>>();
        speedFactor = new HashMap<World, HashMap<Player, Float>>();

        speedTasks = new HashMap<Player, List<SpeedChanger>>();

        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
        playerSpeeds = new HashMap<Player, Float>();
        timeOfLastMove = new HashMap<Player, Long>();
        playerMovementVectors = new HashMap<Player, Vector>();

        entityOwners = new HashMap<World, HashMap<Entity, Player>>();

        crouchingPlayers = new ArrayList<Player>();
    }

    //***************

    public static void resetSpeedAlteredPlayer(Player p)
    {
        SmashEntityTracker.resetSpeedFactor(p);
        if (speedTasks.containsKey(p))
        {
            for (SpeedChanger item : speedTasks.get(p))
            {
                item.cancelTask(p);
            }
            speedTasks.remove(p);
        }
    }

    public static float getSpeedFactor(Player p)
    {
        if (speedFactor.containsKey(p.getWorld()) && speedFactor.get(p.getWorld()).containsKey(p))
        {
            return speedFactor.get(p.getWorld()).get(p);
        }
        return 1F;
    }

    public static void forgetTransgressions(World w)
    {
        if (speedFactor.containsKey(w))
        {
            for (Player p : speedFactor.get(w).keySet())
            {
                resetSpeedFactor(p);
            }
            speedFactor.remove(w);
        }
        if (culprits.containsKey(w))
        {
            culprits.remove(w);
        }
        if (weaponNames.containsKey(w))
        {
            weaponNames.remove(w);
        }
        if (powerLevels.containsKey(w))
        {
            powerLevels.remove(w);
        }
        if (speedFactor.containsKey(w))
        {
            for (Player p : speedFactor.get(w).keySet())
            {
                resetSpeedFactor(p);
            }
            speedFactor.remove(w);
        }
        if (entityOwners.containsKey(w))
        {
            entityOwners.remove(w);
        }
    }

    //True if speed factor changed
    @CheckReturnValue
    public static boolean setSpeedFactor(Player p, float factor)
    {
        if (factor < 640)
        {
            if (!speedFactor.containsKey(p.getWorld()))
            {
                speedFactor.put(p.getWorld(), new HashMap<Player, Float>());
            }
            speedFactor.get(p.getWorld()).put(p, factor);
            try
            {
                p.setWalkSpeed(factor*0.2F);
            }
            catch (IllegalArgumentException e)
            {
                p.setWalkSpeed(1F);
            }
            if (p.isFlying())
            {
                try
                {
                    p.setFlySpeed(factor* SmashWorldInteractor.DEFAULT_FLY_SPEED);
                }
                catch (IllegalArgumentException e)
                {
                    p.setFlySpeed(1F);
                }
            }
            return true;
        }
        return false;
    }

    public static boolean multiplySpeedFactor(Player p, float multiplyFactorBy)
    {
        return setSpeedFactor(p, getSpeedFactor(p)*multiplyFactorBy);
    }

    public static void resetSpeedFactor(Player p)
    {
        if (!speedFactor.containsKey(p.getWorld()))
        {
            speedFactor.put(p.getWorld(), new HashMap<Player, Float>());
        }
        if (speedFactor.get(p.getWorld()).containsKey(p))
        {
            setSpeedFactor(p, 1F);
        }
        p.setWalkSpeed(0.2F);
        p.setFlySpeed(SmashWorldInteractor.DEFAULT_FLY_SPEED);
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

    /**
     * Function: setImmuneEntityOwner
     * Purpose: To make it so that the entity will not damage the player.
     * @param e - The Entity which the player will not be damaged by
     * @param p - The player who should not be damaged by the entity
     */
    private static void setImmuneEntityOwner(Player p, Entity e)
    {
        if (!entityOwners.containsKey(p.getWorld()))
        {
            entityOwners.put(p.getWorld(), new HashMap<Entity, Player>());
        }
        entityOwners.get(p.getWorld()).put(e, p);
    }

    /**
     * Function: hasImmuneEntityOwner
     * Purpose: To determine whether the entity in question has a player who should be damaged
     * @param e - The Entity in question
     * @return - True if the entity shouldn't be damaging a particular player
     */
    private static boolean hasImmuneEntityOwner(Entity e)
    {
        return entityOwners.containsKey(e.getWorld()) && entityOwners.get(e.getWorld()).containsKey(e);
    }

    /**
     * Function: isImmuneEntityOwner
     * Purpose: To determine if the player shouldn't be damaged by the entity
     * @param p - The player who we want to check for being immune to the entity
     * @param e - The entity we want to check if it should damage the player
     * @return - True if the player shouldn't be damaged by the entity
     */
    public static boolean isImmuneEntityOwner(Player p, Entity e)
    {
        return hasImmuneEntityOwner(e) && entityOwners.get(p.getWorld()).get(e).equals(p);
    }

    //**************

    public static void addCulprit(Player p, Entity damager, String weaponName, int powerLevel)
    {
        addCulprit(p, damager, weaponName, powerLevel, false);
    }

    public static void addCulprit(Player p, Entity damager, String weaponName, int powerLevel, boolean makeImmune)
    {
        if (makeImmune)
        {
            setImmuneEntityOwner(p, damager);
        }
        if (!culprits.containsKey(p.getWorld()))
        {
            culprits.put(p.getWorld(), new HashMap<Entity, Player>());
        }
        if (!weaponNames.containsKey(p.getWorld()))
        {
            weaponNames.put(p.getWorld(), new HashMap<Entity, String>());
        }
        if (!powerLevels.containsKey(p.getWorld()))
        {
            powerLevels.put(p.getWorld(), new HashMap<Entity, Integer>());
        }
        culprits.get(p.getWorld()).put(damager, p);
        weaponNames.get(p.getWorld()).put(damager, weaponName);
        powerLevels.get(p.getWorld()).put(damager, powerLevel);
    }


    public static String getWeaponName(Entity damager)
    {
        if (weaponNames.containsKey(damager.getWorld()) && weaponNames.get(damager.getWorld()).containsKey(damager))
        {
            return weaponNames.get(damager.getWorld()).get(damager);
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Attempted to get the weaponName for " + damager.toString() + ", but there was none!");
        return null;
    }

    public static String getCulpritName(Entity damager)
    {
        if (culprits.containsKey(damager.getWorld()) && culprits.get(damager.getWorld()).containsKey(damager))
        {
            return culprits.get(damager.getWorld()).get(damager).getName();
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Attempted to get the culprit for " + damager.toString() + ", but there was none!");
        return null;
    }

    public static int getCulpritDamage(Entity damager)
    {
        if (powerLevels.containsKey(damager.getWorld()) &&  powerLevels.get(damager.getWorld()).containsKey(damager))
        {
            return powerLevels.get(damager.getWorld()).get(damager);
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Attempted to get the power level for " + damager.toString() + ", but there was none!");
        return -1;
    }

    public static boolean hasCulprit(Entity damager)
    {
        return culprits.containsKey(damager.getWorld()) && culprits.get(damager.getWorld()).containsKey(damager);
    }

    //***********************

    public static boolean isHoldingStill(Player p)
    {
        if (timeOfLastMove.containsKey(p) && (timeOfLastMove.get(p) + 1000 > SmashManager.getPlugin().getMillisecond()))
        {
            forgetSpeed(p);
            return false;
        }
        return p.getVelocity().equals(zero);
    }

    public static float getSpeed(Player p)
    {
        if (!playerSpeeds.containsKey(p) || isHoldingStill(p))
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
        //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity from setting to zero");
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
        if (!playerMovementVectors.containsKey(p) || isHoldingStill(p))
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
    public void crouchEvent(PlayerToggleSneakEvent e)
    {
        setCrouching(e.getPlayer(), e.isSneaking());
    }

    @EventHandler
    public void moveEvent(final PlayerMoveEvent e)
    {
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call() {
                Player p = e.getPlayer();
                timeOfLastMove.put(p, SmashManager.getPlugin().getMillisecond());
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

    @EventHandler
    public void projLaunch(ProjectileLaunchEvent e)
    {
        World w = e.getEntity().getWorld();
        if (e.getEntity().getShooter() instanceof Player && SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w)
                && ((Player)e.getEntity().getShooter()).getItemInHand().hasItemMeta() && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName()) // ((Player)e.getEntity().getShooter()).getItemInHand().hasItemMeta()
        // && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName() && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName()
        // && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName().is
        {
            final Player p = (Player)e.getEntity().getShooter();
            SmashEntityTracker.addCulprit(p, e.getEntity(), p.getItemInHand().getItemMeta().getDisplayName(), 10);
        }
    }

    @EventHandler
    public void projLaunch(ProjectileHitEvent e)
    {
        final Entity proj = e.getEntity();
        if (proj instanceof Arrow)
        {
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call()
                {
                    proj.remove();
                    return "";
                }
            });
        }
    }
}
