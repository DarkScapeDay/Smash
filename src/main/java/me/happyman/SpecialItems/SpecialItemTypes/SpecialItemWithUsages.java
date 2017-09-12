package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;

import static me.happyman.Plugin.sendErrorMessage;

public class SpecialItemWithUsages extends SpecialItemWithExp
{
    private final int max_usages;
    private final HashMap<Player, Integer> usages = new HashMap<Player, Integer>();
    private final boolean rechargeOnLand;

    public SpecialItemWithUsages(UsefulItemStack item, int max_usages, boolean rechargeOnLand)
    {
        super(item);
        this.max_usages = max_usages;
        this.rechargeOnLand = rechargeOnLand;
    }

    @Override
    public void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        if (((Entity)p).isOnGround())
        {
            addUsage(p);
            Vector v = p.getLocation().getDirection();
            //Vector v = smashManager.getUnitDirection(p.getLocation(), p.getTargetBlock(new HashSet<Material>(Arrays.asList(Material.AIR)), 4).getLocation());
            //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity from \"Normal\" Sword");
            p.setVelocity(new Vector(v.getX()*5.5, 0.5, v.getZ()*5.5));
        }
        addUsage(p);
    }

    @Override
    public void performLandAction(Player p)
    {
        super.performLandAction(p);
        if (rechargeOnLand)
        {
            performResetAction(p);
        }
    }

    @Override
    public final void performResetAction(Player p)
    {
        super.performResetAction(p);
        setUsages(p, 0);
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

    public final void addUsage(Player p)
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
                sendErrorMessage("Warning! Tried to add to rocket usages when all the usages were used up!");
            }
        }
        else
        {
            sendErrorMessage("Error! Tried to increment someone's usages when they weren't holding a " + getItemStack().getItemMeta().getDisplayName() + "!");
        }
    }

    public final int getUsages(Player p)
    {
        if (!usages.containsKey(p))
        {
            usages.put(p, 0);
        }
        return usages.get(p);
    }

    @Override
    public boolean canBeUsed(Player p)
    {
        return max_usages > getUsages(p);
    }

    @Override
    public final void setExpToRemaining(Player p)
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

}
