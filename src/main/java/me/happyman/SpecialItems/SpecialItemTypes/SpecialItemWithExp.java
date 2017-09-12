package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.entity.Player;

abstract class SpecialItemWithExp extends SpecialItem
{
    SpecialItemWithExp(UsefulItemStack item)
    {
        super(item);
    }

    @Override
    public void performSelectAction(Player p)
    {
        setExpToRemaining(p);
    }

    @Override
    public void performResetAction(Player p)
    {
        super.performResetAction(p);
        setExpToRemaining(p);
    }

    @Override
    public void performDeselectAction(Player p)
    {
        p.setExp(0);
    }

    //public abstract void performRightClickAction(Player p);
    public abstract void setExpToRemaining(Player p);
}
