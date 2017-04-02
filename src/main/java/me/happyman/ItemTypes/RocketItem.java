package me.happyman.ItemTypes;

import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class RocketItem extends SmashItemWithUsages
{
    private final float launchMod;

    public RocketItem(String name, int usages, float launchMod)
    {
        super(Material.FIREWORK, name, usages, true);
        this.launchMod = launchMod;
    }

    public void performRightClickAction(Player p)
    {
        if (canUseItem(p))
        {
            addUsage(p);
            performAction(p);
        }
    }

    @Override
    public boolean canUseItem(Player p)
    {
        return getUsages(p) < getMaxUsages();
    }

    public void performAction(Player p)
    {
        float slowDownFactor = SmashEntityTracker.getSpeedFactor(p);
        float vLaunch;
        float horizontalBoost;
        if (!SmashEntityTracker.isCrouching(p))
        {
            vLaunch = 1.206F*launchMod*slowDownFactor;
            horizontalBoost = 0.80F*launchMod*slowDownFactor;
        }
        else
        {
            vLaunch = 0.5F;
            horizontalBoost = 2F;
        }
        //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity from rocket");
        p.setVelocity(new Vector(p.getLocation().getDirection().getX()*horizontalBoost, vLaunch, p.getLocation().getDirection().getZ()*horizontalBoost));
    }
}
