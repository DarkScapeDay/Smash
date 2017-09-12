package me.happyman.SpecialItems.SmashKitMgt;

import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.SpecialItemTypes.SpecialItemWithCharge;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static me.happyman.Plugin.getPlugin;
import static me.happyman.worlds.WorldType.isInSpectatorMode;

public abstract class FinalSmash extends SpecialItemWithCharge
{
    //private static HashMap<Player, Long> activationTimes;
    private static final List<Player> activeFinalSmashers = new ArrayList<Player>();;
    private final int abilityDuration; //seconds
    private final Sound[] sounds;
    private final float[] volumes;
    private final float[] pitches;

    public FinalSmash(int abilityDuration, Sound[] activationSounds, float[] activationVolumes)
    {
        this(abilityDuration, activationSounds, activationVolumes, null);
    }

    public FinalSmash(int abilityDurationSeconds, Sound[] activationSounds, float[] activationVolumes, float[] pitches)
    {
        super(new UsefulItemStack(Material.NETHER_STAR, ChatColor.GOLD + "Final Smash", new Enchantment[] {Enchantment.DIG_SPEED}, new int[] {1}), 0,  .006F, ChargingMode.DISCHARGE_AUTOMATICALLY);
        //activationTimes = new HashMap<Player, Long>();
        this.abilityDuration = abilityDurationSeconds;
        sounds = activationSounds;
        volumes = activationVolumes;
        this.pitches = pitches;
    }

    public SpecialItem[] getSecretItems()
    {
        return null;
    }

    public FinalSmash()
    {
        this(30, null, null);
    }

    @Override
    public boolean canBeUsed(Player p)
    {
        return SmashWorldManager.isSmashWorld(p.getWorld()) && !isInSpectatorMode(p);
    }

    public static boolean hasFinalSmashActive(Player p)
    {
        return activeFinalSmashers.contains(p);
    }


    @Override
    public void setCharge(Player p, float amount)
    {
        super.setCharge(p, amount);
        if (amount == 0)
        {
            startFinalSmash(p);
        }
    }

    @Override
    public void performResetAction(Player p)
    {
        if (hasInstanceTaskActive(p) && activeFinalSmashers.contains(p))
        {
            endFinalSmashAbility(p);
            activeFinalSmashers.remove(p);
        }
        super.performResetAction(p);
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
            activationTimes.put(p, getSecond());
            trackPlayerCharge(p);
        }
    }*/



    public int getAbilityDurationSeconds()
    {
        return abilityDuration;
    }

    protected abstract void performFinalSmashAbility(Player p);

    protected abstract void endFinalSmashAbility(Player p);

    @Override
    protected void decreaseCharge(final Player p)
    {
        super.decreaseCharge(p);
        if (getCharge(p) == 0)
        {
            startFinalSmash(p);
        }
    }

    private void endFinalSmash(Player player)
    {
        SpecialItem[] secretItems = getSecretItems();
        if (secretItems != null)
        {
            for (SpecialItem item : getSecretItems())
            {
                item.removeAll(player);
            }
        }
    }

    private void startFinalSmash(final Player p)
    {
        removeOne(p);
        SpecialItem[] secretItems = getSecretItems();
        if (secretItems != null)
        {
            for (SpecialItem item : getSecretItems())
            {
                item.give(p);
            }
        }
        if (!hasFinalSmashActive(p))
        {
            playSmashSound(p.getLocation());
            performFinalSmashAbility(p);
            activeFinalSmashers.add(p);
            this.addTask(p, Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                @Override
                public void run()
                {
                    if (hasFinalSmashActive(p))
                    {
                        performResetAction(p);
                    }
                }
            }, abilityDuration*20 + 1));
        }
    }

    @Override
    public final void performRightClickAction(final Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        startFinalSmash(p);
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
    public boolean give(Player p)
    {
        if (!hasFinalSmashActive(p) && super.give(p))
        {
            setCharge(p, 1F);
            setCharging(p, false);
            return true;
        }
        return false;
    }
}
