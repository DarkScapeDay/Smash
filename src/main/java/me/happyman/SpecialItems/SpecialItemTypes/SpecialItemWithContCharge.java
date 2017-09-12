package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public abstract class SpecialItemWithContCharge extends SpecialItemWithCharge
{
    private final boolean cancellable;

    public SpecialItemWithContCharge(UsefulItemStack item, float chargeAmount, float dischargeAmount, ChargingMode mode, boolean cancellable)
    {
        super(item, chargeAmount, dischargeAmount, mode);
        this.cancellable = cancellable;
    }

    public abstract void performDeactivationAction(Player p);

    public abstract void performActivationAction(Player p);

    @Override
    protected void setCharge(Player p, float charge)
    {
        super.setCharge(p, charge);
        if (charge == mode.limit)
        {
            performDeactivationAction(p);
            setCharging(p, mode.defaultChargeState);
        }
    }

    @Override
    public boolean canBeUsed(Player p)
    {
        return super.canBeUsed(p) && (cancellable || getCharge(p) == mode.defaultCharge);
    }

    @Override
    public final void performRightClickAction(Player p, Block blockClicked)
    {
        if (setCharging(p, !isCharging(p)))
        {
            if (isGoingAgainstDefault(p))
            {
                performActivationAction(p);
            }
            else if (cancellable)
            {
                performDeactivationAction(p);
            }
        }
    }

    @Override
    public final void performDeselectAction(Player p)
    {
        p.setExp(0);
    }

    @Override
    public final void performLeftClickAction(Player p, Block blockClicked) {}
}
