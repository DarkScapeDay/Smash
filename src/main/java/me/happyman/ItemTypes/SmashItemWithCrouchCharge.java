package me.happyman.ItemTypes;

import me.happyman.commands.SmashManager;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.worlds.SmashWorldInteractor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.concurrent.Callable;

public abstract class SmashItemWithCrouchCharge extends SmashItemWithCharge implements Listener
{
    public SmashItemWithCrouchCharge(Material mat, String name, float chargeAmount, float dischargeAmount)
    {
        super(mat, name, chargeAmount, dischargeAmount, true);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    @Override
    public void setCharge(Player p, float charge)
    {
        getPlayerCharges().put(p, charge);
        if (isBeingHeld(p))
        {
            setExpToRemaining(p);
        }
        else if (isDischarger() && charge == 0F && !isCharging(p))
        {
            cancelTask(p);
        }
    }

    @EventHandler
    public void xpBarUpdate(final PlayerItemHeldEvent e)
    {
        activateCrouchChargeItemIfCrouching(e.getPlayer());
    }

    @EventHandler
    public void baseballCrouch(PlayerToggleSneakEvent e)
    {
        Player p = e.getPlayer();
        if (inventoryContains(p))
        {
            if (e.isSneaking())
            {
                setCharging(p, true);
                activateTask(e.getPlayer());
            }
            else
            {
                setCharging(p, false);
            }
        }
    }

    @EventHandler
    public void pickupBatChargeRightAway(PlayerPickupItemEvent e)
    {
        if (!SmashWorldInteractor.isInSpectatorMode(e.getPlayer()))
        {
            activateCrouchChargeItemIfCrouching(e.getPlayer());
        }
    }

    private void activateCrouchChargeItemIfCrouching(final Player p)
    {
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call() {
                if (isBeingHeld(p))
                {
                    if (SmashEntityTracker.isCrouching(p))
                    {
                        setCharging(p, true);
                        activateTask(p);
                    }
                    setExpToRemaining(p);
                }
                return "";
            }
        });

    }
}
