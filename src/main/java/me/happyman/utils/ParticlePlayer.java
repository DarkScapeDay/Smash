package me.happyman.utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticlePlayer
{
    private static final Random r = new Random();

    public static Vector getVectorOfYaw(Entity e)
    {
        return getVectorOfYaw(e, 1F);
    }

    public static Vector getVectorOfYaw(Entity e, float vMod)
    {
        final double yaw = e.getLocation().getYaw();
        return new Vector(-Math.sin(yaw*Math.PI/180)*vMod, 0, Math.cos(yaw*Math.PI/180)*vMod);
    }

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

    public static Location getAbsFromRelLocFRU(Location base, float forwardOffset, float rightOffset, float upOffset, boolean useFlatDir)
    {
        return getAbsFromRelLocFRU(base, new Vector(forwardOffset, upOffset, -rightOffset), useFlatDir);
    }

    public static Location getFRUFromAbsLoc(Location base, Vector offset, boolean useFlatDir)
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

    public static void playBasicParticle(Location l, ParticleEffect.OrdinaryColor[] colorsToChooseFrom)
    {
        ParticleEffect.REDSTONE.display(colorsToChooseFrom[r.nextInt(colorsToChooseFrom.length)], l, l.getWorld().getPlayers());
    }

    public static void playRelativeParticleParallelogramFURVectors(Location l, ParticleEffect.OrdinaryColor color, Vector relativeCorner1FUR, Vector relativeCorner2FUR, Vector relativeCorner3FUR, float particlesPerBlock, boolean symmetrically)
    {
        for (Vector v : getParticleParallelogramFRUVectors(relativeCorner1FUR, relativeCorner2FUR, relativeCorner3FUR, particlesPerBlock, symmetrically))
        {
            playBasicParticle(getAbsFromRelLocFRU(l, (float)v.getX(), (float)v.getY(), (float)v.getZ(), true), color, false);
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

    public static void playWing(Entity e, int frame, List<ParticleEffect.OrdinaryColor> colors)
    {
        Random r = new Random();
        Vector facingVector = getVectorOfYaw(e);
        float backwardMod = 0.3F;
        float angle = 2F*(float)Math.PI*(frame%30)/30;
        float angleMod1 = (float)Math.sin(angle);
        float angleMod2 = (float)Math.sin(angle+Math.PI/2);
        //Bukkit.broadcastMessage("" + tipMod);
        for (int i = 0; i < 40; i++)
        {
            float wingOffset = (r.nextFloat() - 0.5F)*2;
            wingOffset += 0.05F* getSign(wingOffset);
            float wingFactor = Math.abs(wingOffset);
            float furtherBackMod = (float)Math.pow((2.5F+angleMod2)/3*wingFactor, 2.3F);
            playBasicParticle(e.getLocation().add(-facingVector.getX()*(backwardMod+furtherBackMod) - facingVector.getZ()*wingOffset*2.5,
                    1.3F + (r.nextFloat() - 0.5F)*2*(1F-wingFactor)*0.5 + Math.pow(wingFactor, 2.3F)*((2.5+angleMod1)/3),
                    -facingVector.getZ()*(backwardMod+furtherBackMod) + facingVector.getX()*wingOffset*2.5), colors.get(r.nextInt(colors.size())), false);
        }
        if (frame % 13 == 3)
        {
            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 0.5F, 1.12F);
        }
        if (e instanceof Player && frame % 18 == 0)
        {
            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.1F, 2F);
        }
    }

    public static ParticleEffect.OrdinaryColor getParticleColor(int red, int green, int blue)
    {
        return new ParticleEffect.OrdinaryColor(red, green, blue);
    }
}
