package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithOverlappingTasks;
import me.happyman.SmashItemDrops.Hammer;
import me.happyman.SmashKitMgt.SmashKit;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.SmashKits.Kirby;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

public class KirbyInhaler extends SmashItemWithOverlappingTasks
{
    private static HashMap<Player, SmashKit> maskKit;
    private static final int MASK_KIT_DURATION = 45;

    private final int INHALE_RANGE = 12;
    private final float MAX_INHALE_ACCELERATION = 0.26F;
    private final float NOSEBLEED_ZONE = 1.6F;
    private final float INHALE_SLOWDOWN = 0.4F;
    private static final float MUTATION_NERF = 0.5F;

    private final Kirby KIRBY_KIT;

    private HashMap<Player, Integer> inhalers;

    public KirbyInhaler(Kirby kit)
    {
        super(Material.GLASS_BOTTLE, ChatColor.GRAY + "" + ChatColor.BOLD + "Inhale");
        maskKit = new HashMap<Player, SmashKit>();
        KIRBY_KIT = kit;
        inhalers = new HashMap<Player, Integer>();
    }

    public boolean hasMaskKit(Player p)
    {
        return maskKit.containsKey(p);
    }

    @Override
    public void setExpToRemaining(Player p)
    {
        p.setExp(0);
    }

    private void performSwallow(final Player p, final LivingEntity target)
    {
        p.getWorld().playSound(p.getLocation(), Sound.BURP, 1F, 1F);
        final Random r = new Random();
        if (target instanceof Player)
        {
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable()
            {
                public String call()
                {
                    SmashAttackListener.attackWithCustomKB(p, "Swallow", new Location(p.getWorld(), (r.nextFloat() - 0.5), p.getEyeLocation().getY() - 0.4, (r.nextFloat() - 0.5)), (Player)target, 30, 50, true, false);
                    return "";
                }
            });
            if (!(SmashKitManager.getSelectedKit((Player)target) instanceof Kirby))
            {
                SmashAttackListener.setFinalAttackMod(p, MUTATION_NERF);
                SmashItemManager.setItemSlotForKitChange(p);
                SmashItemManager.setSpecialWielder(p, this);
                SmashKit targetKit = SmashKitManager.getSelectedKit((Player)target);
                targetKit.applyKitInventory(p, false);
                maskKit.put(p, targetKit);
                boolean foundRocket = false;
                boolean foundAir = false;
                for (int i = 0; i < 36; i++)
                {
                    if (!foundRocket && getMaskKit(p).getRocket().isThis(p.getInventory().getItem(i)))
                    {
                        p.getInventory().setItem(i, KIRBY_KIT.getRocket().getItem());
                        foundRocket = true;
                    }
                    else if (!foundAir && (p.getInventory().getItem(i) == null || p.getInventory().getItem(i).getType().equals(Material.AIR)))
                    {
                        p.getInventory().setItem(i, KIRBY_KIT.getReverser().getItem());
                        foundAir = true;
                    }
                    if (foundRocket && foundAir)
                    {
                        break;
                    }
                }
                p.getEquipment().setChestplate(KIRBY_KIT.getArmor()[2]);
                p.getEquipment().setLeggings(KIRBY_KIT.getArmor()[1]);
                putTask(p, Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
                    public void run() {
                        resetToKirby(p, false);
                    }
                }, MASK_KIT_DURATION*20));
            }
        }
        else
        {
            //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity from inhaler");
            target.setVelocity(new Vector((r.nextFloat() - 0.5)*2, 1.8, (r.nextFloat() - 0.5)*2));
            target.damage(8);
        }
    }

    public static void resetToKirby(final Player p, boolean clearItemDrops)
    {
        SmashAttackListener.forgetFinalAttackMod(p);
        if (!Hammer.isWieldingHammer(p))
        {
            if (maskKit.containsKey(p))
            {
                maskKit.remove(p);
            }
            if (SmashItemManager.isSpecialWielder(p))
            {
                SmashItemManager.resetSpecialWielder(p, false);
            }
            SmashKitManager.getSelectedKit(p).applyKitInventory(p, clearItemDrops);
        }
        else
        {
            Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable()
            {
                public void run()
                {
                    resetToKirby(p, false);
                }
            }, 2);
        }
    }

    public SmashKit getMaskKit(Player p)
    {
        if (hasMaskKit(p))
        {
            return maskKit.get(p);
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Tried to get the mask kit for player p, who didn't actually have one.");
        return null;
    }

    public void cancelTask(Player p)
    {
        super.cancelTask(p);
        resetToKirby(p, true);
    }

    public void activateTask(final Player p)
    {
        putTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable()
        {
            int i = 0;
            public void run()
            {
                if (isBeingHeld(p))
                {
                    if (i < 4)
                    {
                        final LivingEntity target = SmashManager.getNearestEntityExcept(p, p, true, 30*(float)Math.PI/180, true);
                        if (target != null)
                        {
                            float distance = (float)p.getLocation().distance(target.getLocation());
                            int maxSpeed = 10;
                            if (distance > NOSEBLEED_ZONE && (!(target instanceof Player) || SmashEntityTracker.getSpeed((Player)target) < maxSpeed) && distance <= INHALE_RANGE && distance > 0)
                            {
                                SmashAttackListener.sendEntityTowardLocation(p.getLocation(), target, MAX_INHALE_ACCELERATION*distance/INHALE_RANGE, true);
                            }
                            if (distance <= NOSEBLEED_ZONE)
                            {
                                performSwallow(p, target);
                                i = 4;
                            }
                        }
                        i++;
                    }
                }
            }
        }, 0, 1));
    }

    public void performRightClickAction(final Player p) //This can happen up to every 0.2 seconds or 4 ticks
    {
        p.getWorld().playSound(p.getLocation(), Sound.CAT_HISS, 0.5F, 1F);
        p.setWalkSpeed(INHALE_SLOWDOWN*0.2F);
        cancelInhaler(p, false);
        inhalers.put(p, Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
            public void run() {
                cancelInhaler(p, true);
            }
        }, 7));
        activateTask(p);
    }

    private void cancelInhaler(Player p, boolean resetWalkSpeed)
    {
        if (inhalers.containsKey(p))
        {
            Bukkit.getScheduler().cancelTask(inhalers.get(p));
        }
        if (resetWalkSpeed)
        {
            SmashEntityTracker.setSpeedToCurrentSpeed(p);
        }
    }

    @Override
    public void performDeselectAction(Player p)
    {
        cancelInhaler(p, true);
    }
}
