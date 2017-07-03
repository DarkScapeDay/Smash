package me.happyman.ItemTypes;

import me.happyman.commands.SmashManager;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.HashMap;

public abstract class SmashItemWithUsages extends SmashItemWithExp
{
    private final int max_usages;
    private HashMap<Player, Integer> usages;
    private final boolean rechargeOnLand;

    public SmashItemWithUsages(Material mat, String name, Enchantment[] enchants, int[] levels, int usages, boolean rechargeOnLand)
    {
        super(mat, name, enchants, levels);
        this.max_usages = usages;
        this.usages = new HashMap<Player, Integer>();
        this.rechargeOnLand = rechargeOnLand;
    }
//
    public SmashItemWithUsages(Material mat, String name, int usages, boolean rechargeOnLand) {
        super(mat, name);
        this.max_usages = usages;
        this.usages = new HashMap<Player, Integer>();
        this.rechargeOnLand = rechargeOnLand;
    }

    public boolean rechargesOnLand()
    {
        return rechargeOnLand;
    }

    public int getMaxUsages()
    {
        return max_usages;
    }

    public void addUsage(Player p)
    {
        if (isBeingHeld(p))
        {
            if (max_usages - getUsages(p) > 0)
            {
                usages.put(p, getUsages(p) + 1);
                setExpToRemaining(p);
            }
            else
            {
                SmashManager.getPlugin().sendErrorMessage("Warning! Tried to add to rocket usages when all the usages were used up!");
            }
        }
        else
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Tried to increment someone's usages when they weren't holding a " + getItem().getItemMeta().getDisplayName() + "!");
        }
    }

    public void setExpToRemaining(Player p)
    {
        if (max_usages == 0)
        {
            p.setExp(0);
        }
        else
        {
            p.setExp(1 - (float)1.0* getUsages(p) / max_usages);
        }
    }

    public void restoreUsages(Player p)
    {
        if (!rechargeOnLand || SmashWorldManager.canRefuelNow(p))
        {
            setUsages(p, 0);
        }
    }

    public void setUsages(Player p, int amount)
    {
        if (getUsages(p) != amount)
        {
            usages.put(p, amount);
            if (isBeingHeld(p))
            {
                setExpToRemaining(p);
            }
        }
    }

    public int getUsages(Player p)
    {
        if (!usages.containsKey(p))
        {
            usages.put(p, 0);
        }
        return usages.get(p);
    }

    public boolean canUseItem(Player p)
    {
        return max_usages > getUsages(p);
    }

    public void performRightClickAction(Player p)
    {
        if (canUseItem(p))
        {
            performAction(p);
        }
    }

    public abstract void performAction(Player p);
}
