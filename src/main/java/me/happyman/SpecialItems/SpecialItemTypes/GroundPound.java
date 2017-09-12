package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.SmashAttackManager;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.getPlugin;

public class GroundPound extends SpecialItemWithUsages
{
    private int power;
    private float groundPoundRadius;
    private Sound landingSound;
    private Float landingSoundPitch;
    private HashMap<Player, Float> distanceOfPound;

    public GroundPound(Material item, int powerAtCenter, float radius)
    {
        this(item, powerAtCenter, radius, null, null);
    }

    public GroundPound(Material item, int powerAtCenter, float radius, Sound landingSound, Float landingSoundPitch)
    {
        super(new UsefulItemStack(item, ChatColor.RED + "" + ChatColor.BOLD + "Ground Pound"), 1, true);
        this.power = powerAtCenter;
        this.groundPoundRadius = radius;
        this.landingSound = landingSound;
        this.landingSoundPitch = landingSoundPitch;
        distanceOfPound = new HashMap<Player, Float>();
    }

//    @Override
//    public void performResetAction(final Player p)
//    {
//        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
//            public void run()
//            {
//                setUsages(p, 0);
//            }
//        }, 2);
//    }

    @Override
    public boolean canBeUsed(Player p)
    {
        return super.canBeUsed(p) && !((Entity)p).isOnGround();
    }

    @Override
    public void performRightClickAction(final Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
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

    @Override
    public boolean performHeldLandAction(Player p)
    {
        if (isBeingHeld(p) && !canBeUsed(p))
        {
            float speed = getDistanceOfPound(p)/20;
            if (landingSound != null && landingSoundPitch != null)
            {
                p.getWorld().playSound(p.getLocation(), landingSound, 1, landingSoundPitch);
            }
            float maxY = 4;
            float powerOfLand = power * speed;
            /* Land power notification
            if (Bukkit.getAttacker("HappyMan") != null)
            {
                //Bukkit.getAttacker("HappyMan").sendMessage("power of land: " + powerOfLand);
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
                    if (Bukkit.getAttacker("HappyMan") != null)
                    {
                        //Bukkit.getAttacker("HappyMan").sendMessage("final power: " + adjustedPower);
                    }
                    */
                    SmashAttackManager.attackPlayer(p, getItemStack().getItemMeta().getDisplayName(), p.getEyeLocation(), victim, adjustedPower, false);
                }
            }
        }
        performResetAction(p);
        forgetDistanceOfPound(p);
        return true;
    }
}
