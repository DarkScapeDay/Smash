package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.SpecialItems.SpecialItemTypes.SpeedChanger;
import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class LightFeather extends SpeedChanger
{
    public LightFeather()
    {
        super(new UsefulItemStack(Material.FEATHER, ChatColor.GRAY + "Light Feather"), 1.5F, 10);
    }

    @Override
    public void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        removeOne(p);
    }

    public void setExpToRemaining(Player p)
    {
        p.setExp(0);
    }
}
