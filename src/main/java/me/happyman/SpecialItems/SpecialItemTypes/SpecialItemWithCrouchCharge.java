package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.Callable;

import static me.happyman.Plugin.getPlugin;

public class SpecialItemWithCrouchCharge extends SpecialItemWithCharge
{
    public SpecialItemWithCrouchCharge(UsefulItemStack item, float chargeAmount, float dischargeAmount)
    {
        super(item, chargeAmount, dischargeAmount, ChargingMode.DISCHARGE_AUTOMATICALLY);
    }

    @Override
    public void performSelectAction(final Player p)
    {
        super.performSelectAction(p);
        activateCrouchChargeItemIfCrouching(p);
    }

    @Override
    public final void performSneakAction(Player p, boolean sneaking)
    {
        super.performSneakAction(p, sneaking);
        if (sneaking)
        {
            setCharging(p, true);
        }
        else
        {
            setCharging(p, false);
        }
    }

    @Override
    public boolean performItemPickup(Player p, Location whereTheItemWasFound)
    {
        activateCrouchChargeItemIfCrouching(p);
        return super.performItemPickup(p, whereTheItemWasFound);
    }

    private void activateCrouchChargeItemIfCrouching(final Player p)
    {
        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                if (isBeingHeld(p) && SmashEntityTracker.isCrouching(p))
                {
                    setCharging(p, true);
                }
                return "";
            }
        });
    }
}
