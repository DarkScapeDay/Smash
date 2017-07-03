package me.happyman.commands;

import me.happyman.Listeners.HitData;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.Listeners.TabCompletion;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.worlds.SmashScoreboardManager;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import me.happyman.source;
import org.bukkit.entity.Player;

import java.util.*;

import static me.happyman.worlds.SmashScoreboardManager.updateDamage;

public class SmashManager
{
    private static source plugin;

    private static HashMap<Player, Float> actualDamages;
    private static HashMap<String, HitData> lastHitters;
    private static final float ANGLE_THAT_COUNTS_AS_FACING = 20*(float)Math.PI / 180;


    public SmashManager(source plugIn)
    {
        plugin = plugIn;
        actualDamages = new HashMap<Player, Float>();
        lastHitters = new HashMap<String, HitData>();

        new SmashItemManager(this, plugin);
        new SmashWorldManager();

        new TabCompletion();
        //sendPlayerToWorld(Bukkit.getPlayer("HappyMan"), Bukkit.getWorlds().get(0));
        //Bukkit.unloadWorld("Smash-f1", false);
    }

    public static source getPlugin()
    {
        return plugin;
    }

    public static void setLastHitter(String p, String hitterName, String weaponName, boolean wasProjectile)
    {
        if (!wasProjectile || !p.equals(hitterName))
        {
            while (weaponName.length() > 0 && weaponName.charAt(0) == ' ')
            {
                weaponName = weaponName.substring(1, weaponName.length());
            }
            lastHitters.put(p, new HitData(hitterName, weaponName, getPlugin().getMillisecond()));
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

    public static Player getPlayerWithHighScore(Player targetter)
    {
        Player highestPlayer = null;
        for (Player p : targetter.getWorld().getPlayers())
        {
            if (!p.equals(targetter) && (highestPlayer == null || (SmashScoreboardManager.getPlayerScoreValue(p) != null &&
                    SmashScoreboardManager.getPlayerScoreValue(p) > SmashScoreboardManager.getPlayerScoreValue(highestPlayer))))
            {
                highestPlayer = p;
            }
        }
        return highestPlayer;
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
            if (getPlugin().getMillisecond() - lastHitters.get(p).millisecond <= 30000)
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
        return getPlugin().capitalize(s);
    }

    /**
     * Negative values can be input to remove damage.
     */
     public static void addDamage(Player p, float damage, boolean adjust)
    {
        if (SmashWorldManager.gameHasStarted(p.getWorld()) && !SmashWorldManager.gameHasEnded(p.getWorld()))// || !adjust)
        {
            if (p.getLevel() + damage <= 0)
            {
                clearDamage(p);
            }
            else
            {
                if (adjust)
                {
                    damage *= SmashAttackListener.DAMAGE_GAIN_FACTOR;
                }
                actualDamages.put(p, getDamage(p) + damage);
                p.setLevel(Math.round(actualDamages.get(p)));
                updateDamage(p);
            }
        }
    }

    public static float getDamage(Player p)
    {
        if (!actualDamages.containsKey(p))
        {
            actualDamages.put(p, (float)0);
        }
        return actualDamages.get(p);
    }

    public static void clearDamage(Player p)
    {
        actualDamages.put(p, (float)0);
        p.setLevel(0);
        updateDamage(p);
    }

    public static double getMagnitude(Vector v)
    {
        return getMagnitude(v.getX(), v.getY(), v.getZ());
    }

    public static double getMagnitude(double x, double y, double z)
    {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    public static Vector getVectorOfYaw(Entity e)
    {
        return getVectorOfYaw(e, 1F);
    }

    public static Vector getVectorOfYaw(Entity e, float vMod)
    {
        final double yaw = e.getLocation().getYaw();
        return new Vector(-Math.sin(yaw*Math.PI/180)*vMod, 0, Math.cos(yaw*Math.PI/180)*vMod);
    }

    /*public static Location getActualLocation(Entity e)
    {
        Location l = e.getLocation();
        Vector dir = getVectorOfYaw(e);
        return new Location(l.getWorld(), l.getX() - dir.getX() * 0.5 - dir.getZ()*0.5,
                                             l.getY(),
                                          l.getZ() - dir.getZ() * 0.5 + dir.getX()*0.5);
    }*/

    public static int getSign(float num)
    {
        if (num < 0)
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    //Gets where a projectile should spawn so as not to hit the player
    public static Location getSafeProjLaunchLocation(Player p)
    {
        Location l = p.getEyeLocation();
        Vector dir = l.getDirection();
        if (SmashManager.getAngle(l.getDirection(), SmashEntityTracker.getSpeedVector(p)) < 90 && !SmashEntityTracker.isHoldingStill(p))
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

    public static Location getAbsFromRelLocFRU(Location base, float forwardOffset, float rightOffset, float upOffset, boolean useFlatDir)
    {
        return getAbsFromRelLocFRU(base, new Vector(forwardOffset, upOffset, -rightOffset), useFlatDir);
    }

    public static Location getRelFromAbsLoc(Location base, Vector offset, boolean useFlatDir)
    {
        float theta = (base.getYaw() + 90)*((float)Math.PI/180);
        float gamma;
        if (useFlatDir)
        {
            gamma = 0;
        }
        else
        {
            gamma = base.getPitch()*(float)Math.PI/180;
        }

        return new Location(base.getWorld(),
                base.getX() + Math.cos(gamma)*Math.cos(theta)*offset.getX() - Math.sin(gamma)*offset.getY() + Math.cos(gamma)*Math.sin(theta)*offset.getZ(),
                base.getY() + Math.sin(gamma)*Math.cos(theta)*offset.getX() + Math.cos(gamma)*offset.getY() + Math.sin(gamma)*Math.sin(theta)*offset.getZ(),
                base.getZ() + Math.sin(theta)*offset.getX() + Math.cos(theta)*offset.getZ());
    }

    public static Vector getAbsOffsetFromRelLocFUR(Location base, Vector offset, boolean useFlatDir)
    {
        float theta = (base.getYaw() + 90)*((float)Math.PI/180);
        float gamma;
        if (useFlatDir)
        {
            gamma = 0;
        }
        else
        {
            gamma = base.getPitch()*(float)Math.PI/180;
        }
        return new Vector(
                Math.cos(gamma)*Math.cos(theta)*offset.getX() + Math.sin(gamma)*Math.cos(theta)*offset.getY() + Math.sin(theta)*offset.getZ(),
                -Math.sin(gamma)*offset.getX() + Math.cos(gamma)*offset.getY(),
                Math.cos(gamma)*Math.sin(theta)*offset.getX() + Math.sin(gamma)*Math.sin(theta)*offset.getY() - Math.cos(theta)*offset.getZ());
    }

    //Yay for linear algebra
    public static Location getAbsFromRelLocFRU(Location base, Vector offset, boolean useFlatDir)
    {
        Vector absOffset = getAbsOffsetFromRelLocFUR(base, offset, useFlatDir);

        return new Location(base.getWorld(),
                base.getX() + absOffset.getX(),
                base.getY() + absOffset.getY(),
                base.getZ() + absOffset.getZ());
        /*
        float theta = (float)((-base.getYaw() - 90)*Math.PI/180);
        if (useFlatDir)
        {
            /*Vector baseDir = base.getDirection().clone().setY(0);
            float theta = getAngle(baseDir, new Vector(1, 0, 0));
            if (baseDir.getZ() > 0)
            {
                theta = -theta;
            }
            Vector absLocation = new Vector(
                    base.getX() + Math.cos(theta)*offset.getX() - Math.sin(theta)*offset.getZ(),
                    base.getY() + offset.getY(),
                    base.getZ() - Math.sin(theta)*offset.getX() - Math.cos(theta)*offset.getZ());
            return new Location(base.getWorld(), absLocation.getX(), absLocation.getY(), absLocation.getZ());
        }
        else
        {
            float gamma = (float)((base.getPitch() + 90)*Math.PI/180);

            Vector absLocation = new Vector(
                    base.getX() + Math.sin(gamma)*Math.cos(theta)*offset.getX() - Math.cos(gamma)*Math.cos(theta)*offset.getY() - Math.sin(theta)*offset.getZ(),
                    base.getY() + Math.cos(gamma)*offset.getX() + Math.sin(gamma)*offset.getY(),
                    base.getZ() - Math.sin(gamma)*Math.sin(theta)*offset.getX() + Math.cos(gamma)*Math.sin(theta)*offset.getY() - Math.cos(theta)*offset.getZ());
            return new Location(base.getWorld(), absLocation.getX(), absLocation.getY(), absLocation.getZ());
        }
        */
    }

    public static void playBasicParticle(Location l, ParticleEffect.OrdinaryColor color)
    {
        playBasicParticle(l, color, false);
    }

    public static void playBasicParticle(Location l, ParticleEffect.OrdinaryColor color, boolean adjustY)
    {
        if (adjustY)
        {
            l.setY(l.getY() - 0.5F);
        }
        ParticleEffect.REDSTONE.display(color, l, l.getWorld().getPlayers());
    }

    public static void playRelativeParticleParallelogramFURVectors(Location l, ParticleEffect.OrdinaryColor color, Vector relativeCorner1FUR, Vector relativeCorner2FUR, Vector relativeCorner3FUR, float particlesPerBlock, boolean symmetrically)
    {
        for (Vector v : getParticleParallelogramFRUVectors(relativeCorner1FUR, relativeCorner2FUR, relativeCorner3FUR, particlesPerBlock, symmetrically))
        {
            SmashManager.playBasicParticle(SmashManager.getAbsFromRelLocFRU(l, (float)v.getX(), (float)v.getY(), (float)v.getZ(), true), color, false);
        }
    }

    public static List<Vector> getParticleParallelogramFRUVectors(Vector relativeCorner1FUR, Vector relativeCorner2FUR, Vector relativeCorner3FUR, float distanceBetweenParticles, boolean symmetrically)
    {
        List<Vector> results = new ArrayList<Vector>();

        for (float
             i = (float)relativeCorner1FUR.getX(),
             j = (float)relativeCorner1FUR.getZ(),
             k = (float)relativeCorner1FUR.getY(),
             rel12x = (float)(relativeCorner2FUR.getX() - i),
             rel12y = (float)(relativeCorner2FUR.getZ() - j),
             rel12z = (float)(relativeCorner2FUR.getY() - k),
             rel12mag = (float)Math.sqrt(Math.pow(rel12x, 2) + Math.pow(rel12y, 2) + Math.pow(rel12z, 2)),
             rel13x = (float)(relativeCorner3FUR.getX() - relativeCorner1FUR.getX()),
             rel13y = (float)(relativeCorner3FUR.getZ() - relativeCorner1FUR.getZ()),
             rel13z = (float)(relativeCorner3FUR.getY() - relativeCorner1FUR.getY()),
             rel13mag = (float)Math.sqrt(Math.pow(rel13x, 2) + Math.pow(rel13y, 2) + Math.pow(rel13z, 2));

             Math.sqrt(Math.pow(i - relativeCorner1FUR.getX(), 2) + Math.pow(j - relativeCorner1FUR.getZ(), 2) + Math.pow(k - relativeCorner1FUR.getY(), 2)) <= rel12mag;

             i += distanceBetweenParticles*rel12x/rel12mag,
             j += distanceBetweenParticles*rel12y/rel12mag,
             k += distanceBetweenParticles*rel12z/rel12mag)
        {
            for (float
                 m = i,
                 n = j,
                 o = k;

                 Math.sqrt(Math.pow(m - i, 2) + Math.pow(n - j, 2) + Math.pow(o - k, 2)) <= rel13mag;

                 m += distanceBetweenParticles*rel13x/rel13mag,
                 n += distanceBetweenParticles*rel13y/rel13mag,
                 o += distanceBetweenParticles*rel13z/rel13mag)
            {

                results.add(new Vector(m, n, o));
                if (symmetrically)
                {
                    results.add(new Vector(m, -n, o));
                }
            }
        }
        return results;
    }

    public static List<Vector> getParticleTriangleFRUVectors(Vector relativeCorner1FUR, Vector relativeCorner2FUR, Vector relativeCorner3FUR, float distanceBetweenParticles, boolean symmetrically)
    {
        List<Vector> results = new ArrayList<Vector>();

        for (float
            i = (float)relativeCorner1FUR.getX(),
            j = (float)relativeCorner1FUR.getZ(),
            k = (float)relativeCorner1FUR.getY(),
            rel12x = (float)(relativeCorner2FUR.getX() - i),
            rel12y = (float)(relativeCorner2FUR.getZ() - j),
            rel12z = (float)(relativeCorner2FUR.getY() - k),
            rel12mag = (float)Math.sqrt(Math.pow(rel12x, 2) + Math.pow(rel12y, 2) + Math.pow(rel12z, 2)),
            rel13x = (float)(relativeCorner3FUR.getX() - relativeCorner1FUR.getX()),
            rel13y = (float)(relativeCorner3FUR.getZ() - relativeCorner1FUR.getZ()),
            rel13z = (float)(relativeCorner3FUR.getY() - relativeCorner1FUR.getY()),
            rel13mag = (float)Math.sqrt(Math.pow(rel13x, 2) + Math.pow(rel13y, 2) + Math.pow(rel13z, 2));

            Math.sqrt(Math.pow(i - relativeCorner1FUR.getX(), 2) + Math.pow(j - relativeCorner1FUR.getZ(), 2) + Math.pow(k - relativeCorner1FUR.getY(), 2)) <= rel12mag;


            i += distanceBetweenParticles*rel12x/rel12mag,
            j += distanceBetweenParticles*rel12y/rel12mag,
            k += distanceBetweenParticles*rel12z/rel12mag)
        {
            for (float
                m = i,
                n = j,
                o = k,
                travelPercent = (float)Math.sqrt(Math.pow(i - relativeCorner1FUR.getX(), 2) + Math.pow(j - relativeCorner1FUR.getZ(), 2) + Math.pow(k - relativeCorner1FUR.getY(), 2))/rel12mag;

                Math.sqrt(Math.pow(m - i, 2) + Math.pow(n - j, 2) + Math.pow(o - k, 2)) <= rel13mag * (1-travelPercent);

                m += distanceBetweenParticles*rel13x/rel13mag,
                n += distanceBetweenParticles*rel13y/rel13mag,
                o += distanceBetweenParticles*rel13z/rel13mag)
            {
                results.add(new Vector(m, n, o));
                if (symmetrically)
                {
                    results.add(new Vector(m, -n, o));
                }
            }
        }
        return results;
    }

    public static void playBasicParticle(Entity e, ParticleEffect.OrdinaryColor color, boolean adjustY)
    {
        if (!e.isDead())
        {
            Location l = e.getLocation();
            playBasicParticle(l, color, adjustY);
        }
    }

    public static ParticleEffect.OrdinaryColor getParticleColor(int red, int green, int blue)
    {
        return new ParticleEffect.OrdinaryColor(red, green, blue);
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
//                Player p = (Player)e.getPlayer();
//                if (!goodPlayers.contains(p))
//                {
//                    /*
//                    if (p.getInventory().contains(getTourneySword(p)))
//                    {
//                        openWorldGui(p);
//                    }
//                    else if (p.getInventory().contains(getFreeSword()));*/
//                }
//            }
//        };
//   }
}
