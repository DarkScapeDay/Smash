package me.happyman.ItemTypes;

import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.concurrent.Callable;

public abstract class GroundPound extends SmashItemWithUsages
{
    private int power;
    private float groundPoundRadius;
    private Sound landingSound;
    private Float landingSoundPitch;
    private HashMap<Player, Float> distanceOfPound;

    public GroundPound(Material mat, String name, int powerAtCenter, float radius)
    {
        super(mat, name, 1, true);
        this.power = powerAtCenter;
        this.groundPoundRadius = radius;
        landingSound = null;
        landingSoundPitch = null;
        distanceOfPound = new HashMap<Player, Float>();
    }

    public GroundPound(Material mat, String name, int powerAtCenter, float radius, Sound landingSound, float landingSoundPitch)
    {
        super(mat, name, 1, true);
        this.power = powerAtCenter;
        this.groundPoundRadius = radius;
        this.landingSound = landingSound;
        this.landingSoundPitch = landingSoundPitch;
        distanceOfPound = new HashMap<Player, Float>();
    }

    @Override
    public void restoreUsages(final Player p)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
            public void run()
            {
                setUsages(p, 0);
            }
        }, 2);
    }

    public void performAction(final Player p)
    {
        if (!((Entity)p).isOnGround())
        {
            addUsage(p);
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call()
                {
                    //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity for ground pound");
                    p.setVelocity(new Vector(0, -10, 0));
                    return "";
                }
            });
            distanceOfPound.put(p, SmashEntityTracker.getBlockDistanceBelowEntity(p, power));
            if (!p.getWorld().getHighestBlockAt(p.getLocation()).getRelative(0, -1, 0).getType().equals(Material.AIR))
            {
                for (float y = (float)p.getLocation().getY(); y >= 0; y--)
                {
                    if (!p.getWorld().getBlockAt(p.getLocation().getBlockX(), Math.round(y), p.getLocation().getBlockZ()).getType().equals(Material.AIR))
                    {
                        break;
                    }
                }
            }
        }
    }

    private float getDistanceOfPound(Player p)
    {
        if (distanceOfPound.containsKey(p))
        {
            return distanceOfPound.get(p);
        }
        return 0;
    }

    private void forgetDistanceOfPound(Player p)
    {
        if (distanceOfPound.containsKey(p))
        {
            distanceOfPound.remove(p);
        }
    }

    @Override
    public void performDeselectAction(Player p)
    {
        super.performDeselectAction(p);
        forgetDistanceOfPound(p);
    }

    public void performLand(Player p)
    {
        if (isBeingHeld(p) && !canUseItem(p))
        {
            float speed = getDistanceOfPound(p)/20;
            if (landingSound != null && landingSoundPitch != null)
            {
                p.getWorld().playSound(p.getLocation(), landingSound, 1, landingSoundPitch);
            }
            float maxY = 4;
            float powerOfLand = power * speed;
            /* Land power notification
            if (Bukkit.getPlayer("HappyMan") != null)
            {
                //Bukkit.getPlayer("HappyMan").sendMessage("power of land: " + powerOfLand);
            }
            */

            for (Entity e : p.getNearbyEntities(groundPoundRadius, maxY, groundPoundRadius))
            {
                float distance = (float)e.getLocation().distance(p.getLocation());
                if (e.isOnGround() && distance < groundPoundRadius && e instanceof Player && Math.abs(e.getLocation().getY() - p.getLocation().getY()) < maxY)
                {
                    Player victim = (Player)e;
                    float adjustedPower = (1-distance/groundPoundRadius)*powerOfLand;
                    /*Power adjusted for distance notification
                    if (Bukkit.getPlayer("HappyMan") != null)
                    {
                        //Bukkit.getPlayer("HappyMan").sendMessage("final power: " + adjustedPower);
                    }
                    */
                    SmashAttackListener.attackPlayer(p, getItem().getItemMeta().getDisplayName(), p.getEyeLocation(), victim, adjustedPower, false);
                }
            }
        }
        restoreUsages(p);
        forgetDistanceOfPound(p);
    }
}
