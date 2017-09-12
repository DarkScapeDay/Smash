package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

import static me.happyman.Plugin.getPlugin;

public class SpecialItemWithCharge extends SpecialItemWithTask
{
    private final HashMap<Player, Integer> chargeTrackTasks = new HashMap<Player, Integer>();;
    private final float dischargeAmount;
    private final float chargeAmount;
    private final HashMap<Player, Float> playerCharges = new HashMap<Player, Float>();;
    private final ArrayList<Player> againstDefaultChargers = new ArrayList<Player>();;
    final ChargingMode mode;

    public enum ChargingMode
    {
        CHARGE_AUTOMATICALLY(true, 1F, 0F), DISCHARGE_AUTOMATICALLY(false, 0F, 1F);

        final boolean defaultChargeState;
        final float defaultCharge;
        final float limit;

        ChargingMode(boolean defaultChargeState, float defaultCharge, float limit)
        {
            this.defaultChargeState = defaultChargeState;
            this.defaultCharge = defaultCharge;
            this.limit = limit;
        }
    }

    public SpecialItemWithCharge(UsefulItemStack item, float chargeAmount, float dischargeAmount, ChargingMode mode)
    {
        super(item);
        this.dischargeAmount = dischargeAmount;
        this.chargeAmount = chargeAmount;
        this.mode = mode;
    }

    public boolean isCharging(Player p)
    {
        return mode.defaultChargeState != isGoingAgainstDefault(p);
    }

    protected boolean isGoingAgainstDefault(Player p)
    {
        return againstDefaultChargers.contains(p);
    }

    protected boolean setCharging(final Player p, boolean wantToCharge)
    {
        if (wantToCharge != mode.defaultChargeState)
        {
            if (!againstDefaultChargers.contains(p))
            {
                againstDefaultChargers.add(p);
                trackCharge(p);
                return true;
            }
        }
        else
        {
            if (againstDefaultChargers.contains(p))
            {
                againstDefaultChargers.remove(p);
                return true;
            }
        }
        return false;
    }

    private void trackCharge(final Player p)
    {
        if (!chargeTrackTasks.containsKey(p))
        {
            chargeTrackTasks.put(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    if (isCharging(p))
                    {
                        increaseCharge(p);
                    }
                    else
                    {
                        decreaseCharge(p);
                    }
                }
            }, 0, 1));
        }
    }

    public boolean canBeUsed(Player p)
    {
        return getCharge(p) >= dischargeAmount && super.canBeUsed(p);
    }

    public void performResetAction(Player p)
    {
        super.performResetAction(p);
        forgetCharge(p);
    }

    public float getCharge(Player p)
    {
        Float charge = playerCharges.get(p);
        return charge == null ? mode.defaultCharge : charge;
    }

    private void forgetCharge(Player p)
    {
        playerCharges.remove(p);
        againstDefaultChargers.remove(p);
        Integer chargeTask = chargeTrackTasks.get(p);
        if (chargeTask != null)
        {
            Bukkit.getScheduler().cancelTask(chargeTask);
            chargeTrackTasks.remove(p);
        }
        if (isBeingHeld(p))
        {
            setExpToRemaining(p);
        }
    }

    protected void setCharge(Player p, float charge)
    {
        if (charge == mode.defaultCharge)
        {
            forgetCharge(p);
        }
        else
        {
            trackCharge(p);
            playerCharges.put(p, charge);
            if (isBeingHeld(p))
            {
                setExpToRemaining(p);
            }
        }
    }

    protected void increaseCharge(Player p)
    {
        float newCharge = getCharge(p) + chargeAmount;
        setCharge(p, newCharge < 1 ? newCharge : 1F);
    }

    protected void decreaseCharge(Player p)
    {
        float newCharge = getCharge(p) - dischargeAmount;
        setCharge(p, newCharge > 0 ? newCharge : 0F);
    }

    @Override
    public void performDeselectAction(Player p)
    {
        super.performDeselectAction(p);
        setCharging(p, mode.defaultChargeState);
    }

    @Override
    public void setExpToRemaining(Player p)
    {
        p.setExp(getCharge(p));
    }

    @Override
    public void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        decreaseCharge(p);
    }
}
