package me.happyman.utils;

import me.happyman.Plugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SmashManager
{
    private static final HashMap<String, HitData> lastHitters = new HashMap<String, HitData>();
    private static final float ANGLE_THAT_COUNTS_AS_FACING = 20*(float)Math.PI / 180;

    public static void setLastHitter(String p, String hitterName, String weaponName)
    {
        if (!p.equals(hitterName))
        {
            while (weaponName.length() > 0 && weaponName.charAt(0) == ' ')
            {
                weaponName = weaponName.substring(1, weaponName.length());
            }
            lastHitters.put(p, new HitData(hitterName, weaponName, Plugin.getMillisecond()));
        }
    }

    public static LivingEntity getNearestEntityExcept(Entity e, Entity entityNotToTarget, boolean allowMobs)
    {
        return getNearestEntityExcept(e, entityNotToTarget, false, allowMobs);
    }

    public static LivingEntity getNearestEntityExcept(Entity e, Entity entityNotToTarget, boolean angle, boolean allowMobs)
    {
        return getNearestEntityExcept(e, entityNotToTarget, angle, ANGLE_THAT_COUNTS_AS_FACING, allowMobs);
    }

    public static LivingEntity getNearestEntityExcept(Entity e, Entity entityNotToTarget, boolean angle, float minRadians, boolean allowMobs)
    {
        LivingEntity closestEntity = null;
        float closestAngle = (float)Math.PI;
        List<LivingEntity> entities;
        if (allowMobs)
        {
            entities = e.getWorld().getLivingEntities();
        }
        else
        {
            entities = new ArrayList<LivingEntity>(e.getWorld().getPlayers());
        }
        for (LivingEntity entity : entities)
        {
            //if ((closestPlayer == null || p.getLocation().distance(player.getLocation()) < p.getLocation().distance(closestPlayer.getLocation()))
            //        && p.getLocation().distance(player.getLocation()) > 10)
            if (!entity.equals(entityNotToTarget))
            {
                if (angle)
                {
                    float angleFacing = getAngle(getUnitDirection(e.getLocation(), entity.getLocation()), e.getLocation().getDirection());
                    if (angleFacing < minRadians && angleFacing < closestAngle)
                    {
                        closestEntity = entity;
                    }
                }
                else if (closestEntity == null || entity.getLocation().distance(e.getLocation()) < closestEntity.getLocation().distance(e.getLocation()))
                {
                    closestEntity = entity;
                }
            }
        }
        return closestEntity;
    }

    public static float getAngle(Vector v1, Vector v2)
    {
        float x1 = (float)v1.getX();
        float y1 = (float)v1.getY();
        float z1 = (float)v1.getZ();
        float x2 = (float)v2.getX();
        float y2 = (float)v2.getY();
        float z2 = (float)v2.getZ();
        return (float)Math.acos((x1 * x2 + y1 * y2 + z1 * z2)/(getMagnitude(x1, y1, z1) * getMagnitude(x2, y2, z2)));
    }

    public static HitData getLastHitter(String p)
    {
        if (lastHitters.containsKey(p))
        {
            return lastHitters.get(p);
        }
        return null;
    }

    public static HitData getLastHitter(Player p)
    {
        return getLastHitter(p.getName());
    }


    public static String getLastHitterName(String p)
    {
        if (lastHitters.containsKey(p))
        {
            return lastHitters.get(p).killerName;
        }
        return null;
    }

    public static String getLastHitterWeapon(String p)
    {
        if (lastHitters.containsKey(p))
        {
            return lastHitters.get(p).weaponName;
        }
        return null;
    }

    public static boolean hasLastHitter(String p)
    {
        if (lastHitters.containsKey(p))
        {
            if (Plugin.getMillisecond() - lastHitters.get(p).millisecond <= 30000)
            {
                return true;
            }
            else
            {
                lastHitters.remove(p);
            }
        }
        return false;
    }

    public static Player getNearestPlayerToPlayer(Player p)
    {
        return (Player)getNearestEntityExcept(p, p, false);
    }

    public static Player getNearestPlayerToPlayer(Player p, boolean angle)
    {
        return (Player)getNearestEntityExcept(p, p, angle, ANGLE_THAT_COUNTS_AS_FACING, false);
    }

    public static Player getNearestPlayerToPlayer(Player p, boolean angle, float maxRadians)
    {
        return (Player)getNearestEntityExcept(p, p, angle, maxRadians, false);
    }

    public static void forgetLastHitter(Player p)
    {
        if (lastHitters.containsKey(p.getName()))
        {
            lastHitters.remove(p.getName());
        }
    }

    public static String capitalize(String s)
    {
        if (ChatColor.stripColor(s.toLowerCase()).contains("hammer"))
        {
            return "HAMMER";
        }
        return capitalize(s);
    }

    public static double getMagnitude(Vector v)
    {
        return getMagnitude(v.getX(), v.getY(), v.getZ());
    }

    public static double getMagnitude(double x, double y, double z)
    {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    /*public static Location getActualLocation(Entity e)
    {
        Location l = e.getLocation();
        Vector dir = getVectorOfYaw(e);
        return new Location(l.getWorld(), l.getX() - dir.getX() * 0.5 - dir.getZ()*0.5,
                                             l.getY(),
                                          l.getZ() - dir.getZ() * 0.5 + dir.getX()*0.5);
    }*/

    //Gets where a projectile should spawn so as not to hit the player
    public static Location getSafeProjLaunchLocation(Player p)
    {
        Location l = p.getEyeLocation();
        Vector dir = l.getDirection();
        if (SmashManager.getAngle(l.getDirection(), SmashEntityTracker.getSpeedVector(p)) < 90 && !SmashEntityTracker.isAfk(p))
        {
            l.setX(l.getX() + dir.getX()*(SmashEntityTracker.getSpeedVector(p).getX()*3 + 1.7F)); //- dir.getZ()*0.4);
            l.setY(l.getY() + dir.getY()*(SmashEntityTracker.getSpeedVector(p).getY()*3 + 2.3F)); //+ 0              );
            l.setZ(l.getZ() + dir.getZ()*(SmashEntityTracker.getSpeedVector(p).getZ()*3 + 1.7F));////////////////; //+ dir.getX()*0.4);
        }
        else
        {
            l.setX(l.getX() + dir.getX()*(3F)); //- dir.getZ()*0.4);
            l.setY(l.getY() + dir.getY()*(4F)); //+ 0              );
            l.setZ(l.getZ() + dir.getZ()*(3F));////////////////; //+ dir.getX()*0.4);
        }
        return l;
    }

    public static double getMagnitude(double x, double y)
    {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    public static void scaleToUnitVector(Vector v)
    {
        double currentX = v.getX();
        double currentY = v.getY();
        double currentZ = v.getZ();
        double currentMag = getMagnitude(currentX, currentY, currentZ);
        v.setX(currentX/currentMag);
        v.setY(currentY/currentMag);
        v.setZ(currentZ/currentMag);
    }

    public static Vector getUnitDirection(Location l1, Location l2)
    {
        double deltaX = l2.getX() - l1.getX();
        double deltaY = l2.getY() - l1.getY();
        double deltaZ = l2.getZ() - l1.getZ();
        double distance = getMagnitude(deltaX, deltaY, deltaZ);
        deltaX /= distance;
        deltaY /= distance;
        deltaZ /= distance;
        return new Vector(deltaX, deltaY, deltaZ);
    }

    public static void preventJumping(Player p)
    {
        PotionEffect jumpEffect = new PotionEffect(PotionEffectType.JUMP, 1000000, 250);
        p.addPotionEffect(jumpEffect);
    }

    public static void resetJumpBoost(Player p)
    {
        p.removePotionEffect(PotionEffectType.JUMP);
    }



//    private Runnable getCloseRunnable(final InventoryCloseEvent e)
//    {
//        return new Runnable()
//        {
//            public void run()
//            {
//                Player p = (Player)e.getAttacker();
//                if (!goodPlayers.contains(p))
//                {
//                    /*
//                    if (p.getInventory().contains(getTourneySword(p)))
//                    {
//                        openSmashGui(p);
//                    }
//                    else if (p.getInventory().contains(getFreeSword()));*/
//                }
//            }
//        };
//   }
}
