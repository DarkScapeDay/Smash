package me.happyman.SmashKits;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class FinalSmash extends SmashItemWithCharge
{
    //private static HashMap<Player, Long> activationTimes;
    private static HashMap<Player, List<Integer>> tasks = new HashMap<Player, List<Integer>>();
    private List<Player> activeFinalSmashers;
    private final int abilityDuration; //seconds
    private final Sound[] sounds;
    private final float[] volumes;
    private final float[] pitches;

    public FinalSmash(int abilityDuration, Sound[] activationSounds, float[] activationVolumes)
    {
        this(abilityDuration, activationSounds, activationVolumes, null);
    }

    public void addTask(Player p, int taskNumber)
    {
        if (!tasks.containsKey(p))
        {
            tasks.put(p, new ArrayList<Integer>());
        }
        tasks.get(p).add(taskNumber);
        SmashManager.getPlugin().cancelTaskAfterDelay(taskNumber, abilityDuration*20);
    }

    public FinalSmash(int abilityDuration, Sound[] activationSounds, float[] activationVolumes, float[] pitches)
    {
        super(Material.NETHER_STAR, ChatColor.GOLD + "Final Smash", new Enchantment[] {Enchantment.DIG_SPEED}, new int[] {1}, 0, .006F, true);
        //activationTimes = new HashMap<Player, Long>();
        this.abilityDuration = abilityDuration;
        sounds = activationSounds;
        volumes = activationVolumes;
        this.pitches = pitches;
        activeFinalSmashers = new ArrayList<Player>();
    }

    public FinalSmash()
    {
        this(30, null, null);
    }

    @Override
    public boolean canUseItem(Player p)
    {
        return SmashWorldManager.isSmashWorld(p.getWorld()) && !SmashWorldManager.isInSpectatorMode(p);
    }

    public boolean hasFinalSmashActive(Player p)
    {
        return activeFinalSmashers.contains(p);
    }


    @Override
    public void setCharge(Player p, float amount)
    {
        super.setCharge(p, amount);
        if (!hasFinalSmashActive(p) && amount == 0)
        {
            performRightClickAction(p);
        }
    }

    @Override
    public void cancelTask(Player p)
    {
        if (hasTaskActive(p))
        {
            //forgetActivationTime(p);
            if (activeFinalSmashers.contains(p))
            {
                endFinalSmashAbility(p);
                activeFinalSmashers.remove(p);
            }
            if (tasks.containsKey(p))
            {
                for (Integer task : tasks.get(p))
                {
                    Bukkit.getScheduler().cancelTask(task);
                }
                tasks.remove(p);
            }
        }
        super.cancelTask(p);
    }

    /*public int getAbilityCooldown()
    {
        return abilityDuration;
    }

    private void forgetActivationTime(Player p)
    {
        if (activationTimes.containsKey(p))
        {
            activationTimes.remove(p);
        }
    }

    public void activateFinalSmash(Player p)
    {
        if (!hasFinalSmashActive(p))
        {
            activationTimes.put(p, SmashManager.getPlugin().getSecond());
            activateTask(p);
        }
    }*/



    public int getAbilityDurationSeconds()
    {
        return abilityDuration;
    }

    protected abstract void performFinalSmashAbility(Player p);

    protected abstract void endFinalSmashAbility(Player p);

    @Override
    public void performRightClickAction(final Player p)
    {
        super.cancelTask(p);
        if (isBeingHeld(p))
        {
            SmashItemManager.removeOneItemFromHand(p);
            p.setExp(0);
        }
        else
        {
            p.getInventory().remove(getItem());
        }
        if (!hasFinalSmashActive(p))
        {
            //Bukkit.getPlayer("HappyMan").sendMessage("activating final smash for " + p.getName());
            playSmashSound(p.getLocation());

            //SmashWorldManager.sendMessageToWorld(p.getWorld(), ChatColor.GOLD + "" + ChatColor.ITALIC + p.getName() + "(" + SmashKitManager.getSelectedKitName(p) + ") has activated his Final Smash ability!");

            performFinalSmashAbility(p);
            activeFinalSmashers.add(p);
            putTask(p, Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    if (hasFinalSmashActive(p))
                    {
                        cancelTask(p);
                    }
                }
            }, abilityDuration*20));
        }
    }

    public void playSmashSound(Location l)
    {
        playSmashSound(l, 1);
    }

    public void playSmashSound(Location l, float pitchMod)
    {
        if (sounds != null && volumes != null && volumes.length >= sounds.length)
        {
            for (int i = 0; i < sounds.length; i++)
            {
                float miniVolume = volumes[i] % 1;
                if (miniVolume == 0)
                {
                    miniVolume = 1;
                }

                float pitch = 1;
                if (pitches != null)
                {
                    pitch = pitches[i];
                }

                for (int j = 0; j < volumes[i]; j++)
                {
                    l.getWorld().playSound(l, sounds[i], miniVolume, pitch*pitchMod);
                }
            }
        }
    }

    @Override
    public void decreaseCharge(final Player p)
    {
        super.decreaseCharge(p);
        if (getCharge(p) == 0)
        {
            performRightClickAction(p);
        }
    }

    @Override
    public boolean give(final Player p)
    {
        if (!hasFinalSmashActive(p) && super.give(p))
        {
            setCharge(p, 1F);
            activateTask(p);
            setCharging(p, false);
            return true;
        }
        return false;
    }
}
