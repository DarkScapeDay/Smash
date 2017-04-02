package me.happyman.ItemTypes;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class SmashItemWithCharge extends SmashItemWithTask
{
    private final float DISCHARGE_AMOUNT;
    private final float CHARGE_AMOUNT;
    private HashMap<Player, Float> playerCharges;
    private ArrayList<Player> goingAgainstGrain;
    private final boolean discharger;

    public SmashItemWithCharge(Material mat, String name, float chargeAmount, float dischargeAmount, boolean dischargesAutomatically)
    {
        super(mat, name);
        DISCHARGE_AMOUNT = dischargeAmount;
        CHARGE_AMOUNT = chargeAmount;
        playerCharges = new HashMap<Player, Float>();
        goingAgainstGrain = new ArrayList<Player>();
        discharger = dischargesAutomatically;
    }

    public SmashItemWithCharge(Material mat, String name, String lore, float chargeAmount, float dischargeAmount, boolean dischargesAutomatically)
    {
        this(mat, name, lore, chargeAmount, dischargeAmount, dischargesAutomatically, false);
    }

    public SmashItemWithCharge(Material mat, String name, String lore, float chargeAmount, float dischargeAmount, boolean dischargesAutomatically, boolean canBeLeftClicked)
    {
        super(mat, name, lore, canBeLeftClicked);
        DISCHARGE_AMOUNT = dischargeAmount;
        CHARGE_AMOUNT = chargeAmount;
        playerCharges = new HashMap<Player, Float>();
        goingAgainstGrain = new ArrayList<Player>();
        discharger = dischargesAutomatically;
    }

    public SmashItemWithCharge(Material mat, String name, Enchantment[] enchants, int[] levels, float chargeAmount, float dischargeAmount, boolean dischargesAutomatically)
    {
        super(mat, name, enchants, levels);
        DISCHARGE_AMOUNT = dischargeAmount;
        CHARGE_AMOUNT = chargeAmount;
        playerCharges = new HashMap<Player, Float>();
        goingAgainstGrain = new ArrayList<Player>();
        discharger = dischargesAutomatically;
    }

    public HashMap<Player, Float> getPlayerCharges()
    {
        return playerCharges;
    }

    public boolean isCharging(Player p)
    {
        return goingAgainstGrain.contains(p) && discharger || !goingAgainstGrain.contains(p) && !discharger;
    }

    public float getDischargeAmount()
    {
        return DISCHARGE_AMOUNT;
    }

    public float getChargeAmount()
    {
        return CHARGE_AMOUNT;
    }

    public void setCharging(Player p, boolean wantToCharge)
    {
        if (!isCharging(p) && wantToCharge && discharger || isCharging(p) && !wantToCharge && !discharger)
        {
            goingAgainstGrain.add(p);
        }
        else if (!isCharging(p) && wantToCharge && !discharger || isCharging(p) && !wantToCharge && discharger)
        {
            goingAgainstGrain.remove(p);
        }
    }

    public boolean canUseItem(Player p)
    {
        return getCharge(p) >= getDischargeAmount() && !SmashKitManager.canChangeKit(p);
    }

    public void setExpToRemaining(Player p)
    {
        p.setExp(getCharge(p));
    }

    public void resetCharge(Player p)
    {
        initializeCharge(p);
        setCharging(p, !discharger);
    }

    private void initializeCharge(Player p)
    {
        if (discharger)
        {
            playerCharges.put(p, 0F);
        }
        else
        {
            playerCharges.put(p, 1F);
        }
        setExpToRemaining(p);
    }

    public float getCharge(Player p)
    {
        if (!playerCharges.containsKey(p))
        {
            initializeCharge(p);
        }
        return playerCharges.get(p);
    }

    public boolean isPerformingManualAction(Player p)
    {
        return goingAgainstGrain.contains(p) && isBeingHeld(p);
    }

    public void putPlayerCharge(Player p, float charge)
    {
        playerCharges.put(p, charge);
    }

    public void setCharge(Player p, float charge)
    {
        playerCharges.put(p, charge);
        if (isBeingHeld(p))
        {
            setExpToRemaining(p);
        }
        if (!discharger && charge == 0F || discharger && charge == 1F)
        {
            setCharging(p, !discharger);
        }
        else if (!discharger && charge == 1F && isCharging(p) || discharger && charge == 0F && !isCharging(p))
        {
            cancelTask(p);
        }
    }

    private void increaseCharge(Player p)
    {
        if (getCharge(p) + CHARGE_AMOUNT < 1)
        {
            setCharge(p, getCharge(p) + CHARGE_AMOUNT);
        }
        else
        {
            setCharge(p, 1F);
        }
    }

    public void decreaseCharge(Player p)
    {
        if (getCharge(p) - DISCHARGE_AMOUNT > 0)
        {
            setCharge(p, getCharge(p) - DISCHARGE_AMOUNT);
        }
        else
        {
            setCharge(p, 0F);
        }
    }

    public void activateTask(final Player p)
    {
        if (!hasTaskActive(p))
        {
            putTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                public void run()
                {
                    if (!isBeingHeld(p))
                    {
                        setCharging(p, !discharger);
                    }
                    if (isPerformingManualAction(p) && !discharger || !isPerformingManualAction(p) && discharger)
                    {
                        decreaseCharge(p);
                    }
                    else
                    {
                        increaseCharge(p);
                    }
                }
            }, 0, 1));
        }
    }

    public boolean isDischarger()
    {
        return discharger;
    }

    @Override
    public void performDeselectAction(Player p)
    {
        super.performDeselectAction(p);
        p.setExp(0);
        setCharging(p, !discharger);
    }
}
