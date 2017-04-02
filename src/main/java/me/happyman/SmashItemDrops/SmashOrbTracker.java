package me.happyman.SmashItemDrops;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class SmashOrbTracker
{
    private Item item;
    private Integer task;
    private ArrayList<Item> surroundingOrbs;
    private Vector currentVelocity;
    private Location nextLocation;
    private static final float orbMoveSpeed = 0.15F;
    private static final float inertiaMod = 0.5F; //0-1, closer to 1 meaning it won't change velocity as fast
    private final int STARTING_HITS_BEFORE_BREAK = 8;
    private static final int HIT_MS_COOLDOWN = 200;
    private static final float ORB_RADIUS = .3F;
    private static final int ORB_COUNT = 100;//100;
    private static final Random r = new Random();
    private static final ArrayList<Material> ORB_MATERIAL = new ArrayList<Material>(Arrays.asList(Material.GLOWSTONE, Material.COAL_BLOCK));
    private int hitsBeforeBreak;
    private Long timeOfLastHit;
    private final Vector zero;

    protected SmashOrbTracker(Item item, Location startingLocation)
    {
        task = null;
        this.item = item;
        zero = new Vector().zero();
        hitsBeforeBreak = STARTING_HITS_BEFORE_BREAK;
        timeOfLastHit = null;
        surroundingOrbs = new ArrayList<Item>();
        currentVelocity = zero;
        for (int i = 0; i < ORB_COUNT; i++)
        {
            final float[] offset = getRandomOffset();
            surroundingOrbs.add((Item)startingLocation.getWorld().dropItem(
                    startingLocation.clone().add(offset[0], offset[1], offset[2]), new ItemStack(getRandomOrbMaterial()))
            );
        }

        SmashOrb.logOrb(this);
    }

    public static Material getRandomOrbMaterial()
    {
        return ORB_MATERIAL.get(r.nextInt(ORB_MATERIAL.size()));
    }

    public static ArrayList<Material> getOrbMaterials()
    {
        return ORB_MATERIAL;
    }

    protected float[] getRandomOffset()
    {
        float[] coords = new float[3];
        for (int coordNum = 0; coordNum < 3; coordNum++)
        {
            coords[coordNum] = (r.nextFloat() - 0.5F)*2F*ORB_RADIUS;
        }
        float distanceFromCenter = (float) SmashManager.getMagnitude(coords[0], coords[1], coords[2]);
        for (int coordNum = 0; coordNum < 3; coordNum++)
        {
            coords[coordNum] = coords[coordNum] * ORB_RADIUS / distanceFromCenter;
        }
        return coords;
    }


    protected ArrayList<Item> getSurroundingOrbs()
    {
        return surroundingOrbs;
    }

    protected Item getCenter()
    {
        return item;
    }

    protected void remove()
    {
        Item orb = getCenter();
        orb.getLocation().getWorld().playEffect(orb.getLocation(), Effect.EXPLOSION, 0, SmashWorldManager.SEARCH_DISTANCE);
        for (Item e : getSurroundingOrbs())
        {
            e.remove();
        }
        if (!orb.isDead())
        {
            orb.remove();
            SmashOrb.unlogSmashOrb(orb.getWorld());
        }
        cancelTask();
    }

    public boolean isOnHitCooldown()
    {
        if (timeOfLastHit != null && SmashManager.getPlugin().getMillisecond() - timeOfLastHit < HIT_MS_COOLDOWN)
        {
            return true;
        }
        timeOfLastHit = null;
        return false;
    }

    public void hit(Player p)
    {
        if (!isOnHitCooldown())
        {
            chooseNextLocation();
            hitsBeforeBreak--;
            float pitch = 0.5F + (float)(STARTING_HITS_BEFORE_BREAK - hitsBeforeBreak)/STARTING_HITS_BEFORE_BREAK;
            p.getWorld().playSound(p.getLocation(), Sound.ORB_PICKUP, 0.6F, pitch);
            timeOfLastHit = SmashManager.getPlugin().getMillisecond();
            if (hitsBeforeBreak <= 0)
            {
                smash(p);
            }
        }
    }

    private void smash(final Player p)
    {
        if (!SmashKitManager.hasFinalSmashActive(p))
        {
            p.getWorld().playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
            remove();
            SmashKitManager.getSelectedKit(p).getProperties().getFinalSmash().give(p);
        }
    }

    protected void act()
    {
        float buffer = .9F;
        for (Entity e : getCenter().getNearbyEntities(ORB_RADIUS + buffer, ORB_RADIUS + buffer, ORB_RADIUS + buffer))
        {
            if (e != null
                    && (SmashEntityTracker.hasCulprit(e) || e instanceof Projectile && ((Projectile)e).getShooter() instanceof Player)
                    && SmashAttackListener.getAdjustedItemLocation(e.getLocation()).distance(getCenter().getLocation()) < ORB_RADIUS + buffer)
            {
                Player attacker;
                if (SmashEntityTracker.hasCulprit(e))
                {
                    attacker = Bukkit.getPlayer(SmashEntityTracker.getCulpritName(e));
                }
                else
                {
                    attacker = (Player)((Projectile)e).getShooter();
                }
                if (attacker != null)
                {
                    e.remove();
                    hit(attacker);
                }
            }
        }

        Location currentLoc = getCenter().getLocation();
        if (nextLocation == null || SmashManager.getMagnitude(currentLoc.getX() - nextLocation.getX(), currentLoc.getZ() - nextLocation.getZ()) < 3 || !clear(getLocation(), SmashManager.getUnitDirection(getLocation(), nextLocation)))
        {
            chooseNextLocation();
        }

        move();
    }

    //Return true if found a safe location
    private void chooseNextLocation()
    {
        Location currentLoc = getCenter().getLocation();

        Location whereToGo = SmashWorldManager.getRandomItemSpawnLocation(currentLoc.getWorld()).clone().add(0, 7, 0);
        Vector directionOfNextLocation = SmashManager.getUnitDirection(currentLoc, whereToGo);
        int iterator = 0;
        while (!clear(currentLoc, directionOfNextLocation) && iterator < 100)
        {
            whereToGo = SmashWorldManager.getRandomItemSpawnLocation(getCenter().getWorld()).clone().add(0, 7, 0);
            directionOfNextLocation = SmashManager.getUnitDirection(currentLoc, whereToGo);
            iterator++;
        }
        if (iterator == 100)
        {
            nextLocation = currentLoc;
        }
        else
        {
            nextLocation = whereToGo;
        }
    }

    private void move()
    {
        if (currentVelocity.equals(zero))
        {
            currentVelocity = SmashManager.getUnitDirection(getCenter().getLocation(), nextLocation).multiply(orbMoveSpeed);
        }
        else
        {
            currentVelocity.multiply(inertiaMod);
            Vector directionToAdd = SmashManager.getUnitDirection(getCenter().getLocation(), nextLocation);
            directionToAdd.multiply(orbMoveSpeed*(1-inertiaMod));
            currentVelocity.add(directionToAdd);
        }

        //centerOrb.teleport(newLocation);
        //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity of Smash orb");
        getCenter().setVelocity(currentVelocity);
        for (Item orb : getSurroundingOrbs())
        {
            //orb.teleport(newLocation.clone().add(offset[0], offset[1], offset[2]));
            orb.setVelocity(currentVelocity);
        }
    }

    private boolean clear(Location base, Vector directionOfNextLocation)
    {
        base.setDirection(directionOfNextLocation);
        int distance = 60;
        for (int i = 0; i < distance; i++)
        {
            float radians = (float)((float)i%4 / 4 * Math.PI * 2);
            Location whereToCheck1 = SmashManager.getAbsFromRelLocFRU(base,
                    new Vector(i, Math.cos(radians), Math.sin(radians)), false);
            Location whereToCheck2 = SmashManager.getAbsFromRelLocFRU(base,
                    new Vector(i, 0, 0), false);
            if (!whereToCheck1.getBlock().getType().equals(Material.AIR) || !whereToCheck2.getBlock().getType().equals(Material.AIR))
            {
                return false;
            }
        }
        return true;
    }

    protected void setTask(int task)
    {
        this.task = task;
    }

    private void cancelTask()
    {
        if (task != null)
        {
            Bukkit.getScheduler().cancelTask(task);
        }
    }

    public Location getLocation()
    {
        return getCenter().getLocation();
    }
}
